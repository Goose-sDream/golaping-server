package com.goosesdream.golaping.configuration

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

//@Configuration
//class WebConfig : WebMvcConfigurer {
//    override fun addCorsMappings(registry: CorsRegistry) {
//        registry.addMapping("/**")
//            .allowedOrigins("http://localhost:3300", "http://golaping.site")
//            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
//            .allowedHeaders("*")
//            .allowCredentials(true) // 쿠키
//    }
//}