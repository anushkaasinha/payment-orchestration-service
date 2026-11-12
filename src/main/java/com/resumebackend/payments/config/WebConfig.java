package com.resumebackend.payments.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addViewControllers(ViewControllerRegistry r) {
        r.addRedirectViewController("/", "/app/");
        r.addRedirectViewController("/app", "/app/");
        r.addViewController("/app/").setViewName("forward:/app/index.html");
    }
}
