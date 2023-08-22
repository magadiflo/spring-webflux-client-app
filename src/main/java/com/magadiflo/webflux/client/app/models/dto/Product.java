package com.magadiflo.webflux.client.app.models.dto;

import java.time.LocalDate;

public record Product(String id, String name, Double price, LocalDate createAt, String image, Category category) {
}
