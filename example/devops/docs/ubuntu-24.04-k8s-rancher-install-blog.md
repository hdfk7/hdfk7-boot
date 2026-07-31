# 从零部署 Ubuntu Server 24.04.2 LTS、Kubernetes v1.35.6 和 Rancher v2.14.2

本文记录一次基于三台 Ubuntu Server 24.04.2 LTS 服务器部署 Kubernetes 集群，并在集群中安装 `Rancher` 的完整过程。整体目标是先用 `kubeadm` 搭建一个可用的三节点 Kubernetes 集群，再通过 `Helm` 安装 `ingress-nginx`、`cert-manager` 和 `Rancher`，最终通过固定域名访问 `Rancher` 控制台。

这篇文章更偏实战记录，命令会尽量保持完整，并在关键位置说明为什么要这样配置。对于刚开始搭建 Kubernetes 的环境来说，最容易出问题的地方通常不是某一条安装命令，而是网络、容器运行时、Pod 网段和 Service 网段没有提前规划好，所以本文会把这些参数放在前面统一说明。

本文环境规划如下：

| 节点 | 角色 | IP |
| --- | --- | --- |
| vm1 | control-plane / ingress | 192.168.31.201 |
| vm2 | worker | 192.168.31.202 |
| vm3 | worker | 192.168.31.203 |

计划安装版本：

| 组件 | 版本 |
| --- | --- |
| Ubuntu Server | 24.04.2 LTS |
| containerd | v2.2.4 |
| Kubernetes | v1.35.6 |
| Rancher | v2.14.2 |

Ubuntu Server 24.04.2 LTS 下载地址：

```text
https://releases.ubuntu.com/24.04.2/ubuntu-24.04.2-live-server-amd64.iso
```

建议每台服务器磁盘空间至少 40GB。

## 部署思路

本文采用一主两从的部署方式：

| 节点 | 主要用途 |
| --- | --- |
| vm1 | Kubernetes 控制平面节点，同时承载 `ingress-nginx` |
| vm2 | 普通工作节点 |
| vm3 | 普通工作节点 |

整体部署顺序如下：

1. 先完成三台 Ubuntu Server 的基础初始化，包括固定 IP、主机名、关闭 swap、配置内核参数。
2. 在三台服务器上安装 `containerd`，并把 cgroup driver 调整为 `systemd`。
3. 在三台服务器上安装指定版本的 `kubelet`、`kubeadm`、`kubectl`。
4. 在 vm1 上执行 `kubeadm init` 初始化控制平面。
5. 安装 `Calico`，让 Pod 网络真正可用。
6. 在 vm2、vm3 上执行 `kubeadm join` 加入集群。
7. 使用 `Helm` 安装 `ingress-nginx`、`cert-manager` 和 `Rancher`。

其中有两个网段需要提前确认：

| 网段 | 本文取值 | 说明 |
| --- | --- | --- |
| Service CIDR | 10.10.0.0/16 | Kubernetes Service 使用的虚拟网段 |
| Pod CIDR | 10.110.0.0/16 | Pod 使用的网络地址池，需要和 `Calico` 配置一致 |

只要你修改了这两个网段，后面的 `kubeadm init`、`Calico` 配置和代理 `NO_PROXY` 都要同步调整。

## 一、系统初始化

以下操作除特别说明外，需要在三台服务器上都执行。

系统初始化的目标是让三台机器处在一个干净、可预期的状态。Kubernetes 对节点的网络转发、内核模块、swap 状态都有要求，如果这些基础项没有处理好，后面很容易遇到节点 `NotReady`、Pod 无法通信、`kubelet` 启动异常等问题。

### 1. 固定服务器 IP

Ubuntu Server 的网卡配置文件通常在：

```bash
/etc/netplan/*.yaml
```

修改完成后执行：

```bash
netplan generate
netplan apply
```

本文使用的 IP 规划为：

```text
vm1: 192.168.31.201
vm2: 192.168.31.202
vm3: 192.168.31.203
```

控制平面节点的 IP 一定要稳定，因为后面 worker 节点加入集群时会连接 `192.168.31.201:6443`。如果控制平面节点 IP 变化，集群证书、API Server 访问地址和节点加入命令都会受到影响。

### 2. 设置主机名

分别在三台机器上设置对应主机名。

vm1：

```bash
hostnamectl set-hostname vm1
```

vm2：

```bash
hostnamectl set-hostname vm2
```

vm3：

```bash
hostnamectl set-hostname vm3
```

