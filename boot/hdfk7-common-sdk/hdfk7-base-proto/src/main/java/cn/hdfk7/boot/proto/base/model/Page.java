package cn.hdfk7.boot.proto.base.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.util.Collections;
import java.util.List;

@Setter
@Getter
public class Page<T> extends BaseModel {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "页数据")
    private List<T> records = Collections.emptyList();

    @Schema(description = "页大小")
    private long size = 10;

    @Schema(description = "当前页")
    private long current = 1;

    @Schema(description = "总条数")
    private long total = 0;

    @Schema(description = "总页数")
    private long pages = 0;
}
