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
    private Long size;

    @Schema(description = "当前页")
    private Long current;

    @Schema(description = "总条数")
    private Long total;

    @Schema(description = "总页数")
    private Long pages;
}
