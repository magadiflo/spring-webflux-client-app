package com.magadiflo.webflux.client.app.models.services;

import com.magadiflo.webflux.client.app.models.dto.Product;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.codec.multipart.FilePart;
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
    public Flux<Product> findAllProducts() {
        return this.client.get()
                .accept(MediaType.APPLICATION_JSON)
                .exchangeToFlux(response -> response.bodyToFlux(Product.class));
    }

    @Override
    public Mono<Product> findProduct(String id) {
        return this.client.get().uri("/{id}", Collections.singletonMap("id", id))
                .accept(MediaType.APPLICATION_JSON)
                .exchangeToMono(response -> response.bodyToMono(Product.class));
    }

    @Override
    public Mono<Product> saveProduct(Product product) {
        return this.client.post()
                .contentType(MediaType.APPLICATION_JSON)    // <-- tipo de contenido que enviamos en el Request
                .accept(MediaType.APPLICATION_JSON)// <-- tipo de contenido que aceptamos en el Response
                .bodyValue(product)
                .exchangeToMono(response -> response.bodyToMono(Product.class));
    }

    @Override
    public Mono<Product> saveProductWithValidation(Product product) {
        return this.client.post().uri("/create-product-with-validation")
                .contentType(MediaType.APPLICATION_JSON)    // <-- tipo de contenido que enviamos en el Request
                .accept(MediaType.APPLICATION_JSON)// <-- tipo de contenido que aceptamos en el Response
                .bodyValue(product)
                .exchangeToMono(response -> {
                    if (response.statusCode().equals(HttpStatus.CREATED)) {
                        return response.bodyToMono(Product.class);
                    }
                    return response.createError();
                });
    }

    @Override
    public Mono<Product> updateProduct(String id, Product product) {
        return this.client.put().uri("/{id}", Collections.singletonMap("id", id))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(product)
                .exchangeToMono(response -> response.bodyToMono(Product.class));
    }

    @Override
    public Mono<Boolean> deleteProduct(String id) {
        return this.client.delete().uri("/{id}", Collections.singletonMap("id", id))
                .exchangeToMono(response -> response.statusCode().equals(HttpStatus.NO_CONTENT) ? Mono.just(true) : Mono.just(false));
    }

    @Override
    public Mono<Product> imageUpload(String id, FilePart imageFile) {
        MultipartBodyBuilder parts = new MultipartBodyBuilder();
        parts.asyncPart("imageFile", imageFile.content(), DataBuffer.class)
                .headers(httpHeaders -> {
                    httpHeaders.setContentDispositionFormData("imageFile", imageFile.filename());
                });
        return this.client.post().uri("/upload/{id}", Collections.singletonMap("id", id))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(parts.build())
                .exchangeToMono(response -> response.bodyToMono(Product.class));
    }
}
