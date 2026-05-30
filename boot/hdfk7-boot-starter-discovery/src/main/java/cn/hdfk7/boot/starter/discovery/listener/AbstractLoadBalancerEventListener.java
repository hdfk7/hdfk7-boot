package cn.hdfk7.boot.starter.discovery.listener;

import cn.hdfk7.boot.starter.discovery.service.NacosServiceLookup;
import cn.hutool.core.util.ReflectUtil;
import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.cloud.nacos.discovery.NacosServiceDiscovery;
import com.alibaba.nacos.api.naming.listener.Event;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
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

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public abstract class AbstractLoadBalancerEventListener implements EventListener, ApplicationRunner, ApplicationEventPublisherAware, ApplicationContextAware {
    protected ApplicationEventPublisher applicationEventPublisher;
    protected final Set<String> services = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean initialized = new AtomicBoolean();
    private final AtomicBoolean started = new AtomicBoolean();
    protected NacosDiscoveryProperties nacosDiscoveryProperties;
    protected LoadBalancerCacheManager balancerCacheManager;
    protected NacosServiceDiscovery nacosServiceDiscovery;
    protected NacosServiceLookup nacosServiceLookup;
    protected ApplicationContext applicationContext;
    protected volatile boolean loop = true;
    protected EventListener eventListener;

    @Override
    public void run(@NonNull ApplicationArguments args) {
        if (shouldAutoStart()) {
            start();
        }
    }

    protected boolean shouldAutoStart() {
        return true;
    }

    protected void start() {
        if (!started.compareAndSet(false, true)) {
            return;
        }
        try {
            initDependenciesIfNecessary();
            refreshServices();
            startWatchThread();
        } catch (Exception e) {
            started.set(false);
            log.error(e.getMessage(), e);
        }
    }

    protected boolean isRunning() {
        return started.get() && !isShutDown();
    }

    private void initDependenciesIfNecessary() {
        if (!initialized.compareAndSet(false, true)) {
            return;
        }
        nacosDiscoveryProperties = applicationContext.getBean(NacosDiscoveryProperties.class);
        nacosServiceLookup = applicationContext.getBean(NacosServiceLookup.class);
        nacosServiceDiscovery = applicationContext.getBean(NacosServiceDiscovery.class);
        balancerCacheManager = applicationContext.getBean(CaffeineBasedLoadBalancerCacheManager.class);
        eventListener = applicationContext.getBean(this.getClass());
    }

    private void startWatchThread() {
        Thread thread = new Thread(this::watchServices, "loadbalancer-event-listener");
        thread.start();
    }

    private void watchServices() {
        while (isRunning()) {
            try {
                TimeUnit.MILLISECONDS.sleep(nacosDiscoveryProperties.getWatchDelay());
                refreshServices();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                loop = false;
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    private void refreshServices() throws Exception {
        for (String serviceName : nacosServiceDiscovery.getServices()) {
            subscribeService(serviceName);
        }
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void setApplicationEventPublisher(@NonNull ApplicationEventPublisher applicationEventPublisher) {
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

    protected void subscribeService(String serviceName) {
        try {
            if (!isRunning() || !services.add(serviceName)) {
                return;
            }
            nacosServiceLookup.subscribeIfNecessary(serviceName, eventListener);
        } catch (Exception e) {
            services.remove(serviceName);
            log.error(e.getMessage(), e);
        }
    }

    protected boolean isShutDown() {
        return !loop;
    }
}
