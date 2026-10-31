package com.example.testing_poc.service;

import com.example.testing_poc.dto.*;
import java.util.List;

public interface BankService {

    List<ReferentielDto> getReferentiel(String type);

    AccountDto getAccountById(Long id);

    AccountDto createAccount(CreateAccountRequest request);

    CreditCalculationResponse calculateCredit(CreditCalculationRequest request);

    TransferResponse executeTransfer(TransferRequest request);

    void deleteAccount(Long id);
}
