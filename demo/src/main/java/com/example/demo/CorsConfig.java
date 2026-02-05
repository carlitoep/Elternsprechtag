package com.example.demo;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 🔴 NICHT /api/**
                .allowedOriginPatterns( // 🔴 NICHT allowedOrigins
                    "https://elternsprechtag-1.onrender.com",
                    "https://elternsprechtag.goetheschule-neu-isenburg.de",
                    "http://localhost:*"
                )
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600);
    }
}
