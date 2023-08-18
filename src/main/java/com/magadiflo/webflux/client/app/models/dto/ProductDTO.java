package com.magadiflo.webflux.client.app.models.dto;

import java.time.LocalDate;

public record ProductDTO(String id, String name, Double price, LocalDate createAt, String image, CategoryDTO categoryDTO) {
}