主机名会显示在 Kubernetes 节点列表中，建议使用简单、稳定、容易识别的名称。本文直接使用 `vm1`、`vm2`、`vm3`，后面给节点打标签和观察状态时也更直观。

### 3. 关闭 swap 和防火墙

Kubernetes 节点建议关闭 swap。

```bash
swapoff -a
sed -ri '/\sswap\s/s/^#?/#/' /etc/fstab
```

关闭 Ubuntu 默认防火墙：

```bash
ufw disable
systemctl disable ufw
```

关闭 swap 是 `kubelet` 的常规要求。虽然新版本 Kubernetes 对 swap 的支持有所变化，但生产和学习环境里，关闭 swap 仍然是最少踩坑的做法。

防火墙这里直接关闭，是为了减少实验环境中的网络干扰。生产环境不一定要关闭防火墙，但需要明确放通 Kubernetes、容器网络、ingress 以及节点间通信需要的端口。

常见需要放通的端口如下：

| 端口 / 协议 | 方向 | 使用组件 | 说明 |
| --- | --- | --- | --- |
| `6443/TCP` | 所有节点到 control-plane | `kube-apiserver` | Kubernetes API Server，worker 节点和 `kubectl` 都需要访问 |
| `2379-2380/TCP` | control-plane 节点之间 | `etcd` | `etcd` 客户端和节点间通信端口，单控制平面也建议只允许控制平面内部访问 |
| `10250/TCP` | control-plane 到所有节点 | `kubelet` | API Server 访问各节点 `kubelet` 使用 |
| `10257/TCP` | control-plane 内部 | `kube-controller-manager` | 控制器管理器端口，通常只需要控制平面内部访问 |
| `10259/TCP` | control-plane 内部 | `kube-scheduler` | 调度器端口，通常只需要控制平面内部访问 |
| `30000-32767/TCP` | 外部到节点 | `NodePort Service` | 如果使用 `NodePort` 暴露服务，需要放通 |
| `80/TCP` | 外部到 ingress 节点 | `ingress-nginx` | HTTP 入口，本文 ingress 部署在 vm1 |
| `443/TCP` | 外部到 ingress 节点 | `ingress-nginx` | HTTPS 入口，本文 `Rancher` 通过这个入口访问 |

如果使用 `Calico`，还需要根据网络模式放通节点之间的网络流量：

| 端口 / 协议 | 使用场景 | 说明 |
| --- | --- | --- |
| `179/TCP` | `Calico` BGP 模式 | 节点之间建立 BGP 邻居时需要 |
| `IPIP / protocol 4` | `Calico` IP-in-IP 模式 | Pod 跨节点通信封装流量使用 |
| `4789/UDP` | `Calico` VXLAN 模式 | 使用 VXLAN 封装时需要 |
| `5473/TCP` | `Calico` Typha | 部署 Typha 时，`calico-node` 连接 Typha 使用 |

本文是三台机器的小规模实验环境，为了减少网络排查成本直接关闭了 `ufw`。如果是生产环境，建议按实际网络插件、Service 暴露方式和安全边界精确放通，而不是简单开放所有端口。

### 4. 配置 Kubernetes 所需内核模块和网络参数

写入开机自动加载的内核模块：

```bash
printf '%s\n' overlay br_netfilter > /etc/modules-load.d/k8s.conf
```

这里写入了两个模块：

| 模块 | 作用 |
| --- | --- |
| `overlay` | `containerd` 常用的 OverlayFS 存储驱动依赖它 |
| `br_netfilter` | 让经过 Linux bridge 的流量可以被 iptables 或 nftables 规则处理 |

写入 Kubernetes 所需 sysctl 参数：

```bash
printf '%s\n' \
  'net.bridge.bridge-nf-call-iptables = 1' \
  'net.bridge.bridge-nf-call-ip6tables = 1' \
  'net.ipv4.ip_forward = 1' \
  > /etc/sysctl.d/k8s.conf
```

立即加载配置：

```bash
sysctl --system
```

这几个参数主要用于打开桥接流量处理和 IPv4 转发。Kubernetes 的 Pod 网络、Service 转发、跨节点通信都依赖这些内核网络能力。

到这里为止，建议三台服务器都重启一次。

```bash
reboot
```

重启后继续执行后面的步骤。

重启不是绝对必须，但在新装系统上建议做一次。这样可以确认主机名、网络、swap、内核模块和 sysctl 配置都能在开机后自动保持，不会只在当前 shell 里临时生效。

