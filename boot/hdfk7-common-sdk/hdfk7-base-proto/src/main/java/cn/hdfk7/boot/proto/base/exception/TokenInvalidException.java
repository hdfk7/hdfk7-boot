package cn.hdfk7.boot.proto.base.exception;

import cn.hdfk7.boot.proto.base.result.ResultCode;

public class TokenInvalidException extends BaseException {
    public TokenInvalidException() {
        super(ResultCode.TOKEN_INVALID_ERROR);
    }
}
