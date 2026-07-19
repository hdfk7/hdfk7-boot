#!/bin/bash -ile

PROJECT_NAME="${PROJECT_NAME:-}"
DEPLOYMENT_NAME="${DEPLOYMENT_NAME:-}"
NAMESPACE="${NAMESPACE:-}"
DOCKERFILE_PATH="${DOCKERFILE_PATH:-docker/Dockerfile}"
IMAGE_REGISTRY="${IMAGE_REGISTRY:-registry-vpc.cn-chengdu.aliyuncs.com}"
IMAGE_REPO="${IMAGE_REPO:-hd-app}"
DEPLOY_IMAGE_NETWORK="${DEPLOY_IMAGE_NETWORK:-internal}"
DOCKER_USER="${DOCKER_USER:-}"
DOCKER_PWD="${DOCKER_PWD:-}"

RUN_ENV="${RUN_ENV:-dev}"
BRANCH="${BRANCH:-origin/master}"
REMOTE="${REMOTE:-origin}"
GIT_CLEAN_FLAGS="${GIT_CLEAN_FLAGS:--fdx}"
GIT_URL="${GIT_URL:-${REMOTE_GIT_URL:-}}"
GIT_USERNAME="${GIT_USERNAME:-${GIT_USER:-}}"
GIT_PASSWORD="${GIT_PASSWORD:-${GIT_PWD:-}}"
GIT_SOURCE_MODE="local"
TARGET_BRANCH=""
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CODE_DIR="${CODE_DIR:-$(pwd)}"
BUILD_CODE_DIR="${BUILD_CODE_DIR:-${SCRIPT_DIR}/build/${PROJECT_NAME:-app}-${RUN_ENV}-$(date "+%Y%m%d%H%M%S")}"
CLEAN_BUILD_DIR="${CLEAN_BUILD_DIR:-true}"
CREATED_BUILD_CODE_DIR=""
GIT_ASKPASS_FILE=""

error_exit() {
    printf '[%s] [ERROR] %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$1" >&2
    exit 1
}

info_echo() {
    printf '[%s] [INFO] %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$1"
}

warn_echo() {
    printf '[%s] [WARN] %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$1"
}

require_var() {
    local name="$1"
    local value="$2"
    [ -n "${value}" ] || error_exit "缺少必要参数: ${name}"
}

to_internal_registry() {
    local registry="$1"
    if [[ "${registry}" == *vpc* ]]; then
        printf '%s\n' "${registry}"
    elif [[ "${registry}" == registry.* ]]; then
        printf 'registry-vpc.%s\n' "${registry#registry.}"
    else
        error_exit "IMAGE_REGISTRY无法自动切换为内网地址: ${registry}"
    fi
}

to_external_registry() {
    local registry="$1"
    printf '%s\n' "${registry/-vpc/}"
}

clean_worktree() {
    local clean_args
    read -r -a clean_args <<< "${GIT_CLEAN_FLAGS}"
    git reset --hard || error_exit "Git工作区重置失败: ${CODE_DIR}"
    git clean "${clean_args[@]}" || error_exit "Git未跟踪文件清理失败: ${CODE_DIR}"
}

