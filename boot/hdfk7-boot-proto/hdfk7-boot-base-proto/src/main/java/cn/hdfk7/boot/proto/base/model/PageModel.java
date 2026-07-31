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

    @Min(value = 1, message = "页大小不能小于1")
    @Max(value = 999, message = "页大小不能大于999")
    @NotNull(message = "页大小不能为空")
    @Schema(description = "页大小")
    private Long size;

    @Min(value = 1, message = "当前页不能小于1")
    @NotNull(message = "当前页不能为空")
    @Schema(description = "当前页")
    private Long current;

    public <E> PageDTO<E> of() {
        return new PageDTO<>(current, size);
    }
}