## 二、更新系统软件

三台服务器都执行：

```bash
apt update -y
apt upgrade -y
```

这一步会更新系统包索引并升级已有软件包。建议在安装 Kubernetes 组件前完成系统升级，避免后续因为底层依赖版本太旧导致安装失败。

## 三、添加 Kubernetes APT 软件源

三台服务器都执行：

```bash
apt install -y apt-transport-https ca-certificates curl gpg
mkdir -p -m 755 /etc/apt/keyrings
```

下载 Kubernetes 仓库签名 key：

```bash
curl -fsSL https://pkgs.k8s.io/core:/stable:/v1.35/deb/Release.key \
  | gpg --dearmor -o /etc/apt/keyrings/kubernetes-apt-keyring.gpg
```

添加 Kubernetes v1.35 软件源：

```bash
printf '%s\n' \
  'deb [signed-by=/etc/apt/keyrings/kubernetes-apt-keyring.gpg] https://pkgs.k8s.io/core:/stable:/v1.35/deb/ /' \
  > /etc/apt/sources.list.d/kubernetes.list
```

这里添加的是 Kubernetes 官方 v1.35 稳定版本软件源。后面安装 `kubelet`、`kubeadm`、`kubectl` 时，APT 就会从这个仓库拉取对应版本的软件包。

`signed-by` 指定了前面保存的 GPG key，APT 会用它校验软件包签名，避免安装来源不可信的包。

## 四、安装并配置 containerd

三台服务器都执行：

```bash
apt install -y runc wget
```

Kubernetes 本身不直接运行容器，它通过 CRI 调用容器运行时。这里选择 `containerd` 作为容器运行时，后面的 `kubeadm init` 也会显式指定它的 socket 地址。

下载并安装 `containerd v2.2.4`：

```bash
CONTAINERD_VERSION="2.2.4"
wget "https://github.com/containerd/containerd/releases/download/v${CONTAINERD_VERSION}/containerd-${CONTAINERD_VERSION}-linux-amd64.tar.gz" \
  -O "/tmp/containerd-${CONTAINERD_VERSION}-linux-amd64.tar.gz"

tar -C /usr/local -xzf "/tmp/containerd-${CONTAINERD_VERSION}-linux-amd64.tar.gz"
```

安装 `containerd` 的 systemd service：

```bash
wget "https://raw.githubusercontent.com/containerd/containerd/v${CONTAINERD_VERSION}/containerd.service" \
  -O /etc/systemd/system/containerd.service
```

安装 CNI plugins：

```bash
CNI_VERSION="1.9.1"
mkdir -p /opt/cni/bin
wget "https://github.com/containernetworking/plugins/releases/download/v${CNI_VERSION}/cni-plugins-linux-amd64-v${CNI_VERSION}.tgz" \
  -O "/tmp/cni-plugins-linux-amd64-v${CNI_VERSION}.tgz"

tar -C /opt/cni/bin -xzf "/tmp/cni-plugins-linux-amd64-v${CNI_VERSION}.tgz"
```

生成默认配置：

```bash
mkdir -p /etc/containerd
containerd config default > /etc/containerd/config.toml
```

将 `containerd` 的 cgroup driver 调整为 `systemd`：

```bash
sed -i 's/SystemdCgroup = false/SystemdCgroup = true/' /etc/containerd/config.toml
```

这一步很关键。`kubelet` 默认推荐使用 `systemd` 作为 cgroup driver，`containerd` 也调整为 `systemd` 后，两边的 cgroup 管理方式一致，节点运行会更稳定。

启动并设置开机自启：

```bash
systemctl daemon-reload
systemctl enable --now containerd
systemctl restart containerd
```

检查状态：

```bash
systemctl is-active containerd
containerd --version
runc --version
ls /opt/cni/bin
grep SystemdCgroup /etc/containerd/config.toml
```

如果输出中能看到：

```text
SystemdCgroup = true
```

说明 `containerd` 的 cgroup 配置已经调整完成。

## 五、可选：给 containerd 配置代理

如果服务器访问外网正常，这一步可以跳过。

`containerd` 拉取镜像时经常需要访问外部镜像仓库。如果服务器网络无法直接访问相关仓库，可以给 `containerd` 单独配置代理。注意，这里配置的是 `containerd` 服务的环境变量，不是当前终端的临时代理。

三台服务器都执行：

```bash
mkdir -p /etc/systemd/system/containerd.service.d
```

写入代理配置：

