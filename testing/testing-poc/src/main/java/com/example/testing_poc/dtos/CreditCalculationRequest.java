package com.example.testing_poc.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
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
public class CreditCalculationRequest {

    @NotNull(message = "Le montant du crédit est obligatoire")
    @DecimalMin(value = "0.01", message = "Le montant doit être supérieur à zéro")
    private BigDecimal amount;

    @NotNull(message = "La durée est obligatoire")
    @Min(value = 1, message = "La durée doit être supérieure à zéro")
    private Integer durationInMonths;

    @NotNull(message = "Le taux d'intérêt est obligatoire")
    @DecimalMin(value = "0.00", message = "Le taux doit être positif ou nul")
    private BigDecimal annualInterestRate;
}