package cn.hdfk7.boot.starter.common.aspect;

import cn.hdfk7.boot.starter.common.constants.RequestParamConst;
import cn.hdfk7.boot.starter.common.util.IpUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONUtil;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
public abstract class LogAspect {

    public void init(JoinPoint joinPoint) {
        ServletRequestAttributes sra = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (sra == null) {
            return;
        }

        String parameters = JSONUtil.toJsonStr(filterArgs(joinPoint.getArgs()));
        HttpServletRequest request = sra.getRequest();
        request.setAttribute(RequestParamConst.REQUEST_START_TIME, System.currentTimeMillis());
        request.setAttribute(RequestParamConst.METHOD_NAME, joinPoint.getSignature().getName());
        request.setAttribute(RequestParamConst.PARAMETERS, parameters);

        String url = sra.getRequest().getRequestURL().toString();
        String method = sra.getRequest().getMethod();
        String host = IpUtil.getIpAddress(request);
        int port = request.getRemotePort();
        log.debug("method={},url={},host={},port={}", method, url, host, port);
    }

    public Object doTask(ProceedingJoinPoint joinPoint) throws Throwable {
        return joinPoint.proceed();
    }

    public void finishTask(Object ret) {
        ServletRequestAttributes sra = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (sra == null) {
            return;
        }

        String response = ObjUtil.isNotNull(ret) ? JSONUtil.toJsonStr(ret) : "-";
        HttpServletRequest request = sra.getRequest();
        String parameters = (String) request.getAttribute(RequestParamConst.PARAMETERS);
        parameters = ObjUtil.isNotNull(parameters) ? parameters : "-";
        Object startTime = request.getAttribute(RequestParamConst.REQUEST_START_TIME);
        request.removeAttribute(RequestParamConst.METHOD_NAME);
        request.removeAttribute(RequestParamConst.PARAMETERS);
        request.removeAttribute(RequestParamConst.REQUEST_START_TIME);

        long requestStartTime = Objects.nonNull(startTime) ? (Long) startTime : System.currentTimeMillis();
        long requestFinishTime = System.currentTimeMillis();
        long cost = requestFinishTime - requestStartTime;

        String url = request.getRequestURL().toString();
        String method = request.getMethod();
        String host = IpUtil.getIpAddress(request);
        int port = request.getRemotePort();

        log.debug("method={},url={},host={},port={},cost={},parameters={},response={}", method, url, host, port, cost, parameters, response);
    }

    protected List<Object> filterArgs(Object[] args) {
        List<Object> list = new ArrayList<>(args.length);
        for (Object arg : args) {
            if (!(arg instanceof ServletRequest
                    || arg instanceof ServletResponse
                    || arg instanceof MultipartFile
                    || arg instanceof BindingResult
                    || arg instanceof Throwable)) {
                list.add(arg);
            }
        }
        return list;
    }

}