```bash
printf '%s\n' \
  '[Service]' \
  'Environment="HTTP_PROXY=http://192.168.31.200:9032"' \
  'Environment="HTTPS_PROXY=http://192.168.31.200:9032"' \
  'Environment="NO_PROXY=127.0.0.1,localhost,::1,192.168.31.0/24,10.10.0.0/16,10.110.0.0/16,.cluster.local,kubernetes,kubernetes.default,kubernetes.default.svc,kubernetes.default.svc.cluster.local"' \
  > /etc/systemd/system/containerd.service.d/http-proxy.conf
```

重新加载 `systemd` 并重启 `containerd`：

```bash
systemctl daemon-reload
systemctl restart containerd
```

检查代理是否生效：

```bash
systemctl is-active containerd
systemctl show --property=Environment containerd
```

`NO_PROXY` 里面的网段需要根据自己的环境调整。本文中：

```text
192.168.31.0/24   节点所在局域网
10.10.0.0/16      Service CIDR
10.110.0.0/16     Pod CIDR
```

`NO_PROXY` 的作用是告诉 `containerd`，访问这些地址时不要走代理。Kubernetes 集群内部地址、节点局域网、Pod 网段、Service 网段都应该放进去，否则集群内部通信可能被错误转发到代理，导致一些看起来很奇怪的网络问题。

## 六、安装 kubelet、kubeadm、kubectl

三台服务器都执行：

```bash
apt update
```

查看 `kubeadm` 可用版本：

```bash
apt-cache madison kubeadm
```

这一步用来确认当前软件源里到底有哪些 `kubeadm` 版本。建议 `kubelet`、`kubeadm`、`kubectl` 三个组件安装同一个小版本，避免版本不一致带来兼容性问题。

安装指定版本：

```bash
apt install -y kubelet=1.35.6-1.1 kubeadm=1.35.6-1.1 kubectl=1.35.6-1.1
```

锁定版本，避免后续系统升级时自动升级 Kubernetes 组件：

```bash
apt-mark hold kubelet kubeadm kubectl
```

启动 `kubelet`：

```bash
systemctl enable --now kubelet
```

查看版本：

```bash
kubeadm version
```

三个组件的分工可以简单理解为：

| 组件 | 作用 |
| --- | --- |
| `kubeadm` | 初始化集群、生成证书、让节点加入集群 |
| `kubelet` | 运行在每个节点上的核心服务，负责管理本节点上的 Pod |
| `kubectl` | Kubernetes 命令行客户端，用来操作和查看集群 |

执行 `systemctl enable --now kubelet` 后，`kubelet` 会立刻启动并设置为开机自启。在 `kubeadm init` 或 `kubeadm join` 之前，`kubelet` 可能会短暂处于异常或反复重启状态，这通常是正常的，因为它还没有拿到完整的节点配置。

## 七、初始化 Kubernetes 控制平面

只在 vm1 执行。

控制平面是整个 Kubernetes 集群的大脑，包含 API Server、Scheduler、Controller Manager、etcd 等核心组件。这里使用 `kubeadm init` 在 vm1 上完成控制平面初始化。

如果访问 `registry.k8s.io` 比较慢，可以在初始化前先从可用镜像源拉取控制面镜像。注意 `kubeadm config images pull` 和 `kubeadm init` 要使用同一个 `--image-repository`：

```bash
kubeadm config images pull \
  --kubernetes-version=v1.35.6 \
  --image-repository=registry.aliyuncs.com/google_containers \
  --cri-socket unix:///run/containerd/containerd.sock
```

```bash
kubeadm init \
  --kubernetes-version=v1.35.6 \
  --image-repository=registry.aliyuncs.com/google_containers \
  --apiserver-advertise-address=192.168.31.201 \
  --service-cidr=10.10.0.0/16 \
  --pod-network-cidr=10.110.0.0/16 \
  --cri-socket unix:///run/containerd/containerd.sock
```

主要参数说明：

| 参数 | 说明 |
| --- | --- |
| `--kubernetes-version=v1.35.6` | 指定要初始化的 Kubernetes 版本 |
| `--image-repository=registry.aliyuncs.com/google_containers` | 指定控制面镜像仓库，避免直接访问 `registry.k8s.io` |
| `--apiserver-advertise-address=192.168.31.201` | API Server 对外通告的地址，使用 vm1 的固定 IP |
| `--service-cidr=10.10.0.0/16` | Service 虚拟网段 |
| `--pod-network-cidr=10.110.0.0/16` | Pod 网段，需要和后面的 `Calico` 配置一致 |
| `--cri-socket unix:///run/containerd/containerd.sock` | 指定使用 `containerd` 作为容器运行时 |

