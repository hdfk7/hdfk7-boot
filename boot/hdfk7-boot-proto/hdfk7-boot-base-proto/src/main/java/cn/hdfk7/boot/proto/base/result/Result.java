package cn.hdfk7.boot.proto.base.result;

import cn.hdfk7.boot.proto.base.model.BaseModel;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.io.Serial;

@Getter
@Schema(description = "响应实体")
public class Result<T> extends BaseModel {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "响应CODE")
    private int code;

    @Schema(description = "响应提示信息")
    private String msg;

    @Schema(description = "响应数据")
    private T data;

    public Result() {
        this.code = ResultCode.SUCCESS.getCode();
        this.msg = ResultCode.SUCCESS.getMsg();
    }

    public static Result<Void> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        return success(data, null);
    }

    public static <T> Result<T> success(T data, String msg) {
        return of(ResultCode.SUCCESS, data, msg);
    }

    public static Result<Void> failure() {
        return failure(null);
    }

    public static <T> Result<T> failure(T data) {
        return failure(data, null);
    }

    public static <T> Result<T> failure(T data, String msg) {
        return of(ResultCode.SYS_ERROR, data, msg);
    }

    public static <T> Result<T> of(IResultCode resultCode) {
        return of(resultCode, null);
    }

    public static <T> Result<T> of(IResultCode resultCode, T data) {
        return of(resultCode, data, null);
    }

    public static <T> Result<T> of(IResultCode resultCode, T data, String msg) {
        Result<T> result = new Result<>();
        result.code = resultCode.getCode();
        result.data = data;
        result.msg = msg != null ? msg : resultCode.getMsg();
        return result;
    }

    @JsonIgnore
    public boolean isSuccess() {
        return this.code == ResultCode.SUCCESS.getCode();
    }

    @JsonIgnore
    public boolean isFailure() {
        return !isSuccess();
    }
}
