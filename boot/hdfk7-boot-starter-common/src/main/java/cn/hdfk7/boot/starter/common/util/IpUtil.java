package cn.hdfk7.boot.starter.common.util;

import cn.hdfk7.boot.starter.common.constants.HttpHeaderConst;
import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Slf4j
public class IpUtil {
    private static final String LOCAL_HOST = "127.0.0.1";
    private static final String INVALID_IP = "0:0:0:0:0:0:0:1";

    public static String getIpAddress(HttpServletRequest request) {
        // Nginx的反向代理标志
        String ip = request.getHeader(HttpHeaderConst.X_FORWARDED_FOR);
        if (!isValid(ip)) {
            // Apache的反向代理标志
            ip = request.getHeader(HttpHeaderConst.PROXY_CLIENT_IP);
        }
        if (!isValid(ip)) {
            // WebLogic的反向代理标志
            ip = request.getHeader(HttpHeaderConst.WL_PROXY_CLIENT_IP);
        }
        if (!isValid(ip)) {
            // 较少出现
            ip = request.getHeader(HttpHeaderConst.HTTP_CLIENT_IP);
        }
        if (!isValid(ip)) {
            ip = request.getHeader(HttpHeaderConst.HTTP_X_FORWARDED_FOR);
        }
        if (!isValid(ip)) {
            ip = request.getRemoteAddr();
            if (LOCAL_HOST.equals(ip) || INVALID_IP.equals(ip)) {
                ip = getLocalHost();
            }
        }

        // 对于通过多个代理的情况，第一个IP为客户端真实IP,多个IP按照','分割
        if (ip != null && ip.indexOf(",") > 0) {
            ip = ip.substring(0, ip.indexOf(","));
        }

        return ip;
    }

    public static String getLocalHost() {
        String ip = null;
        // 根据网卡取本机配置的IP
        InetAddress inetAddress;
        try {
            inetAddress = InetAddress.getLocalHost();
            ip = inetAddress.getHostAddress();
        } catch (UnknownHostException e) {
            log.debug("getLocalHost failed!", e);
        }

        return ip;
    }

    private static boolean isValid(String ip) {
        return !(StrUtil.isEmpty(ip) || HttpHeaderConst.UNKNOWN.equalsIgnoreCase(ip));
    }
}