初始化成功后，命令行会输出 worker 节点加入集群的命令，形式大概如下：

```bash
kubeadm join 192.168.31.201:6443 \
  --token abcdef.0123456789abcdef \
  --discovery-token-ca-cert-hash sha256:xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

这个加入命令后面在 vm2、vm3 上会用到。建议先保存下来。

如果后面忘记了，可以在 vm1 上重新生成：

```bash
kubeadm token create --print-join-command
```

初始化完成后，控制平面节点通常会自动带上 `node-role.kubernetes.io/control-plane` 标签，并带有 `NoSchedule` 污点。多节点集群一般保留这个污点，让业务 Pod 主要运行在 worker 节点上。

## 八、配置 kubectl 凭证

只在 vm1 执行。

`kubeadm init` 会生成管理员 kubeconfig 文件，默认位置是 `/etc/kubernetes/admin.conf`。为了让 root 用户可以直接执行 `kubectl`，这里把它复制到 `/root/.kube/config`。

```bash
mkdir -p /root/.kube
cp -i /etc/kubernetes/admin.conf /root/.kube/config
chown root:root /root/.kube/config
```

查看节点：

```bash
kubectl get nodes
```

此时节点可能还是 `NotReady`，因为还没有安装 CNI 网络插件。

这是正常现象。Kubernetes 初始化完成后，只是控制平面组件运行起来了，Pod 网络还没有真正打通。等后面安装 `Calico` 后，节点才会逐步变成 `Ready`。

## 九、安装 Calico 网络插件

只在 vm1 执行。

Kubernetes 只定义了容器网络接口规范，具体 Pod 网络需要由 CNI 插件实现。本文使用 `Calico`，它会为 Pod 分配地址，并处理跨节点 Pod 通信。

下载 `Calico` 配置文件。下面命令使用了代理，如果不需要代理，可以去掉 `-x http://192.168.31.200:9032`。

```bash
curl -x http://192.168.31.200:9032 \
  -L https://raw.githubusercontent.com/projectcalico/calico/v3.32.0/manifests/calico.yaml \
  -o calico.yaml
```

然后在 vm1 上使用本地 `Calico` YAML：

```bash
cp /root/offline-k8s/calico/calico-v3.32.0.yaml calico.yaml
```

修改 `Calico` 的 Pod 网段，使它与 `kubeadm init` 中的 `--pod-network-cidr=10.110.0.0/16` 保持一致：

```bash
sed -i 's|# - name: CALICO_IPV4POOL_CIDR|- name: CALICO_IPV4POOL_CIDR|' calico.yaml
sed -i 's|#   value: "192.168.0.0/16"|  value: "10.110.0.0/16"|' calico.yaml
```

检查修改结果：

```bash
grep -A2 CALICO_IPV4POOL_CIDR calico.yaml
```

应用 `Calico`：

```bash
kubectl apply -f calico.yaml
```

安装后可以观察 `kube-system` 命名空间里的 `Calico` Pod：

```bash
kubectl get pods -n kube-system
```

当 `Calico` 相关 Pod 正常运行后，节点状态一般会从 `NotReady` 逐步变成 `Ready`。

## 十、worker 节点加入集群

在 vm2、vm3 执行 `kubeadm init` 输出的 join 命令。

worker 节点加入集群时，会通过控制平面节点的 API Server 完成发现、证书校验和注册。加入成功后，vm2、vm3 上的 `kubelet` 会和控制平面保持通信，并开始承载业务 Pod。

示例：

