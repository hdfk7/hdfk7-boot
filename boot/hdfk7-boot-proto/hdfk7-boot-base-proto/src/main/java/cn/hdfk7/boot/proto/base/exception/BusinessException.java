package cn.hdfk7.boot.proto.base.exception;

import cn.hdfk7.boot.proto.base.result.IResultCode;
import cn.hdfk7.boot.proto.base.result.ResultCode;

public class BusinessException extends BaseException {
    public BusinessException() {
        this((String) null);
    }

    public BusinessException(String message) {
        this(ResultCode.BUSINESS_ERROR, message);
    }

    public BusinessException(IResultCode resultCode) {
        this(resultCode, resultCode.getMsg());
    }

    public BusinessException(IResultCode resultCode, String message) {
        this(resultCode, message, null);
    }

    public BusinessException(IResultCode resultCode, String message, Object data) {
        super(resultCode, message, data);
    }
}
