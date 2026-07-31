package com.aidemo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Knife4j 文档配置。
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI aiDemoOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI Demo 接口文档")
                        .description("用于学习 AI 问答、RAG 检索增强生成和 MCP 工具接入的调试接口。")
                        .version("1.0.0")
                        .contact(new Contact().name("ai-demo"))
                        .license(new License().name("Apache 2.0")));
    }

    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
                .group("00 全部接口")
                .pathsToMatch("/api/**")
                .build();
    }

    @Bean
    public GroupedOpenApi chatApi() {
        return GroupedOpenApi.builder()
                .group("AI 问答")
                .pathsToMatch("/api/chat/**")
                .build();
    }

    @Bean
    public GroupedOpenApi ragApi() {
        return GroupedOpenApi.builder()
                .group("RAG 学习")
                .pathsToMatch("/api/rag/**")
                .build();
    }

}