```bash
kubeadm join 192.168.31.201:6443 \
  --token abcdef.0123456789abcdef \
  --discovery-token-ca-cert-hash sha256:xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

回到 vm1 查看集群状态：

```bash
kubectl get nodes
kubectl get pods --all-namespaces
```

也可以持续观察：

```bash
watch -n1 'kubectl get pods --all-namespaces; echo "===== 节点状态 ====="; kubectl get nodes'
```

等所有节点变成 `Ready`，系统 Pod 都正常运行后，Kubernetes 基础集群就部署完成了。

如果希望 worker 节点在 `kubectl get nodes` 中显示 `worker` 角色，可以手动打标签：

```bash
kubectl label node vm2 node-role.kubernetes.io/worker=
kubectl label node vm3 node-role.kubernetes.io/worker=
```

control-plane 节点通常不需要手动打标签，`kubeadm init` 会自动加上 `node-role.kubernetes.io/control-plane`。

这里的 worker 标签主要用于显示和后续调度选择，并不是节点加入集群的必要条件。真正决定节点能不能运行 Pod 的，是 `kubelet`、`containerd`、CNI 网络和节点状态是否正常。

## 十一、安装 Helm 并添加 Chart 仓库

以下操作只在 vm1 执行。

`Helm` 可以理解为 Kubernetes 的应用包管理工具。`Rancher`、`ingress-nginx`、`cert-manager` 都可以通过 `Helm` Chart 安装，这样比手工维护大量 YAML 更清晰，也方便后续升级。

安装 `Helm`。下面命令使用了代理，如果不需要代理，可以去掉 `-x http://192.168.31.200:9032`。

```bash
curl -x http://192.168.31.200:9032 https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash
```

添加 `Rancher`、`ingress-nginx`、`cert-manager` 仓库：

```bash
helm repo add rancher-stable https://releases.rancher.com/server-charts/stable
helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
helm repo add jetstack https://charts.jetstack.io
helm repo add rancher-charts https://charts.rancher.io
helm repo update
```

仓库添加完成后，`Helm` 就可以从这些仓库中拉取对应 Chart。本文使用 `rancher-stable` 安装 `Rancher`，使用 `ingress-nginx` 安装入口控制器，使用 `jetstack` 安装 `cert-manager`。

## 十二、部署 ingress-nginx

本文将 `ingress-nginx` 部署到 vm1，并直接使用宿主机的 80、443 端口。

`Rancher` 是通过 Ingress 暴露访问入口的，所以在安装 `Rancher` 之前，需要先准备好一个可用的 Ingress Controller。本文把 `ingress-nginx` 以 DaemonSet 形式部署到 vm1，并启用 `hostNetwork`，这样访问 vm1 的 80、443 端口就可以直接进入 `ingress-nginx`。

先给 vm1 打标签：

```bash
kubectl label node vm1 ingress-node=true
```

部署 `ingress-nginx`：

```bash
helm upgrade --install nginx-ingress ingress-nginx/ingress-nginx \
  --namespace ingress-nginx \
  --create-namespace \
  --set controller.kind=DaemonSet \
  --set controller.hostNetwork=true \
  --set controller.dnsPolicy=ClusterFirstWithHostNet \
  --set controller.service.enabled=false \
  --set controller.ingressClassResource.default=true \
  --set controller.watchIngressWithoutClass=true \
  --set controller.reportNodeInternalIp=true \
  --set-string controller.nodeSelector.ingress-node=true \
  --set controller.tolerations[0].key=node-role.kubernetes.io/control-plane \
  --set controller.tolerations[0].operator=Exists \
  --set controller.tolerations[0].effect=NoSchedule
```

这里几个关键配置的含义：

| 配置 | 作用 |
| --- | --- |
| `controller.kind=DaemonSet` | 以 `DaemonSet` 方式运行 ingress controller |
| `controller.hostNetwork=true` | 直接使用宿主机网络 |
| `controller.service.enabled=false` | 不创建 Service 暴露入口 |
| `controller.nodeSelector.ingress-node=true` | 只调度到带有 `ingress-node=true` 标签的节点 |
| `controller.tolerations` | 允许 Pod 调度到 control-plane 节点 |

查看 `ingress-nginx` 状态：

```bash
kubectl get pods -n ingress-nginx
```

如果这里的 Pod 无法启动，需要重点检查两件事：

1. vm1 是否已经打上 `ingress-node=true` 标签。
2. vm1 作为 control-plane 节点是否存在 `NoSchedule` 污点，`Helm` 命令里的 tolerations 是否正确生效。

## 十三、部署 cert-manager

只在 vm1 执行。

`cert-manager` 用来在 Kubernetes 中管理证书资源。`Rancher` 安装时通常会依赖它处理证书相关能力，所以建议在 `Rancher` 前先部署 `cert-manager`。

```bash
helm upgrade --install cert-manager jetstack/cert-manager \
  --namespace cert-manager \
  --create-namespace \
  --set crds.enabled=true
```

查看状态：

```bash
kubectl get pods -n cert-manager
```

确认 `cert-manager`、`cert-manager-cainjector`、`cert-manager-webhook` 都正常运行后，再继续部署 `Rancher`。

## 十四、部署 Rancher

只在 vm1 执行。

