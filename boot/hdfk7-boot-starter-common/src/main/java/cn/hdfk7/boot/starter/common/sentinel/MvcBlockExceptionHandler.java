package cn.hdfk7.boot.starter.common.sentinel;

import cn.hdfk7.boot.proto.base.result.Result;
import cn.hdfk7.boot.proto.base.result.ResultCode;
import cn.hutool.json.JSONUtil;
import com.alibaba.csp.sentinel.adapter.spring.webmvc_v6x.callback.BlockExceptionHandler;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnClass(value = {BlockExceptionHandler.class})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class MvcBlockExceptionHandler implements BlockExceptionHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, String resourceName, BlockException e) throws Exception {
        int status = HttpStatus.TOO_MANY_REQUESTS.value();
        Result<Void> result = ResultCode.SERVICE_DOWNGRADE_ERROR.bindResult();
        if (e instanceof AuthorityException) {
            status = HttpStatus.UNAUTHORIZED.value();
            result = ResultCode.UNAUTHORIZED_ERROR.bindResult();
        }
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setStatus(status);
        response.getWriter().println(JSONUtil.toJsonStr(result));
    }
}