process_git_branch() {
    local remote_ref_prefix
    local remote_branch_prefix
    local current_hash

    require_var "BRANCH" "${BRANCH}"
    require_var "REMOTE" "${REMOTE}"

    cd "${CODE_DIR}" || error_exit "进入临时构建目录失败: ${CODE_DIR}"
    git rev-parse --is-inside-work-tree >/dev/null 2>&1 || error_exit "临时构建目录不是有效Git仓库: ${CODE_DIR}"

    remote_ref_prefix="refs/remotes/${REMOTE}/"
    remote_branch_prefix="${REMOTE}/"
    TARGET_BRANCH="${BRANCH#refs/heads/}"
    TARGET_BRANCH="${TARGET_BRANCH#"$remote_ref_prefix"}"
    TARGET_BRANCH="${TARGET_BRANCH#"$remote_branch_prefix"}"

    require_var "TARGET_BRANCH" "${TARGET_BRANCH}"
    git check-ref-format --branch "${TARGET_BRANCH}" >/dev/null 2>&1 || error_exit "非法Git分支名: ${TARGET_BRANCH}"

    info_echo "Git分支处理参数：remote=${REMOTE}, branch=${TARGET_BRANCH}, clean=${GIT_CLEAN_FLAGS}"

    info_echo "清理Git工作区（切换前）"
    clean_worktree

    info_echo "获取远程分支信息"
    git fetch "${REMOTE}" --prune || error_exit "远程分支信息获取失败: ${REMOTE}"
    git rev-parse --verify --quiet "${REMOTE}/${TARGET_BRANCH}^{commit}" >/dev/null \
        || error_exit "远程分支不存在: ${REMOTE}/${TARGET_BRANCH}"

    info_echo "切换Git分支"
    git checkout -B "${TARGET_BRANCH}" "${REMOTE}/${TARGET_BRANCH}" || error_exit "Git分支切换失败: ${REMOTE}/${TARGET_BRANCH}"

    info_echo "清理Git工作区（切换后）"
    clean_worktree

    current_hash=$(git rev-parse --short HEAD || error_exit "获取Git提交哈希失败")
    info_echo "Git分支处理完成：${TARGET_BRANCH}@${current_hash}"
}

clone_remote_repository() {
    require_var "GIT_URL" "${GIT_URL}"
    require_var "GIT_USERNAME" "${GIT_USERNAME}"
    require_var "GIT_PASSWORD" "${GIT_PASSWORD}"

    GIT_ASKPASS_FILE="${BUILD_CODE_DIR}.git-askpass"
    cat > "${GIT_ASKPASS_FILE}" <<'EOF' || error_exit "创建Git认证脚本失败"
#!/bin/sh
case "$1" in
    *Username*) printf '%s\n' "$GIT_USERNAME" ;;
    *Password*) printf '%s\n' "$GIT_PASSWORD" ;;
    *) printf '\n' ;;
esac
EOF
    chmod 700 "${GIT_ASKPASS_FILE}" || error_exit "创建Git认证脚本失败"

    export GIT_TERMINAL_PROMPT=0
    export GIT_ASKPASS="${GIT_ASKPASS_FILE}"
    export GIT_USERNAME GIT_PASSWORD

    info_echo "克隆远程仓库"
    git clone --origin "${REMOTE}" "${GIT_URL}" "${BUILD_CODE_DIR}" || error_exit "克隆远程仓库失败"
    info_echo "远程仓库克隆完成"
}

trap 'if [ -n "${GIT_ASKPASS_FILE}" ] && [ -f "${GIT_ASKPASS_FILE}" ]; then rm -f -- "${GIT_ASKPASS_FILE}" || warn_echo "Git认证临时文件清理未完成：${GIT_ASKPASS_FILE}"; fi; if [ "${CLEAN_BUILD_DIR}" = "true" ] && [ -n "${CREATED_BUILD_CODE_DIR}" ] && [ -d "${CREATED_BUILD_CODE_DIR}" ]; then info_echo "清理临时构建目录：${CREATED_BUILD_CODE_DIR}"; rm -rf -- "${CREATED_BUILD_CODE_DIR}" || warn_echo "临时构建目录清理未完成：${CREATED_BUILD_CODE_DIR}"; elif [ "${CLEAN_BUILD_DIR}" != "true" ] && [ -n "${CREATED_BUILD_CODE_DIR}" ] && [ -d "${CREATED_BUILD_CODE_DIR}" ]; then info_echo "保留临时构建目录：${CREATED_BUILD_CODE_DIR}"; fi' EXIT

require_var "PROJECT_NAME" "${PROJECT_NAME}"
require_var "DEPLOYMENT_NAME" "${DEPLOYMENT_NAME}"
require_var "NAMESPACE" "${NAMESPACE}"
require_var "DOCKER_USER" "${DOCKER_USER}"
require_var "DOCKER_PWD" "${DOCKER_PWD}"

if [ -n "${GIT_URL}" ] || [ -n "${GIT_USERNAME}" ] || [ -n "${GIT_PASSWORD}" ]; then
    require_var "GIT_URL" "${GIT_URL}"
    require_var "GIT_USERNAME" "${GIT_USERNAME}"
    require_var "GIT_PASSWORD" "${GIT_PASSWORD}"
    GIT_SOURCE_MODE="remote"
