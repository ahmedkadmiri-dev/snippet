package com.example.testing_poc.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BankServiceImpl implements BankService {

    private final Map<Long, AccountDto> accounts = new ConcurrentHashMap<>();
    private final AtomicLong accountSequence = new AtomicLong(1);

    @Override
    public List<ReferentielDto> getReferentiel(String type) {

        if ("CURRENCY".equalsIgnoreCase(type)) {
            return List.of(
                    ReferentielDto.builder()
                            .code("MAD")
                            .libelle("Dirham marocain")
                            .description("Devise marocaine")
                            .build(),
                    ReferentielDto.builder()
                            .code("EUR")
                            .libelle("Euro")
                            .description("Devise européenne")
                            .build(),
                    ReferentielDto.builder()
                            .code("USD")
                            .libelle("Dollar américain")
                            .description("Devise américaine")
                            .build()
            );
        }

        if ("ACCOUNT_STATUS".equalsIgnoreCase(type)) {
            return List.of(
                    ReferentielDto.builder()
                            .code("ACTIVE")
                            .libelle("Actif")
                            .description("Compte actif")
                            .build(),
                    ReferentielDto.builder()
                            .code("BLOCKED")
                            .libelle("Bloqué")
                            .description("Compte bloqué")
                            .build()
            );
        }

        throw new IllegalArgumentException("Type de référentiel inconnu : " + type);
    }

    @Override
    public AccountDto getAccountById(Long id) {

        AccountDto account = accounts.get(id);

        if (account == null) {
            throw new NoSuchElementException("Compte bancaire introuvable : " + id);
        }

        return account;
    }

    @Override
    public AccountDto createAccount(CreateAccountRequest request) {

        Long id = accountSequence.getAndIncrement();

        AccountDto account = AccountDto.builder()
                .id(id)
                .accountNumber("BANK-%08d".formatted(id))
                .ownerName(request.getOwnerName())
                .balance(request.getInitialBalance())
                .currency(request.getCurrency())
                .status("ACTIVE")
                .build();

        accounts.put(id, account);

        return account;
    }

    @Override
    public CreditCalculationResponse calculateCredit(CreditCalculationRequest request) {

        BigDecimal amount = request.getAmount();
        int duration = request.getDurationInMonths();

        BigDecimal monthlyRate = request.getAnnualInterestRate()
                .divide(BigDecimal.valueOf(1200), 10, RoundingMode.HALF_UP);

        BigDecimal monthlyPayment;

        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            monthlyPayment = amount.divide(
                    BigDecimal.valueOf(duration), 2, RoundingMode.HALF_UP);
        } else {
            double rate = monthlyRate.doubleValue();

            double factor = Math.pow(1 + rate, duration);

            monthlyPayment = amount.multiply(monthlyRate)
                    .multiply(BigDecimal.valueOf(factor))
                    .divide(
                            BigDecimal.valueOf(factor - 1),
                            2,
                            RoundingMode.HALF_UP
                    );
        }

        BigDecimal totalAmount = monthlyPayment.multiply(BigDecimal.valueOf(duration));

        BigDecimal totalInterest = totalAmount.subtract(amount);

        return CreditCalculationResponse.builder()
                .loanAmount(amount)
                .monthlyPayment(monthlyPayment)
                .totalInterest(totalInterest)
                .totalAmount(totalAmount)
                .durationInMonths(duration)
                .build();
    }

    @Override
    public TransferResponse executeTransfer(TransferRequest request) {

        if (request.getSourceAccountId().equals(request.getDestinationAccountId())) {
            throw new IllegalArgumentException(
                    "Le compte source et le compte destinataire doivent être différents");
        }

        AccountDto source = getAccountById(request.getSourceAccountId());
        AccountDto destination = getAccountById(request.getDestinationAccountId());

        if (!"ACTIVE".equals(source.getStatus())
                || !"ACTIVE".equals(destination.getStatus())) {
            throw new IllegalStateException("Les comptes doivent être actifs");
        }

        if (!source.getCurrency().equals(destination.getCurrency())) {
            throw new IllegalArgumentException("Les devises des comptes doivent être identiques");
        }

        if (source.getBalance().compareTo(request.getAmount()) < 0) {
            throw new IllegalStateException("Solde insuffisant");
        }

        AccountDto updatedSource = AccountDto.builder()
                .id(source.getId())
                .accountNumber(source.getAccountNumber())
                .ownerName(source.getOwnerName())
                .balance(source.getBalance().subtract(request.getAmount()))
                .currency(source.getCurrency())
                .status(source.getStatus())
                .build();

        AccountDto updatedDestination = AccountDto.builder()
                .id(destination.getId())
                .accountNumber(destination.getAccountNumber())
                .ownerName(destination.getOwnerName())
                .balance(destination.getBalance().add(request.getAmount()))
                .currency(destination.getCurrency())
                .status(destination.getStatus())
                .build();

        accounts.put(source.getId(), updatedSource);
        accounts.put(destination.getId(), updatedDestination);

        return TransferResponse.builder()
                .transactionReference(UUID.randomUUID().toString())
                .sourceAccountId(source.getId())
                .destinationAccountId(destination.getId())
                .amount(request.getAmount())
                .currency(source.getCurrency())
                .status("EXECUTED")
                .executionDate(LocalDateTime.now())
                .build();
    }

    @Override
    public void deleteAccount(Long id) {

        AccountDto removed = accounts.remove(id);

        if (removed == null) {
            throw new NoSuchElementException("Compte bancaire introuvable : " + id);
        }
    }
}
