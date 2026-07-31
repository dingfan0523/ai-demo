package com.aidemo.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 启动时打印接口文档地址。
 */
@Slf4j
@Component
public class ApiDocsStartupPrinter implements ApplicationRunner {

    private final Environment environment;

    public ApiDocsStartupPrinter(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        String port = environment.getProperty("server.port", "8080");
        String knife4jUrl = "http://localhost:" + port + "/doc.html";
        String apiDocsUrl = "http://localhost:" + port + "/v3/api-docs";

        log.info("==================================================");
        log.info("Knife4j UI: {}", knife4jUrl);
        log.info("OpenAPI:   {}", apiDocsUrl);
        log.info("==================================================");
    }
}