else
    require_var "CODE_DIR" "${CODE_DIR}"
    [ -d "${CODE_DIR}" ] || error_exit "源代码目录不存在: ${CODE_DIR}"
    [ -f "${CODE_DIR}/pom.xml" ] || error_exit "源代码目录缺少pom.xml: ${CODE_DIR}"
    CODE_DIR="$(cd "${CODE_DIR}" && pwd)" || error_exit "获取源代码目录绝对路径失败: ${CODE_DIR}"
fi

case "${RUN_ENV}" in
    dev|test|prod)
        ;;
    *)
        error_exit "不支持的环境：${RUN_ENV}，仅支持dev/test/prod"
        ;;
esac

case "${DEPLOY_IMAGE_NETWORK}" in
    internal|vpc)
        DEPLOY_IMAGE_NETWORK="internal"
        ;;
    external|public)
        DEPLOY_IMAGE_NETWORK="external"
        ;;
    *)
        error_exit "不支持的部署镜像网络：${DEPLOY_IMAGE_NETWORK}，仅支持internal/external"
        ;;
esac

BUILD_IMAGE_REGISTRY="$(to_internal_registry "${IMAGE_REGISTRY}")"
if [ "${DEPLOY_IMAGE_NETWORK}" = "external" ]; then
    DEPLOY_IMAGE_REGISTRY="$(to_external_registry "${BUILD_IMAGE_REGISTRY}")"
else
    DEPLOY_IMAGE_REGISTRY="${BUILD_IMAGE_REGISTRY}"
fi

info_echo "构建脚本目录：${SCRIPT_DIR}"
info_echo "代码来源：${GIT_SOURCE_MODE}"
if [ "${GIT_SOURCE_MODE}" = "local" ]; then
    info_echo "源代码目录：${CODE_DIR}"
else
    info_echo "远程Git地址：${GIT_URL}"
fi
info_echo "目标环境：${RUN_ENV}"
info_echo "项目名称：${PROJECT_NAME}"
info_echo "部署对象：namespace=${NAMESPACE}, deployment=${DEPLOYMENT_NAME}"
info_echo "临时构建目录清理：${CLEAN_BUILD_DIR}"
info_echo "镜像仓库输入：${IMAGE_REGISTRY}"
info_echo "构建推送仓库：${BUILD_IMAGE_REGISTRY}"
info_echo "部署镜像网络：${DEPLOY_IMAGE_NETWORK}"
info_echo "部署镜像仓库：${DEPLOY_IMAGE_REGISTRY}"

BUILD_CODE_PARENT="$(dirname "${BUILD_CODE_DIR}")"
BUILD_CODE_NAME="$(basename "${BUILD_CODE_DIR}")"
mkdir -p "${BUILD_CODE_PARENT}" || error_exit "创建临时构建目录父目录失败: ${BUILD_CODE_DIR}"
BUILD_CODE_PARENT="$(cd "${BUILD_CODE_PARENT}" && pwd)" || error_exit "获取临时构建目录父目录失败: ${BUILD_CODE_DIR}"
BUILD_CODE_DIR="${BUILD_CODE_PARENT}/${BUILD_CODE_NAME}"
if [ "${GIT_SOURCE_MODE}" = "local" ]; then
    [ "${BUILD_CODE_DIR}" != "${CODE_DIR}" ] || error_exit "BUILD_CODE_DIR不能和CODE_DIR相同"
    case "${BUILD_CODE_DIR}/" in
        "${CODE_DIR}/"*)
            error_exit "BUILD_CODE_DIR不能放在CODE_DIR目录内部: ${BUILD_CODE_DIR}"
            ;;
    esac
fi
[ ! -e "${BUILD_CODE_DIR}" ] || error_exit "临时构建目录已存在，请换一个新目录: ${BUILD_CODE_DIR}"
mkdir -p "${BUILD_CODE_DIR}" || error_exit "创建临时构建目录失败: ${BUILD_CODE_DIR}"
CREATED_BUILD_CODE_DIR="${BUILD_CODE_DIR}"
info_echo "临时构建目录：${BUILD_CODE_DIR}"