`Rancher` 是一个 Kubernetes 管理平台，可以用来管理当前集群，也可以纳管其他 Kubernetes 集群。本文先把 `Rancher` 安装在刚创建好的本地集群中。

本文使用的访问域名是：

```text
rancher.prodops.hdfk7.cn
```

这个域名需要提前通过 hosts、DNS 或 SLB 解析到 ingress 所在节点 IP。按本文规划，也就是解析到：

```text
192.168.31.201
```

如果只是本地测试，可以先在访问电脑的 hosts 文件中添加一条解析记录。生产环境建议使用正式 DNS 解析，并结合真实证书或企业内部证书体系。

部署 `Rancher`：

```bash
helm upgrade --install rancher rancher-stable/rancher \
  --namespace cattle-system \
  --create-namespace \
  --version 2.14.2 \
  --set hostname=rancher.prodops.hdfk7.cn \
  --set bootstrapPassword='PNsrQU09ZymN7Vxo' \
  --set ingress.ingressClassName=nginx \
  --set replicas=1
```

查看 `Rancher` 状态：

```bash
kubectl get pods -n cattle-system
kubectl get ingress -n cattle-system
```

等 `cattle-system` 命名空间下的 Pod 都正常运行后，就可以通过浏览器访问：

```text
https://rancher.prodops.hdfk7.cn
```

首次登录密码就是安装时设置的：

```text
PNsrQU09ZymN7Vxo
```

生产环境建议登录后立即修改默认密码，并按实际情况配置正式证书。

如果浏览器无法访问 `Rancher`，可以按下面顺序排查：

1. 域名是否解析到了 vm1。
2. vm1 的 80、443 端口是否被 `ingress-nginx` 占用并监听。
3. `ingress-nginx` 命名空间下的 Pod 是否正常。
4. `cattle-system` 命名空间下 `Rancher` Pod 是否正常。
5. `Rancher` Ingress 的 `ingressClassName` 是否为 `nginx`。

## 十五、部署 Rancher Monitoring

`Rancher Monitoring` 用来给集群补充资源监控能力，底层主要包含 `Prometheus`、`Grafana`、`node-exporter`、`kube-state-metrics` 等组件。安装完成后，可以在 `Rancher` 页面中查看节点、Pod、工作负载等资源的监控图表。

如果集群资源比较小，可以按最小化方式安装：关闭 `Alertmanager` 和 `prometheus-adapter`，只保留基础监控采集和图表展示能力。这样可以减少资源占用，适合只需要查看 CPU、内存、Pod 状态等基础资源趋势的场景。

需要注意的是，关闭 `prometheus-adapter` 后，集群不会提供 `custom.metrics.k8s.io` 自定义指标能力。如果还希望 `kubectl top nodes`、`kubectl top pods` 可用，仍然需要单独安装 `metrics-server`，因为 `Rancher Monitoring` 本身不会直接提供 `metrics.k8s.io`。

先部署 `rancher-monitoring-crd`：

```bash
helm upgrade --install rancher-monitoring-crd rancher-charts/rancher-monitoring-crd \
  --namespace cattle-monitoring-system \
  --create-namespace
```

再部署 `rancher-monitoring`。下面这组参数按小资源集群优化，只保留 1 天监控数据，并限制 `Prometheus` 的 CPU 和内存：

```bash
helm upgrade --install rancher-monitoring rancher-charts/rancher-monitoring \
  --namespace cattle-monitoring-system \
  --create-namespace \
  --set alertmanager.enabled=false \
  --set prometheus-adapter.enabled=false \
  --set grafana.defaultDashboardsEnabled=true \
  --set grafana.sidecar.dashboards.enabled=true \
  --set grafana.sidecar.dashboards.searchNamespace=ALL \
  --set prometheus.prometheusSpec.retention=1d \
  --set prometheus.prometheusSpec.resources.requests.cpu=100m \
  --set prometheus.prometheusSpec.resources.requests.memory=384Mi \
  --set prometheus.prometheusSpec.resources.limits.cpu=500m \
  --set prometheus.prometheusSpec.resources.limits.memory=768Mi \
  --set global.cattle.clusterId=local \
  --set global.cattle.clusterName=local \
  --set global.cattle.systemDefaultRegistry=""
```

其中几个关键参数的含义如下：

