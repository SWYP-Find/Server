package com.swyp.picke.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        // 1. 운영 서버 주소 명시
        Server prodServer = new Server();
        prodServer.setUrl("https://picke.store");
        prodServer.setDescription("Production Server");

        // 2. 로컬 테스트용 서버 주소
        Server localServer = new Server();
        localServer.setUrl("http://localhost:8080");
        localServer.setDescription("Local Development Server");

        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name("Authorization");

        SecurityRequirement securityRequirement =
                new SecurityRequirement().addList("bearerAuth");

        return new OpenAPI()
                // 3. 서버 리스트 등록
                .servers(List.of(prodServer, localServer))
                .info(new Info()
                              .title("PIQUE API 명세서")
                              .description("PIQUE 서비스 API 명세서입니다.")
                              .version("v1.0.0"))
                .components(new Components()
                                    .addSecuritySchemes("bearerAuth", securityScheme))
                .addSecurityItem(securityRequirement);
    }
}