package cn.hdfk7.boot.starter.discovery.listener;

import cn.hdfk7.boot.starter.discovery.service.NacosServiceLookup;
import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.cloud.nacos.discovery.NacosServiceDiscovery;
import com.alibaba.nacos.api.naming.listener.Event;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.SmartLifecycle;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class GatewayRouteRefreshEventListener implements EventListener, SmartLifecycle {
    private static final int PHASE = Integer.MAX_VALUE - 100;

    private final Set<String> services = ConcurrentHashMap.newKeySet();
    private final ApplicationEventPublisher applicationEventPublisher;
    private final NacosDiscoveryProperties nacosDiscoveryProperties;
    private final NacosServiceDiscovery nacosServiceDiscovery;
    private final NacosServiceLookup nacosServiceLookup;

    private volatile boolean running;
    private ScheduledExecutorService watchExecutor;
    private ScheduledFuture<?> watchTask;

    public GatewayRouteRefreshEventListener(NacosDiscoveryProperties nacosDiscoveryProperties, NacosServiceDiscovery nacosServiceDiscovery, NacosServiceLookup nacosServiceLookup, ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.nacosDiscoveryProperties = nacosDiscoveryProperties;
        this.nacosServiceDiscovery = nacosServiceDiscovery;
        this.nacosServiceLookup = nacosServiceLookup;
    }

    @Override
    public synchronized void start() {
        if (running) {
            return;
        }
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "gateway-route-service-watcher");
            thread.setDaemon(true);
            return thread;
        });
        watchExecutor = executor;
        running = true;
        try {
            this.refresh();
            long watchDelay = Math.max(1L, nacosDiscoveryProperties.getWatchDelay());
            watchTask = executor.scheduleWithFixedDelay(this::refresh, watchDelay, watchDelay, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            running = false;
            watchExecutor = null;
            watchTask = null;
            executor.shutdownNow();
            unsubscribe();
            throw e;
        }
    }

    @Override
    public synchronized void stop() {
        if (!running) {
            return;
        }
        running = false;
        ScheduledFuture<?> task = watchTask;
        if (task != null) {
            task.cancel(true);
            watchTask = null;
        }
        ScheduledExecutorService executor = watchExecutor;
        if (executor != null) {
            executor.shutdownNow();
            watchExecutor = null;
        }
        unsubscribe();
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return PHASE;
    }

    @Override
    public void onEvent(Event event) {
        if (isRunning() && event instanceof NamingEvent namingEvent) {
            applicationEventPublisher.publishEvent(new RefreshRoutesEvent(namingEvent));
        }
    }

    private synchronized void refresh() {
        if (!isRunning()) {
            return;
        }
        try {
            for (String serviceName : nacosServiceDiscovery.getServices()) {
                subscribe(serviceName);
            }
        } catch (Exception e) {
            log.error("Failed to refresh nacos service subscriptions", e);
        }
    }

    private void subscribe(String serviceName) {
        if (!isRunning() || !services.add(serviceName)) {
            return;
        }
        try {
            nacosServiceLookup.subscribe(serviceName, this);
        } catch (Exception e) {
            services.remove(serviceName);
            log.error("Failed to subscribe nacos service, serviceName={}", serviceName, e);
        }
    }

    private void unsubscribe() {
        services.forEach(serviceName -> nacosServiceLookup.unsubscribe(serviceName, this));
        services.clear();
    }
}
