package com.magadiflo.webflux.client.app.handlers.config;

import com.magadiflo.webflux.client.app.handlers.ProductHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.*;

@Configuration
public class RouterConfig {
    @Bean
    public RouterFunction<ServerResponse> routes(ProductHandler handler) {
        return RouterFunctions.route(RequestPredicates.GET("/api/v1/client-app"), handler::findAllProducts)
                .andRoute(RequestPredicates.GET("/api/v1/client-app/{id}"), handler::showProduct)
                .andRoute(RequestPredicates.POST("/api/v1/client-app"), handler::createProduct)
                .andRoute(RequestPredicates.POST("/api/v1/client-app/create-product-with-validation"), handler::createProductWithValidation)
                .andRoute(RequestPredicates.PUT("/api/v1/client-app/{id}"), handler::updateProduct)
                .andRoute(RequestPredicates.DELETE("/api/v1/client-app/{id}"), handler::deleteProduct)
                .andRoute(RequestPredicates.POST("/api/v1/client-app/upload/{id}"), handler::imageUpload);
    }
}
