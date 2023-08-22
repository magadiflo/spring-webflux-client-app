package com.magadiflo.webflux.client.app.handlers;

import com.magadiflo.webflux.client.app.models.dto.Product;
import com.magadiflo.webflux.client.app.models.services.IProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;

@Component
public class ProductHandler {
    private final IProductService productService;

    public ProductHandler(IProductService productService) {
        this.productService = productService;
    }

    public Mono<ServerResponse> findAllProducts(ServerRequest request) {
        Flux<Product> productFlux = this.productService.findAllProducts();
        return ServerResponse.ok().body(productFlux, Product.class);
    }

    public Mono<ServerResponse> showProduct(ServerRequest request) {
        String id = request.pathVariable("id");
        return this.productService.findProduct(id)
                .flatMap(product -> ServerResponse.ok().bodyValue(product))
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> createProduct(ServerRequest request) {
        String requestPathValue = request.requestPath().value();
        Mono<Product> productMono = request.bodyToMono(Product.class);
        return productMono
                .flatMap(this.productService::saveProduct)
                .flatMap(productDB -> ServerResponse
                        .created(URI.create(requestPathValue + "/" + productDB.id()))
                        .bodyValue(productDB)
                );
    }

    public Mono<ServerResponse> createProductWithValidation(ServerRequest request) {
        String requestPathValue = request.requestPath().value();
        Mono<Product> productMono = request.bodyToMono(Product.class);
        return productMono
                .flatMap(this.productService::saveProductWithValidation)
                .flatMap(productDB -> ServerResponse
                        .created(URI.create(requestPathValue + "/" + productDB.id()))
                        .bodyValue(productDB)
                )
                .onErrorResume(throwable -> {
                    WebClientResponseException responseException = (WebClientResponseException) throwable;
                    if (responseException.getStatusCode().equals(HttpStatus.BAD_REQUEST)) {
                        return ServerResponse.badRequest()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(responseException.getResponseBodyAsString(StandardCharsets.UTF_8));
                    }
                    return Mono.error(responseException);
                });
    }

    public Mono<ServerResponse> updateProduct(ServerRequest request) {
        String id = request.pathVariable("id");
        Mono<Product> productMono = request.bodyToMono(Product.class);
        return productMono
                .flatMap(product -> this.productService.updateProduct(id, product))
                .flatMap(updatedProductDB -> ServerResponse.ok().bodyValue(updatedProductDB))
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> deleteProduct(ServerRequest request) {
        String id = request.pathVariable("id");
        return this.productService.deleteProduct(id)
                .flatMap(wasDeleted -> wasDeleted ? Mono.just(true) : Mono.empty())
                .flatMap(wasDeleted -> ServerResponse.noContent().build())
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> imageUpload(ServerRequest request) {
        String id = request.pathVariable("id");
        return request.multipartData()
                .map(MultiValueMap::toSingleValueMap)
                .map(stringPartMap -> stringPartMap.get("imageFile"))
                .cast(FilePart.class)
                .flatMap(filePart -> this.productService.imageUpload(id, filePart))
                .flatMap(productDB -> ServerResponse.ok().bodyValue(productDB))
                .switchIfEmpty(ServerResponse.notFound().build());
    }
}
