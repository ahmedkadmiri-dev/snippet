package com.example.testing_poc.dto;

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