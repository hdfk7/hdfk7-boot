package cn.hdfk7.app.module.component;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class SwaggerComponent {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI().info(new Info().version("1.0.0").description("接口文档"));
    }

    @Bean
    public GroupedOpenApi defaultOpenApi() {
        return GroupedOpenApi.builder()
                .group("default")
                .pathsToMatch("/**")
                .pathsToExclude("/error")
                .build();
    }

}
