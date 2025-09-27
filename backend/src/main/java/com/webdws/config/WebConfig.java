package com.webdws.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web Configuration for static resource handling
 * 
 * This configuration enables serving static files from the classpath
 * and provides access to schema files and other static resources
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve static files from /static directory
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
        
        // Schema files are served via SchemaController, not as static resources
    }
}
