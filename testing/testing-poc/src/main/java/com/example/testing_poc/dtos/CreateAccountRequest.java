package com.example.testing_poc.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAccountRequest {

    @NotBlank(message = "Le nom du titulaire est obligatoire")
    private String ownerName;

    @NotBlank(message = "La devise est obligatoire")
    private String currency;

    @NotNull(message = "Le dépôt initial est obligatoire")
    @DecimalMin(value = "0.00", message = "Le dépôt initial doit être positif ou nul")
    private BigDecimal initialBalance;
}