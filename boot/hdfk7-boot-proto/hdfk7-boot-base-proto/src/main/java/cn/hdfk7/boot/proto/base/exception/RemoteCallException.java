package cn.hdfk7.boot.proto.base.exception;

import cn.hdfk7.boot.proto.base.result.ResultCode;

public class RemoteCallException extends BaseException {
    public RemoteCallException() {
        super(ResultCode.REMOTE_CALL_ERROR);
    }
}
