package ru.hse.sportclassbookingbackend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private final BuildProperties buildProperties;

    public OpenApiConfig(ObjectProvider<BuildProperties> buildProperties) {
        this.buildProperties = buildProperties.getIfAvailable();
    }

    @Bean
    public OpenAPI sportBookingOpenAPI() {
        String version = buildProperties != null ? buildProperties.getVersion() : "dev";
        return new OpenAPI()
                .info(new Info()
                        .title("Sport Class Booking API")
                        .version(version)
                        .description("""
                                REST API для записи студентов НИУ ВШЭ на спортивные занятия.

                                Роли: STUDENT, TEACHER, ADMIN."""))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT access token из /auth/login (без префикса Bearer)")));
    }
}
