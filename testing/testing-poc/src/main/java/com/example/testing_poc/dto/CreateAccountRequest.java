package com.example.testing_poc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.antlr.v4.runtime.misc.NotNull;

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