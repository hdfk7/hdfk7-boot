package cn.hdfk7.boot.starter.discovery.listener;

import com.alibaba.nacos.api.naming.listener.Event;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.event.EventListener;

@Slf4j
public class GatewayLoadBalancerEventListener extends AbstractLoadBalancerEventListener {
    @Override
    protected boolean shouldAutoStart() {
        return false;
    }

    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        if (!isRunning()) {
            start();
        }
    }

    @Override
    public void onEvent(Event event) {
        super.onEvent(event);
        if (isRunning() && event instanceof NamingEvent e) {
            applicationEventPublisher.publishEvent(new RefreshRoutesEvent(e));
        }
    }
}
