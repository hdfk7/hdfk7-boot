package cn.hdfk7.boot.proto.base.exception;

import cn.hdfk7.boot.proto.base.result.ResultCode;

import java.io.Serial;

public abstract class BaseException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    public ResultCode code;
    public Object errorData;

    public BaseException() {
        this(ResultCode.SYS_ERROR);
    }

    public BaseException(String... message) {
        this(ResultCode.SYS_ERROR, String.join(",", message));
    }

    public BaseException(ResultCode code) {
        this(code.getMsg());
        this.code = code;
    }

    public BaseException(ResultCode code, String message) {
        super(message);
        this.code = code;
    }

    public BaseException(ResultCode code, Object errorData) {
        super(code.getMsg());
        this.code = code;
        this.errorData = errorData;
    }

    public BaseException(ResultCode code, String message, Object errorData) {
        super(message);
        this.code = code;
        this.errorData = errorData;
    }
}
