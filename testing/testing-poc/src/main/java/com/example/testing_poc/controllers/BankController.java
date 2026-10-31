package com.example.testing_poc.controllers;

import com.example.testing_poc.dtos.*;
import com.example.testing_poc.services.BankService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller pour la gestion des opérations bancaires.
 *
 * Endpoints disponibles :
 * - Consultation des référentiels
 * - Gestion des comptes bancaires
 * - Calcul de crédit
 * - Exécution des virements
 */
@RestController
@RequestMapping("/bank")
@RequiredArgsConstructor
public class BankController {

    private final BankService bankService;

    /**
     * Récupérer les référentiels bancaires par type.
     *
     * @param type type du référentiel (CURRENCY, ACCOUNT_STATUS...)
     * @return liste des référentiels correspondants
     */
    @GetMapping("/referentiels/{type}")
    public ResponseEntity<List<ReferentielDto>> getReferentiels(
            @PathVariable String type) {

        return ResponseEntity.ok(bankService.getReferentiels(type));
    }

    /**
     * Récupérer un compte bancaire par son identifiant.
     *
     * @param id identifiant du compte
     * @return informations du compte bancaire
     */
    @GetMapping("/accounts/{id}")
    public ResponseEntity<AccountDto> getAccount(
            @PathVariable Long id) {

        return ResponseEntity.ok(bankService.getAccount(id));
    }

    /**
     * Créer un nouveau compte bancaire.
     *
     * @param request données de création du compte
     * @return compte bancaire créé
     */
    @PostMapping("/accounts")
    public ResponseEntity<AccountDto> createAccount(
            @Valid @RequestBody CreateAccountRequest request) {

        AccountDto account = bankService.createAccount(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(account);
    }

    /**
     * Effectuer un calcul de crédit.
     *
     * @param request paramètres du calcul de crédit
     * @return résultat du calcul (mensualité, intérêts, montant total)
     */
    @PostMapping("/calculations/credit")
    public ResponseEntity<CreditCalculationResponse> calculateCredit(
            @Valid @RequestBody CreditCalculationRequest request) {

        CreditCalculationResponse response = bankService.calculateCredit(request);

        return ResponseEntity.ok(response);
    }

    /**
     * Exécuter un virement bancaire entre deux comptes.
     *
     * @param request informations du virement
     * @return résultat de l'opération
     */
    @PostMapping("/transfers")
    public ResponseEntity<TransferResponse> executeTransfer(
            @Valid @RequestBody TransferRequest request) {

        TransferResponse response = bankService.transfer(request);

        return ResponseEntity.ok(response);
    }

    /**
     * Supprimer un compte bancaire.
     *
     * @param id identifiant du compte à supprimer
     * @return réponse HTTP 204 No Content
     */
    @DeleteMapping("/accounts/{id}")
    public ResponseEntity<Void> deleteAccount(
            @PathVariable Long id) {

        bankService.deleteAccount(id);

        return ResponseEntity.noContent().build();
    }
}