package com.example.testing_poc.dtos;

import jakarta.validation.constraints.DecimalMin;
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
public class TransferRequest {

    @NotNull(message = "Le compte source est obligatoire")
    private Long sourceAccountId;

    @NotNull(message = "Le compte destinataire est obligatoire")
    private Long destinationAccountId;

    @NotNull(message = "Le montant du virement est obligatoire")
    @DecimalMin(value = "0.01", message = "Le montant doit être supérieur à zéro")
    private BigDecimal amount;

    private String description;
}