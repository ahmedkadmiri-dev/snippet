package com.example.testing_poc.services.impl;

import com.example.testing_poc.dtos.*;
import com.example.testing_poc.entities.Account;
import com.example.testing_poc.entities.BankTransaction;
import com.example.testing_poc.entities.Referentiel;
import com.example.testing_poc.repositories.AccountRepository;
import com.example.testing_poc.repositories.BankTransactionRepository;
import com.example.testing_poc.repositories.ReferentielRepository;
import com.example.testing_poc.services.BankService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BankServiceImpl implements BankService {

    private static final int MONEY_SCALE = 2;
    private static final int CALCULATION_SCALE = 10;

    private static final String CURRENCY_TYPE = "CURRENCY";
    private static final String ACCOUNT_STATUS_TYPE = "ACCOUNT_STATUS";

    private static final String ACTIVE_STATUS = "ACTIVE";
    private static final String BLOCKED_STATUS = "BLOCKED";
    private static final String EXECUTED_STATUS = "EXECUTED";

    private final AccountRepository accountRepository;
    private final BankTransactionRepository bankTransactionRepository;
    private final ReferentielRepository referentielRepository;

    @Override
    public List<ReferentielDto> getReferentiels(String type) {
        return referentielRepository.findByTypeIgnoreCase(type)
                .stream()
                .map(this::toReferentielDto)
                .toList();
    }

    @Override
    public AccountDto getAccount(Long id) {
        Account account = findAccount(id);
        return toAccountDto(account);
    }

    @Override
    @Transactional
    public AccountDto createAccount(CreateAccountRequest request) {
        String currency = request.getCurrency().toUpperCase();

        validateReferentiel(CURRENCY_TYPE, currency);

        validateAmount(request.getInitialBalance());

        Account account = Account.builder()
                .accountNumber(generateAccountNumber())
                .ownerName(request.getOwnerName())
                .balance(request.getInitialBalance().setScale(MONEY_SCALE, RoundingMode.HALF_UP))
                .currency(currency)
                .status(ACTIVE_STATUS)
                .build();

        return toAccountDto(accountRepository.save(account));
    }

    @Override
    public CreditCalculationResponse calculateCredit(CreditCalculationRequest request) {
        BigDecimal amount = request.getAmount();
        int duration = request.getDurationInMonths();

        BigDecimal monthlyRate = request.getAnnualInterestRate()
                .divide(BigDecimal.valueOf(1200), CALCULATION_SCALE, RoundingMode.HALF_UP);

        BigDecimal monthlyPayment;

        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            monthlyPayment = amount.divide(
                    BigDecimal.valueOf(duration),
                    MONEY_SCALE,
                    RoundingMode.HALF_UP
            );
        } else {
            BigDecimal factor = BigDecimal.ONE.add(monthlyRate).pow(duration);

            monthlyPayment = amount
                    .multiply(monthlyRate)
                    .multiply(factor)
                    .divide(factor.subtract(BigDecimal.ONE), MONEY_SCALE, RoundingMode.HALF_UP);
        }

        BigDecimal totalAmount = monthlyPayment.multiply(BigDecimal.valueOf(duration))
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        BigDecimal totalInterest = totalAmount.subtract(amount)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        return CreditCalculationResponse.builder()
                .loanAmount(amount.setScale(MONEY_SCALE, RoundingMode.HALF_UP))
                .monthlyPayment(monthlyPayment)
                .totalInterest(totalInterest)
                .totalAmount(totalAmount)
                .durationInMonths(duration)
                .build();
    }

    @Override
    @Transactional
    public TransferResponse transfer(TransferRequest request) {
        Long sourceId = request.getSourceAccountId();
        Long destinationId = request.getDestinationAccountId();

        if (sourceId.equals(destinationId)) {
            throw new IllegalArgumentException(
                    "Le compte source et le compte destinataire doivent être différents."
            );
        }

        validateAmount(request.getAmount());

        // Verrouillage dans l'ordre des identifiants pour limiter les deadlocks.
        Account firstAccount;
        Account secondAccount;

        if (sourceId < destinationId) {
            firstAccount = findAccountForUpdate(sourceId);
            secondAccount = findAccountForUpdate(destinationId);
        } else {
            secondAccount = findAccountForUpdate(destinationId);
            firstAccount = findAccountForUpdate(sourceId);
        }

        Account sourceAccount = firstAccount.getId().equals(sourceId)
                ? firstAccount
                : secondAccount;

        Account destinationAccount = firstAccount.getId().equals(destinationId)
                ? firstAccount
                : secondAccount;

        validateAccountStatus(sourceAccount);
        validateAccountStatus(destinationAccount);

        if (!sourceAccount.getCurrency().equalsIgnoreCase(destinationAccount.getCurrency())) {
            throw new IllegalArgumentException(
                    "Les comptes doivent utiliser la même devise pour effectuer un virement."
            );
        }

        if (sourceAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new IllegalArgumentException("Solde insuffisant.");
        }

        sourceAccount.setBalance(
                sourceAccount.getBalance().subtract(request.getAmount())
        );

        destinationAccount.setBalance(
                destinationAccount.getBalance().add(request.getAmount())
        );

        String transactionReference = generateTransactionReference();
        LocalDateTime executionDate = LocalDateTime.now();

        BankTransaction transaction = BankTransaction.builder()
                .transactionReference(transactionReference)
                .sourceAccount(sourceAccount)
                .destinationAccount(destinationAccount)
                .amount(request.getAmount().setScale(MONEY_SCALE, RoundingMode.HALF_UP))
                .currency(sourceAccount.getCurrency())
                .status(EXECUTED_STATUS)
                .description(request.getDescription())
                .executionDate(executionDate)
                .build();

        bankTransactionRepository.save(transaction);

        return TransferResponse.builder()
                .transactionReference(transactionReference)
                .sourceAccountId(sourceAccount.getId())
                .destinationAccountId(destinationAccount.getId())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .status(transaction.getStatus())
                .executionDate(executionDate)
                .build();
    }

    @Override
    @Transactional
    public void deleteAccount(Long id) {
        Account account = findAccount(id);

        boolean hasTransactions =
                bankTransactionRepository.existsBySourceAccount_IdOrDestinationAccount_Id(id, id);

        if (hasTransactions) {
            throw new IllegalArgumentException(
                    "Impossible de supprimer un compte ayant un historique de transactions."
            );
        }

        accountRepository.delete(account);
    }

    // =====================================================
    // PRIVATE METHODS
    // =====================================================

    private Account findAccount(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Compte bancaire introuvable : " + id
                ));
    }

    private Account findAccountForUpdate(Long id) {
        return accountRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Compte bancaire introuvable : " + id
                ));
    }

    private void validateReferentiel(String type, String code) {
        boolean exists = referentielRepository
                .existsByTypeIgnoreCaseAndCodeIgnoreCase(type, code);

        if (!exists) {
            throw new IllegalArgumentException(
                    "Référentiel inexistant : type=" + type + ", code=" + code
            );
        }
    }

    private void validateAccountStatus(Account account) {
        if (!ACTIVE_STATUS.equalsIgnoreCase(account.getStatus())) {
            throw new IllegalArgumentException(
                    "Le compte " + account.getAccountNumber() + " n'est pas actif."
            );
        }
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant doit être strictement positif.");
        }
    }

    private String generateAccountNumber() {
        String accountNumber;

        do {
            accountNumber = "BANK-" + UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 8)
                    .toUpperCase();
        } while (accountRepository.existsByAccountNumber(accountNumber));

        return accountNumber;
    }

    private String generateTransactionReference() {
        return "TRX-" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12)
                .toUpperCase();
    }

    private AccountDto toAccountDto(Account account) {
        return AccountDto.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .ownerName(account.getOwnerName())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .status(account.getStatus())
                .build();
    }

    private ReferentielDto toReferentielDto(Referentiel referentiel) {
        return ReferentielDto.builder()
                .code(referentiel.getCode())
                .libelle(referentiel.getLibelle())
                .description(referentiel.getDescription())
                .build();
    }
}