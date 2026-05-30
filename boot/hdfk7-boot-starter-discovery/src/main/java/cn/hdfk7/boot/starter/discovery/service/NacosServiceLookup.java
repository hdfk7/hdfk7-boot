package cn.hdfk7.boot.starter.discovery.service;

import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.cloud.nacos.NacosServiceManager;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.pojo.ServiceInfo;
import com.alibaba.nacos.common.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class NacosServiceLookup {
    private final NacosServiceManager nacosServiceManager;
    private final NacosDiscoveryProperties nacosDiscoveryProperties;

    public boolean hasHealthyInstances(String serviceId) {
        if (!StringUtils.hasText(serviceId)) {
            return false;
        }
        try {
            List<Instance> instances = namingService()
                    .selectInstances(serviceId, nacosDiscoveryProperties.getGroup(), true, false);
            return !instances.isEmpty();
        } catch (NacosException e) {
            log.warn("Failed to query nacos instances without subscribe, serviceId={}, error={}", serviceId, e.getMessage());
            return false;
        }
    }

    public void subscribeIfNecessary(String serviceName, EventListener eventListener) throws NacosException {
        if (StringUtils.isBlank(serviceName) || hasRegisteredListener(serviceName)) {
            return;
        }
        String groupName = nacosDiscoveryProperties.getGroup();
        namingService().subscribe(serviceName, groupName, eventListener);
    }

    private boolean hasRegisteredListener(String serviceName) throws NacosException {
        String groupName = nacosDiscoveryProperties.getGroup();
        return namingService()
                .getSubscribeServices()
                .stream()
                .anyMatch(serviceInfo -> isSameService(serviceInfo, serviceName, groupName));
    }

    private NamingService namingService() throws NacosException {
        return nacosServiceManager.getNamingService();
    }

    private boolean isSameService(ServiceInfo serviceInfo, String serviceName, String groupName) {
        String groupedServiceName = groupName + "@@" + serviceName;
        return (serviceName.equals(serviceInfo.getName()) && groupName.equals(serviceInfo.getGroupName()))
                || groupedServiceName.equals(serviceInfo.getName())
                || groupedServiceName.equals(serviceInfo.getKeyWithoutClusters());
    }
}
