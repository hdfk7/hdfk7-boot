package cn.hdfk7.boot.proto.base.result;

import cn.hdfk7.boot.proto.base.model.Page;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import lombok.Getter;

import java.util.List;

@Getter
public enum ResultCode {
    SYS_ERROR(-1, "前方路滑请稍后再试"),
    SUCCESS(0, "成功"),
    RESUBMIT_ERROR(1, "重复请求"),
    REMOTE_CALL_ERROR(2, "远程调用异常"),
    SERVICE_DOWNGRADE_ERROR(3, "服务降级"),
    TOKEN_INVALID_ERROR(4, "无效令牌"),
    UNAUTHORIZED_ERROR(5, "未授权"),
    ;

    private int code;
    private String msg;

    ResultCode(int code, String msg) {
        bindCode(code).bindMsg(msg);
    }

    public ResultCode bindMsg(String msg) {
        this.msg = msg;
        return this;
    }

    public ResultCode bindCode(int code) {
        this.code = code;
        return this;
    }

    public <T> Result<T> bindResult(Result<T> result) {
        return result.bindCode(this.code).bindMsg(this.msg);
    }

    public <T> Result<T> bindResult(T data) {
        return new Result<T>().bindCode(this.code).bindMsg(this.msg).bindData(data);
    }

    public <T> Result<T> bindResult(Object data, Class<T> clazz) {
        T o = JSONUtil.toBean(JSONUtil.toJsonStr(data), clazz);
        return new Result<T>().bindCode(this.code).bindMsg(this.msg).bindData(o);
    }

    public <T> Result<List<T>> bindResult(List<?> data, Class<T> clazz) {
        List<T> o = JSONUtil.toList(JSONUtil.toJsonStr(data), clazz);
        return new Result<List<T>>().bindCode(this.code).bindMsg(this.msg).bindData(o);
    }

    @SuppressWarnings("unchecked")
    public <T> Result<Page<T>> bindResult(PageDTO<?> page, Class<T> clazz) {
        List<T> list = JSONUtil.toList(JSONUtil.toJsonStr(page.getRecords()), clazz);
        page.setRecords(null);
        Page<T> o = JSONUtil.toBean(JSONUtil.toJsonStr(page), Page.class);
        o.setRecords(list);
        return new Result<Page<T>>().bindCode(this.code).bindMsg(this.msg).bindData(o);
    }

    public <T> Result<T> bindResult() {
        return bindResult((T) null);
    }

}
