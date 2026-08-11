package cn.hdfk7.boot.proto.base.exception;

import cn.hdfk7.boot.proto.base.result.IResultCode;
import cn.hdfk7.boot.proto.base.result.ResultCode;
import lombok.Getter;

import java.io.Serial;
import java.util.Objects;

@Getter
public abstract class BaseException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    private final IResultCode resultCode;
    private final Object errorData;

    public BaseException() {
        this((String) null);
    }

    public BaseException(String message) {
        this(ResultCode.SYS_ERROR, message);
    }

    public BaseException(IResultCode resultCode) {
        this(resultCode, null);
    }

    public BaseException(IResultCode resultCode, String message) {
        this(resultCode, message, null);
    }

    public BaseException(IResultCode resultCode, String message, Object errorData) {
        super(message != null ? message : Objects.requireNonNull(resultCode, "IResultCode must not be null").getMsg());
        this.resultCode = resultCode;
        this.errorData = errorData;
    }
}
