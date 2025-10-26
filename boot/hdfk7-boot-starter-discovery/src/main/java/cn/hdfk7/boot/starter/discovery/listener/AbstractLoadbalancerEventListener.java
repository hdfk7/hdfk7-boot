package cn.hdfk7.boot.starter.discovery.listener;

import cn.hutool.core.util.ReflectUtil;
import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.cloud.nacos.NacosServiceManager;
import com.alibaba.cloud.nacos.discovery.NacosServiceDiscovery;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.Event;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cloud.loadbalancer.cache.CaffeineBasedLoadBalancerCacheManager;
import org.springframework.cloud.loadbalancer.cache.LoadBalancerCacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.event.ContextClosedEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class AbstractLoadbalancerEventListener implements EventListener, ApplicationRunner, ApplicationEventPublisherAware, ApplicationContextAware {
    protected ApplicationEventPublisher applicationEventPublisher;
    protected volatile List<String> services = new ArrayList<>();
    protected NacosDiscoveryProperties nacosDiscoveryProperties;
    protected LoadBalancerCacheManager balancerCacheManager;
    protected NacosServiceDiscovery nacosServiceDiscovery;
    protected NacosServiceManager nacosServiceManager;
    protected ApplicationContext applicationContext;
    protected volatile Boolean loop = true;
    protected EventListener eventListener;
    protected NamingService naming;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        nacosDiscoveryProperties = applicationContext.getBean(NacosDiscoveryProperties.class);
        nacosServiceManager = applicationContext.getBean(NacosServiceManager.class);
        nacosServiceDiscovery = applicationContext.getBean(NacosServiceDiscovery.class);
        balancerCacheManager = applicationContext.getBean(CaffeineBasedLoadBalancerCacheManager.class);
        eventListener = applicationContext.getBean(this.getClass());
        naming = nacosServiceManager.getNamingService();
        services = nacosServiceDiscovery.getServices();
        services.forEach(this::addServiceListener);
        run();
    }

    protected void run() {
        Thread thread = new Thread(() -> {
            while (!isShutDown()) {
                try {
                    TimeUnit.MILLISECONDS.sleep(nacosDiscoveryProperties.getWatchDelay());
                    if (isShutDown()) break;
                    List<String> newServices = nacosServiceDiscovery.getServices();
                    for (String service : newServices) {
                        if (!services.contains(service)) {
                            services.add(service);
                            addServiceListener(service);
                        }
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        });
        thread.setName("loadbalancer-event-listener");
        thread.start();
    }

    @Override
    public void setApplicationContext(@NotNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void setApplicationEventPublisher(@NotNull ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onEvent(Event event) {
        if (event instanceof ContextClosedEvent) {
            loop = false;
            return;
        }
        if (event instanceof NamingEvent e) {
            ConcurrentMap<String, Cache> cacheMap = (ConcurrentMap<String, Cache>) ReflectUtil.getFieldValue(balancerCacheManager, "cacheMap");
            for (Map.Entry<String, Cache> entry : cacheMap.entrySet()) {
                CaffeineCache cache = (CaffeineCache) entry.getValue();
                if (cache.getNativeCache().getIfPresent(e.getServiceName()) != null) {
                    cache.invalidate();
                }
            }
        }
    }

    protected void addServiceListener(String serviceName) {
        try {
            if (isShutDown()) return;
            naming.subscribe(serviceName, eventListener);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    protected boolean isShutDown() {
        return !loop;
    }
}
