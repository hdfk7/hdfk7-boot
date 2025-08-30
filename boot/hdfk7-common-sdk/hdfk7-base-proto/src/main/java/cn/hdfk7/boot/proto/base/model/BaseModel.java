package cn.hdfk7.boot.proto.base.model;

import cn.hutool.json.JSONUtil;

import java.io.Serial;
import java.io.Serializable;

public abstract class BaseModel implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    public String toString() {
        return JSONUtil.toJsonStr(this);
    }

}