if [ "${GIT_SOURCE_MODE}" = "local" ]; then
    info_echo "复制源代码到临时构建目录"
    cp -a "${CODE_DIR}/." "${BUILD_CODE_DIR}/" || error_exit "复制源代码到临时构建目录失败"
    info_echo "源代码复制完成"
else
    rmdir "${BUILD_CODE_DIR}" || error_exit "初始化远程克隆目录失败: ${BUILD_CODE_DIR}"
    clone_remote_repository
fi
CODE_DIR="${BUILD_CODE_DIR}"

process_git_branch

[ -d "${CODE_DIR}" ] || error_exit "临时构建目录不存在: ${CODE_DIR}"
[ -f "${CODE_DIR}/pom.xml" ] || error_exit "临时构建目录缺少pom.xml: ${CODE_DIR}"
cd "${CODE_DIR}" || error_exit "进入临时构建目录失败: ${CODE_DIR}"

info_echo "检查Java运行环境"
java -version || error_exit "Java环境未安装或配置异常，请先配置JAVA_HOME"

info_echo "检查Maven构建环境"
mvn -version || error_exit "Maven环境未安装或配置异常，请先配置MAVEN_HOME"

info_echo "开始Maven构建（跳过测试）"
mvn clean package -Dmaven.test.skip=true -e -U || error_exit "Maven构建失败，请检查代码或pom.xml"
info_echo "Maven构建完成"

CURRENT_TIME=$(date "+%Y-%m-%d-%H-%M-%S")
COMMIT_BRANCH="${TARGET_BRANCH}"
IMAGE_BRANCH="${COMMIT_BRANCH//\//-}"
COMMIT_HASH=$(git rev-parse --short HEAD || error_exit "获取Git提交哈希失败，请确保临时构建目录是Git仓库")
IMAGE_TAG="${PROJECT_NAME}-${IMAGE_BRANCH}-${COMMIT_HASH}-${CURRENT_TIME}"
BUILD_IMAGE_NAME="${BUILD_IMAGE_REGISTRY}/${IMAGE_REPO}/${RUN_ENV}:${IMAGE_TAG}"
DEPLOY_IMAGE_NAME="${DEPLOY_IMAGE_REGISTRY}/${IMAGE_REPO}/${RUN_ENV}:${IMAGE_TAG}"

info_echo "Git提交：${COMMIT_HASH}"
info_echo "构建镜像名称：${BUILD_IMAGE_NAME}"
info_echo "部署镜像名称：${DEPLOY_IMAGE_NAME}"

info_echo "登录镜像仓库：${BUILD_IMAGE_REGISTRY}"
echo "${DOCKER_PWD}" | docker login -u "${DOCKER_USER}" --password-stdin "${BUILD_IMAGE_REGISTRY}" || error_exit "镜像仓库登录失败，请检查用户名/密码/仓库地址"
info_echo "镜像仓库登录完成"

info_echo "构建镜像"
docker build -f "${DOCKERFILE_PATH}" -t "${BUILD_IMAGE_NAME}" . || error_exit "镜像构建失败，请检查镜像构建文件"
info_echo "镜像构建完成"

info_echo "推送镜像"
docker push "${BUILD_IMAGE_NAME}" || error_exit "镜像推送失败，请检查网络或仓库权限"
info_echo "镜像推送完成"

info_echo "清理本地镜像"
if docker rmi -f "${BUILD_IMAGE_NAME}"; then
    info_echo "本地镜像清理完成"
else
    warn_echo "本地镜像清理未完成：${BUILD_IMAGE_NAME}"
fi

IMAGE_NAME="${DEPLOY_IMAGE_NAME}"
export PROJECT_NAME DEPLOYMENT_NAME NAMESPACE RUN_ENV IMAGE_NAME IMAGE_TAG BUILD_IMAGE_NAME DEPLOY_IMAGE_NAME
info_echo "调用部署脚本：${SCRIPT_DIR}/deployment.sh"
bash "${SCRIPT_DIR}/deployment.sh" || error_exit "部署脚本执行失败"

info_echo "构建发布流程完成"
exit 0