| 参数 | 说明 |
| --- | --- |
| `alertmanager.enabled=false` | 不部署告警管理组件，减少资源占用 |
| `prometheus-adapter.enabled=false` | 不部署自定义指标适配器，减少资源占用 |
| `grafana.defaultDashboardsEnabled=true` | 创建默认 `Grafana` 监控面板 |
| `grafana.sidecar.dashboards.enabled=true` | 启用 dashboard sidecar，自动加载 dashboard 配置 |
| `grafana.sidecar.dashboards.searchNamespace=ALL` | 允许从所有命名空间加载 dashboard，避免 `cattle-dashboards` 中的面板加载不到 |
| `prometheus.prometheusSpec.retention=1d` | 监控数据只保留 1 天 |
| `global.cattle.clusterId=local` | 指定当前集群是 `Rancher` 本地集群 |
| `global.cattle.clusterName=local` | 指定当前集群名称为 `local` |

查看部署状态：

```bash
kubectl get pods -n cattle-monitoring-system
```

如果没有先安装 `rancher-monitoring-crd`，直接安装 `rancher-monitoring` 可能会出现 `no matches for kind "Prometheus"`、`no matches for kind "PrometheusRule"`、`no matches for kind "ServiceMonitor"` 这类错误。

如果这个集群不是 `Rancher` 所在的本地集群，而是被导入的下游集群，`global.cattle.clusterId` 和 `global.cattle.clusterName` 不要写成 `local`，需要改成该集群在 `Rancher` 中的实际集群 ID 和名称。

## 十六、常用检查命令

查看节点状态：

```bash
kubectl get nodes -o wide
```

查看所有命名空间下的 Pod：

```bash
kubectl get pods --all-namespaces
```

查看 kube-system 组件：

```bash
kubectl get pods -n kube-system
```

查看 `ingress-nginx`：

```bash
kubectl get pods -n ingress-nginx
```

查看 `cert-manager`：

```bash
kubectl get pods -n cert-manager
```

查看 `Rancher`：

```bash
kubectl get pods -n cattle-system
kubectl get ingress -n cattle-system
```

查看 `Rancher Monitoring`：

```bash
kubectl get pods -n cattle-monitoring-system
```

持续观察集群状态：

```bash
watch -n1 'kubectl get pods --all-namespaces; echo "===== 节点状态 ====="; kubectl get nodes'
```

## 常见问题

### 1. 节点一直是 NotReady

优先检查 CNI 是否安装成功：

```bash
kubectl get pods -n kube-system
```

如果 `Calico` Pod 没有正常运行，再检查 `calico.yaml` 中的 `CALICO_IPV4POOL_CIDR` 是否和 `kubeadm init` 的 `--pod-network-cidr` 一致。

### 2. worker 节点 join 失败

先确认 worker 节点能访问控制平面节点：

```bash
curl -k https://192.168.31.201:6443
```

如果 join 命令里的 token 过期，可以在 vm1 上重新生成：

```bash
kubeadm token create --print-join-command
```

### 3. Rancher 域名打不开

先看 `Rancher` Ingress：

```bash
kubectl get ingress -n cattle-system
```

再看 `ingress-nginx` 是否正常监听宿主机端口：

```bash
kubectl get pods -n ingress-nginx -o wide
```

如果使用的是本地 hosts 解析，要确认访问电脑上的 hosts 文件已经把 `Rancher` 域名指向 `192.168.31.201`。

## 总结

到这里，一个基于 Ubuntu Server 24.04.2 LTS 的三节点 Kubernetes 集群，以及运行在集群内的 `Rancher` 管理平台就部署完成了。

这套流程的关键点不是单纯把命令跑完，而是保证几个核心配置始终一致：

1. 准备 Ubuntu Server 和固定 IP。
2. 关闭 swap，配置内核模块和网络转发。
3. 安装 `containerd`，并启用 `systemd` cgroup。
4. 安装 `kubelet`、`kubeadm`、`kubectl`。
5. 使用 `kubeadm` 初始化控制平面。
6. 安装 `Calico` 网络插件。
7. worker 节点加入集群。
8. 安装 `Helm`、`ingress-nginx`、`cert-manager`。
9. 使用 `Helm` 部署 `Rancher`。

需要特别注意的是，`kubeadm init` 中的 `--pod-network-cidr` 必须和 `Calico` 的 `CALICO_IPV4POOL_CIDR` 保持一致。本文使用的是：

```text
Pod CIDR:     10.110.0.0/16
Service CIDR: 10.10.0.0/16
```

如果你修改了这两个网段，`containerd` 代理配置中的 `NO_PROXY`、`Calico` 配置和 `kubeadm` 初始化参数也要一起调整。
