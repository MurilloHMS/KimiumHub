package com.proautokimium.api.Infrastructure.settings;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {
    @Value("${storage.image.path}")
    private String productsPath;

    @Value("${storage.equipment.image.path}")
    private String equipmentPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry){
        registry.addResourceHandler("/upload/images/**")
                .addResourceLocations("file:" + productsPath + "/");

        registry.addResourceHandler("/upload/equipment/images/**")
                .addResourceLocations("file:" + equipmentPath + "/");
    }
}
