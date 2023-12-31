package com.magadiflo.webflux.client.app.models.services;

import com.magadiflo.webflux.client.app.models.dto.Product;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface IProductService {
    Flux<Product> findAllProducts();

    Mono<Product> findProduct(String id);

    Mono<Product> saveProduct(Product product);

    Mono<Product> saveProductWithValidation(Product product);

    Mono<Product> updateProduct(String id, Product product);

    Mono<Boolean> deleteProduct(String id);

    Mono<Product> imageUpload(String id, FilePart imageFile);
}
