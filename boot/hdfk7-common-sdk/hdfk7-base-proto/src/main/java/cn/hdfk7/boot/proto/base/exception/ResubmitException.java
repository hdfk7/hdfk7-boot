package cn.hdfk7.boot.proto.base.exception;

import cn.hdfk7.boot.proto.base.result.ResultCode;

public class ResubmitException extends BaseException {
    public ResubmitException() {
        super(ResultCode.RESUBMIT_ERROR);
    }
}
