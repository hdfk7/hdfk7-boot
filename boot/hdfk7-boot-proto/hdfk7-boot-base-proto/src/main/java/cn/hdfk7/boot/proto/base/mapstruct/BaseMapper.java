package cn.hdfk7.boot.proto.base.mapstruct;

import cn.hdfk7.boot.proto.base.model.Page;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;

import java.util.List;

public interface BaseMapper<E, D> {
    D toDto(E entity);

    E toEntity(D dto);

    List<D> toDtoList(List<E> entityList);

    List<E> toEntityList(List<D> dtoList);

    Page<D> toDtoPage(PageDTO<E> entityPage);

    Page<E> toEntityPage(PageDTO<D> dtoPage);
}
