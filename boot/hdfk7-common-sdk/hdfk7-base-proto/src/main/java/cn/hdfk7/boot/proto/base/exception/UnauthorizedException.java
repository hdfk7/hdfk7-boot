package cn.hdfk7.boot.proto.base.exception;

import cn.hdfk7.boot.proto.base.result.ResultCode;

public class UnauthorizedException extends BaseException {
    public UnauthorizedException() {
        super(ResultCode.UNAUTHORIZED_ERROR);
    }
}
