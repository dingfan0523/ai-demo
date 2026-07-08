package com.aidemo.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 启动时打印 Swagger 文档地址
 */
@Slf4j
@Component
public class SwaggerStartupPrinter implements ApplicationRunner {

    private final Environment environment;

    public SwaggerStartupPrinter(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        String port = environment.getProperty("server.port", "8080");
        String swaggerUrl = "http://localhost:" + port + "/swagger-ui/index.html";
        String apiDocsUrl = "http://localhost:" + port + "/v3/api-docs";

        log.info("==================================================");
        log.info("Swagger UI: {}", swaggerUrl);
        log.info("API Docs:   {}", apiDocsUrl);
        log.info("==================================================");
    }
}
