package cn.hdfk7.boot.proto.base.result;

import cn.hdfk7.boot.proto.base.model.BaseModel;
import cn.hutool.core.util.StrUtil;
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
        this(ResultCode.SUCCESS);
    }

    public Result(ResultCode resultCode) {
        this(resultCode, null);
    }

    public Result(ResultCode resultCode, String message) {
        this(resultCode, message, null);
    }

    public Result(ResultCode resultCode, String message, T data) {
        bindCode(resultCode.getCode()).bindMsg(StrUtil.isEmpty(message) ? resultCode.getMsg() : message).bindData(data);
    }

    public Result<T> bindData(T data) {
        this.data = data;
        return this;
    }

    public Result<T> bindMsg(String msg) {
        this.msg = msg;
        return this;
    }

    public Result<T> bindCode(int code) {
        this.code = code;
        return this;
    }

    @JsonIgnore
    public boolean isSuccess() {
        return this.code == ResultCode.SUCCESS.getCode();
    }

}
