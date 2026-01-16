package com.atlantic.esocial;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class EsocialServicosInforendimentosApplication
        extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(EsocialServicosInforendimentosApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(EsocialServicosInforendimentosApplication.class, args);
    }
}
