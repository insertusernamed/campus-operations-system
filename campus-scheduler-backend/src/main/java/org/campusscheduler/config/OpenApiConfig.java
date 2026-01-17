package org.campusscheduler.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for API documentation.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI campusSchedulerOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Campus Scheduler API")
                        .description("REST API for university classroom and space utilization management")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Campus Scheduler Team")
                                .email("support@campusscheduler.org"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Development server")));
    }
}
