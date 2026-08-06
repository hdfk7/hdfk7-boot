package cn.hdfk7.boot.starter.common.web;

import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.StrUtil;

public class ClientIpResolver {
    public String getIpAddress(String realIp, String remoteAddress) {
        realIp = StrUtil.trim(realIp);
        if (isValidIp(realIp)) {
            return realIp;
        }

        remoteAddress = StrUtil.trim(remoteAddress);
        return isValidIp(remoteAddress) ? remoteAddress : null;
    }

    private boolean isValidIp(String ip) {
        return Validator.isIpv4(ip) || Validator.isIpv6(ip);
    }
}
