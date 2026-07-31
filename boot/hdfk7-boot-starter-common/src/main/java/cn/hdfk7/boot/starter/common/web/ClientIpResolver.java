package cn.hdfk7.boot.starter.common.web;

import cn.hdfk7.boot.starter.common.constants.HttpHeaderConst;
import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Set;

@Slf4j
public class ClientIpResolver {
    private static final String LOCALHOST_IPV4 = "127.0.0.1";
    private static final String LOCALHOST_IPV6 = "0:0:0:0:0:0:0:1";
    private static final String IP_SEPARATOR = ",";
    private static final Set<String> LOCALHOST_ADDRESSES = Set.of(LOCALHOST_IPV4, LOCALHOST_IPV6);
    private static final String[] CLIENT_IP_HEADERS = {
            HttpHeaderConst.X_FORWARDED_FOR,
            HttpHeaderConst.PROXY_CLIENT_IP,
            HttpHeaderConst.WL_PROXY_CLIENT_IP,
            HttpHeaderConst.HTTP_CLIENT_IP,
            HttpHeaderConst.HTTP_X_FORWARDED_FOR
    };

    public String getIpAddress(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String ip;
        for (String header : CLIENT_IP_HEADERS) {
            String headerValue = request.getHeader(header);
            if (StrUtil.isBlank(headerValue)) {
                continue;
            }

            for (String item : headerValue.split(IP_SEPARATOR)) {
                ip = StrUtil.trim(item);
                if (isValidIp(ip)) {
                    return ip;
                }
            }
        }

        ip = request.getRemoteAddr();
        if (LOCALHOST_ADDRESSES.contains(ip)) {
            return getLocalHost();
        }
        return isValidIp(ip) ? ip : null;
    }

    public String getLocalHost() {
        try {
            InetAddress inetAddress = InetAddress.getLocalHost();
            return inetAddress.getHostAddress();
        } catch (UnknownHostException e) {
            log.debug("get local host failed", e);
        }
        return null;
    }

    private boolean isValidIp(String ip) {
        return StrUtil.isNotBlank(ip) && !HttpHeaderConst.UNKNOWN.equalsIgnoreCase(ip);
    }
}
