package com.magadiflo.webflux.client.app.models.services;

import com.magadiflo.webflux.client.app.models.dto.ProductDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface IProductService {
    Flux<ProductDTO> findAllProducts();

    Mono<ProductDTO> findProduct(String id);

    Mono<ProductDTO> saveProduct(ProductDTO productDTO);

    Mono<ProductDTO> updateProduct(String id, ProductDTO productDTO);

    Mono<Void> deleteProduct(String id);
}
