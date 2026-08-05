package cn.hdfk7.boot.proto.base.exception;

import cn.hdfk7.boot.proto.base.result.IResultCode;
import cn.hdfk7.boot.proto.base.result.ResultCode;

import java.io.Serial;

public abstract class BaseException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    public IResultCode code;
    public Object errorData;

    public BaseException() {
        this(ResultCode.SYS_ERROR);
    }

    public BaseException(String... message) {
        this(ResultCode.SYS_ERROR, String.join(",", message));
    }

    public BaseException(IResultCode code) {
        this(code, code.getMsg());
    }

    public BaseException(IResultCode code, String message) {
        this(code, message, null);
    }

    public BaseException(IResultCode code, Object errorData) {
        this(code, code.getMsg(), errorData);
    }

    public BaseException(IResultCode code, String message, Object errorData) {
        super(message);
        this.code = code;
        this.errorData = errorData;
    }
}
