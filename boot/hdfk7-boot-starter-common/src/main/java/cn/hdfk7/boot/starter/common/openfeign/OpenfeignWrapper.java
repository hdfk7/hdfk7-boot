package cn.hdfk7.boot.starter.common.openfeign;

import cn.hdfk7.boot.proto.base.result.Result;
import cn.hdfk7.boot.proto.base.result.ResultCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

public interface OpenfeignWrapper {
    Logger log = LoggerFactory.getLogger(OpenfeignWrapper.class);

    static <T> T exec(Supplier<T> supplier, boolean print) {
        try {
            return supplier.get();
        } catch (Exception e) {
            if (print) {
                log.error(e.getLocalizedMessage(), e);
            }
            throw e;
        }
    }

    static <T> Result<T> get(Supplier<Result<T>> supplier) {
        return get(supplier, true);
    }

    static <T> Result<T> get(Supplier<Result<T>> supplier, boolean print) {
        try {
            return exec(supplier, print);
        } catch (Exception e) {
            Result<T> result = ResultCode.REMOTE_CALL_ERROR.bindResult();
            result.bindMsg(result.getMsg() + " " + e.getLocalizedMessage());
            return result;
        }
    }
}
