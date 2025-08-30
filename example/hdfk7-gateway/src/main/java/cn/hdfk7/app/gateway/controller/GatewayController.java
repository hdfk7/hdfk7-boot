package cn.hdfk7.app.gateway.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.reactive.result.view.Rendering;

@Tag(name = "网关接口")
@Slf4j
@Controller
@RequestMapping
public class GatewayController {

    @Operation(summary = "根路径重定向到api文档")
    @GetMapping
    public Rendering toDoc() {
        return Rendering.redirectTo("doc.html").build();
    }
}
