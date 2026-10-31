package com.example.testing_poc.controller;

import com.example.testing_poc.dto.*;
import com.example.testing_poc.service.BankService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bank")
@RequiredArgsConstructor
public class BankController {

    private final BankService bankService;

    /**
     * Récupérer un référentiel bancaire.
     */
    @GetMapping("/referentiels/{type}")
    public ResponseEntity<List<ReferentielDto>> getReferentiel(
            @PathVariable String type) {

        return ResponseEntity.ok(bankService.getReferentiel(type));
    }

    /**
     * Récupérer un compte bancaire par son identifiant.
     */
    @GetMapping("/accounts/{id}")
    public ResponseEntity<AccountDto> getAccountById(
            @PathVariable Long id) {

        return ResponseEntity.ok(bankService.getAccountById(id));
    }

    /**
     * Créer un compte bancaire.
     */
    @PostMapping("/accounts")
    public ResponseEntity<AccountDto> createAccount(
            @Valid @RequestBody CreateAccountRequest request) {

        AccountDto account = bankService.createAccount(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(account);
    }

    /**
     * Effectuer un calcul de crédit.
     */
    @PostMapping("/calculations/credit")
    public ResponseEntity<CreditCalculationResponse> calculateCredit(
            @Valid @RequestBody CreditCalculationRequest request) {

        return ResponseEntity.ok(bankService.calculateCredit(request));
    }

    /**
     * Exécuter un virement bancaire.
     */
    @PostMapping("/transfers")
    public ResponseEntity<TransferResponse> executeTransfer(
            @Valid @RequestBody TransferRequest request) {

        return ResponseEntity.ok(bankService.executeTransfer(request));
    }

    /**
     * Supprimer un compte bancaire.
     */
    @DeleteMapping("/accounts/{id}")
    public ResponseEntity<Void> deleteAccount(
            @PathVariable Long id) {

        bankService.deleteAccount(id);

        return ResponseEntity.noContent().build();
    }
}