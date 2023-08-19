package com.magadiflo.webflux.client.app.models.services;

import com.magadiflo.webflux.client.app.models.dto.ProductDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;

@Service
public class ProductServiceImpl implements IProductService {

    private final WebClient client;

    public ProductServiceImpl(WebClient client) {
        this.client = client;
    }

    @Override
    public Flux<ProductDTO> findAllProducts() {
        return this.client.get()
                .accept(MediaType.APPLICATION_JSON)
                .exchangeToFlux(response -> response.bodyToFlux(ProductDTO.class));
    }

    @Override
    public Mono<ProductDTO> findProduct(String id) {
        return this.client.get().uri("/{id}", Collections.singletonMap("id", id))
                .accept(MediaType.APPLICATION_JSON)
                .exchangeToMono(response -> {
                    if (response.statusCode().equals(HttpStatus.OK)) {
                        return response.bodyToMono(ProductDTO.class);
                    }
                    return response.createError();
                });
    }

    @Override
    public Mono<ProductDTO> saveProduct(ProductDTO productDTO) {
        return this.client.post()
                .contentType(MediaType.APPLICATION_JSON)    // <-- tipo de contenido que enviamos en el Request
                .accept(MediaType.APPLICATION_JSON)// <-- tipo de contenido que aceptamos en el Response
                .bodyValue(productDTO)
                .exchangeToMono(response -> {
                    if (response.statusCode().equals(HttpStatus.CREATED)) {
                        return response.bodyToMono(ProductDTO.class);
                    }
                    return response.createError();
                });
    }

    @Override
    public Mono<ProductDTO> updateProduct(String id, ProductDTO productDTO) {
        return this.client.put().uri("/{id}", Collections.singletonMap("id", id))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(productDTO)
                .exchangeToMono(response -> {
                    if (response.statusCode().equals(HttpStatus.OK)) {
                        return response.bodyToMono(ProductDTO.class);
                    }
                    return response.createError();
                });
    }

    @Override
    public Mono<Void> deleteProduct(String id) {
        return this.client.delete().uri("/{id}", Collections.singletonMap("id", id))
                .exchangeToMono(response -> {
                    if (response.statusCode().equals(HttpStatus.NO_CONTENT)) {
                        return response.bodyToMono(Void.class);
                    }
                    return response.createError();
                });
    }
}
