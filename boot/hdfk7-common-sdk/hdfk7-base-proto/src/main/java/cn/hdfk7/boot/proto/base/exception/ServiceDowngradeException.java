package cn.hdfk7.boot.proto.base.exception;

import cn.hdfk7.boot.proto.base.result.ResultCode;

public class ServiceDowngradeException extends BaseException {
    public ServiceDowngradeException() {
        super(ResultCode.SERVICE_DOWNGRADE_ERROR);
    }
}
