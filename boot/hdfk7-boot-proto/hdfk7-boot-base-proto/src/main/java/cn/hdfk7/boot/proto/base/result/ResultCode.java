package cn.hdfk7.boot.proto.base.result;

import lombok.Getter;

@Getter
public enum ResultCode implements IResultCode {
    SYS_ERROR(-1, "前方路滑请稍后再试"),
    SUCCESS(0, "成功"),
    RESUBMIT_ERROR(1, "重复请求"),
    REMOTE_CALL_ERROR(2, "远程调用异常"),
    SERVICE_DOWNGRADE_ERROR(3, "服务降级"),
    TOKEN_INVALID_ERROR(4, "无效令牌"),
    UNAUTHORIZED_ERROR(5, "未授权"),
    BUSINESS_ERROR(6, "业务异常"),
    ;

    private final int code;
    private final String msg;

    ResultCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
