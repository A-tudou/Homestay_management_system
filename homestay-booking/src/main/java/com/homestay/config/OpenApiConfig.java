package com.homestay.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger 文档配置。
 * 声明全局 Bearer JWT 认证方式，便于在 Swagger UI 中填入 token 调试受保护接口。
 */
@Configuration
public class OpenApiConfig {

    /** 构建 OpenAPI 文档，并声明全局 Bearer JWT 认证方案 */
    @Bean
    public OpenAPI homestayOpenApi() {
        SecurityScheme bearer = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");

        return new OpenAPI()
                .info(new Info()
                        .title("民宿预定管理系统 API")
                        .version("v1")
                        .description("房客端与房东端共用的后端接口，登录后点击 Authorize 填入 token 即可调试"))
                .components(new Components().addSecuritySchemes("bearerAuth", bearer))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
