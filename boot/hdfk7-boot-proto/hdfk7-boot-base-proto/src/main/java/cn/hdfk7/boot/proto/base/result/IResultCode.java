package cn.hdfk7.boot.proto.base.result;

public interface IResultCode {
    int getCode();

    String getMsg();

    default <T> Result<T> toResult() {
        return toResult(null);
    }

    default <T> Result<T> toResult(T data) {
        return toResult(data, null);
    }

    default <T> Result<T> toResult(T data, String msg) {
        return Result.of(this, data, msg);
    }
}
