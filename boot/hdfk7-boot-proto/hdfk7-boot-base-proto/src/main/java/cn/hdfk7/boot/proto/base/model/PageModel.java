package cn.hdfk7.boot.proto.base.model;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

@Setter
@Getter
public abstract class PageModel extends BaseModel {
    @Serial
    private static final long serialVersionUID = 1L;

    @Min(1)
    @Max(999)
    @NotNull
    @Schema(description = "页大小")
    private Long size;

    @Min(1)
    @NotNull
    @Schema(description = "当前页")
    private Long current;

    public <E> PageDTO<E> of() {
        return new PageDTO<>(current, size);
    }
}
