package com.project.farming.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        // 보안 스키마 정의 (JWT 토큰 사용 시)
        String jwtSchemeName = "jwtAuth";
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(jwtSchemeName);
        Components components = new Components()
                .addSecuritySchemes(jwtSchemeName, new SecurityScheme()
                        .name(jwtSchemeName)
                        .type(SecurityScheme.Type.HTTP) // HTTP 방식
                        .scheme("bearer") // Bearer 토큰 (JWT)
                        .bearerFormat("JWT")); // JWT 포맷

        return new OpenAPI()
                .info(new Info()
                        .title("Farming API 문서")
                        .description("텃밭 작물 관리 서비스의 REST API 명세서입니다.")
                        .version("1.0.0"))
                .addSecurityItem(securityRequirement) // 모든 API에 보안 적용
                .components(components);
    }
}