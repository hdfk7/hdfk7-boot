package cn.hdfk7.boot.proto.base.exception;

import cn.hdfk7.boot.proto.base.result.IResultCode;
import cn.hdfk7.boot.proto.base.result.ResultCode;

import java.util.Objects;

public class BusinessException extends BaseException {
    public BusinessException() {
        this(ResultCode.BUSINESS_ERROR);
    }

    public BusinessException(IResultCode code) {
        this(code, code.getMsg());
    }

    public BusinessException(IResultCode code, String message) {
        this(code, message, null);
    }

    public BusinessException(IResultCode code, String message, Object data) {
        super(Objects.requireNonNull(code, "IResultCode must not be null"), message, data);
    }
}
