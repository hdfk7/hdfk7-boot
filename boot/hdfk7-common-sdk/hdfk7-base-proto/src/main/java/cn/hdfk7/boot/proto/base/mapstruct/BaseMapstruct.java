package cn.hdfk7.boot.proto.base.mapstruct;

import cn.hdfk7.boot.proto.base.model.Page;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;

import java.util.List;

public interface BaseMapstruct<S, T> {
    T source2target(S parm);

    S target2source(T parm);

    List<T> source2target(List<S> parm);

    List<S> target2source(List<T> parm);

    Page<T> source2target(PageDTO<S> parm);

    Page<S> target2source(PageDTO<T> parm);

}
