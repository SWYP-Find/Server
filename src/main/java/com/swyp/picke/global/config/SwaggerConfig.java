package com.swyp.picke.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    private static final Set<String> FE_USED_OPERATIONS = Set.of(
            "POST /api/v1/auth/refresh",
            "POST /api/v1/auth/login/{provider}",
            "POST /api/v1/auth/logout",
            "DELETE /api/v1/me",
            "GET /api/v1/home",
            "GET /api/v1/battles/{battleId}",
            "GET /api/v1/battles/{battleId}/status",
            "GET /api/v1/battles/today",
            "GET /api/v1/search/battles",
            "GET /api/v1/battles/{battleId}/perspectives",
            "POST /api/v1/battles/{battleId}/perspectives",
            "GET /api/v1/battles/{battleId}/perspectives/me",
            "GET /api/v1/perspectives/{perspectiveId}",
            "DELETE /api/v1/perspectives/{perspectiveId}",
            "PATCH /api/v1/perspectives/{perspectiveId}",
            "POST /api/v1/perspectives/{perspectiveId}/moderation/retry",
            "GET /api/v1/perspectives/{perspectiveId}/likes",
            "POST /api/v1/perspectives/{perspectiveId}/likes",
            "DELETE /api/v1/perspectives/{perspectiveId}/likes",
            "POST /api/v1/perspectives/{perspectiveId}/reports",
            "GET /api/v1/perspectives/{perspectiveId}/comments/labeled",
            "POST /api/v1/perspectives/{perspectiveId}/comments",
            "DELETE /api/v1/perspectives/{perspectiveId}/comments/{commentId}",
            "PATCH /api/v1/perspectives/{perspectiveId}/comments/{commentId}",
            "POST /api/v1/comments/{commentId}/likes",
            "DELETE /api/v1/comments/{commentId}/likes",
            "POST /api/v1/perspectives/{perspectiveId}/comments/{commentId}/reports",
            "POST /api/v1/battles/{battleId}/votes/pre",
            "POST /api/v1/battles/{battleId}/votes/post",
            "GET /api/v1/battles/{battleId}/vote-stats",
            "GET /api/v1/battles/{battleId}/votes/me",
            "POST /api/v1/battles/{battleId}/poll-vote",
            "GET /api/v1/battles/{battleId}/poll-vote/me",
            "POST /api/v1/battles/{battleId}/quiz-vote",
            "GET /api/v1/battles/{battleId}/quiz-vote/me",
            "GET /api/v1/notifications",
            "GET /api/v1/notifications/{notificationId}",
            "PATCH /api/v1/notifications/{notificationId}/read",
            "PATCH /api/v1/notifications/read-all",
            "GET /api/v1/me/battle-records",
            "GET /api/v1/me/content-activities",
            "GET /api/v1/me/mypage",
            "GET /api/v1/me/notification-settings",
            "PATCH /api/v1/me/notification-settings",
            "PATCH /api/v1/me/profile",
            "GET /api/v1/me/recap",
            "GET /api/v1/me/credits/history",
            "GET /api/v1/share/report",
            "GET /api/v1/share/battle",
            "GET /api/v1/share/recap",
            "GET /api/v1/share/recap/{shareKey}",
            "POST /api/v1/battles/proposals",
            "GET /api/v1/battles/{battleId}/recommendations/interesting",
            "GET /api/v1/battles/{battleId}/scenario",
            // Attendance
            "POST /api/v1/attendance/check",
            "GET /api/v1/attendance/weekly",
            "GET /api/v1/attendance/summary"
    );

    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .group("1. 사용자 API")
                .pathsToMatch("/api/v1/**")
                .pathsToExclude("/api/v1/admin/**", "/api/v1/files/**", "/api/v1/resources/**", "/api/test/**", "/api/v1/admob/**", "/api/v1/ads/**")
                .addOpenApiCustomizer(feUsedApiOnlyCustomizer())
                .build();
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("2. 관리자 API")
                .pathsToMatch("/api/v1/admin/**", "/api/v1/files/**", "/api/v1/resources/**", "/api/test/**", "/api/v1/admob/**")
                .pathsToExclude("/api/v1/admin/ads/**")
                .build();
    }

    /**
     * 제휴 광고는 별도 그룹으로 띄운다.
     *
     * <p>사용자 그룹은 FE_USED_OPERATIONS 화이트리스트로 걸러지므로 거기에 넣으면 어차피 보이지 않는다.
     * 앱용과 관리자용을 한 그룹에 모아 광고 연동만 따로 볼 수 있게 한다.
     */
    @Bean
    public GroupedOpenApi adApi() {
        return GroupedOpenApi.builder()
                .group("3. 광고 API")
                .pathsToMatch("/api/v1/ads", "/api/v1/ads/**", "/api/v1/admin/ads", "/api/v1/admin/ads/**")
                .build();
    }

    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
                .group("0. 모든 API")
                .pathsToMatch("/api/**")
                .build();
    }

    @Bean
    public OpenAPI openAPI() {
        // 1. 운영 서버 (8080)
        Server prodServer = new Server()
                .url("https://picke.store")
                .description("Production Server");

        // 2. 로컬 개발 서버 (8080)
        Server local8080 = new Server()
                .url("http://localhost:8080")
                .description("Local Development Server (8080)");

        // 3. 개발 서버 (8081)
        Server devServer = new Server()
                .url("https://dev.picke.store")
                .description("Remote Dev Server (8081)");

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
                .servers(List.of(prodServer, local8080, devServer))
                .info(new Info()
                              .title("PIQUE API 명세서")
                              .description("PIQUE 서비스 API 명세서입니다.")
                              .version("v1.0.0"))
                .components(new Components()
                                    .addSecuritySchemes("bearerAuth", securityScheme))
                .addSecurityItem(securityRequirement);
    }

    public OpenApiCustomizer feUsedApiOnlyCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }

            List<String> emptyPaths = new ArrayList<>();

            openApi.getPaths().forEach((path, pathItem) -> {
                List<PathItem.HttpMethod> methods =
                        new ArrayList<>(pathItem.readOperationsMap().keySet());

                for (PathItem.HttpMethod method : methods) {
                    String operationKey = method.name() + " " + path;
                    if (!FE_USED_OPERATIONS.contains(operationKey)) {
                        clearOperation(pathItem, method);
                    }
                }

                if (pathItem.readOperationsMap().isEmpty()) {
                    emptyPaths.add(path);
                }
            });

            emptyPaths.forEach(openApi.getPaths()::remove);
            removeUnusedTags(openApi);
        };
    }

    private void removeUnusedTags(OpenAPI openApi) {
        if (openApi.getTags() == null || openApi.getPaths() == null) {
            return;
        }

        Set<String> usedTags = new HashSet<>();
        openApi.getPaths().values().forEach(pathItem ->
                pathItem.readOperations().forEach(operation -> {
                    if (operation.getTags() != null) {
                        usedTags.addAll(operation.getTags());
                    }
                })
        );

        openApi.setTags(openApi.getTags().stream()
                .filter(tag -> usedTags.contains(tag.getName()))
                .toList());
    }

    private void clearOperation(PathItem pathItem, PathItem.HttpMethod method) {
        switch (method) {
            case GET -> pathItem.setGet(null);
            case PUT -> pathItem.setPut(null);
            case POST -> pathItem.setPost(null);
            case DELETE -> pathItem.setDelete(null);
            case OPTIONS -> pathItem.setOptions(null);
            case HEAD -> pathItem.setHead(null);
            case PATCH -> pathItem.setPatch(null);
            case TRACE -> pathItem.setTrace(null);
        }
    }
}
