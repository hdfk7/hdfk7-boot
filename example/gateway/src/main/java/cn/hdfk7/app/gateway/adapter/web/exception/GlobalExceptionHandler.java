package cn.hdfk7.app.gateway.adapter.web.exception;

import cn.hdfk7.boot.starter.common.exception.AbstractGatewayExceptionHandler;
import cn.hdfk7.boot.starter.common.web.ClientIpResolver;
import org.springframework.stereotype.Component;

@Component
public class GlobalExceptionHandler extends AbstractGatewayExceptionHandler {
    public GlobalExceptionHandler(ClientIpResolver clientIpResolver) {
        super(clientIpResolver);
    }
}
