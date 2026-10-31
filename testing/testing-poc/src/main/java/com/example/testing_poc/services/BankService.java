package com.example.testing_poc.services;

import com.example.testing_poc.dtos.*;
import java.util.List;

public interface BankService {

    List<ReferentielDto> getReferentiels(String type);

    AccountDto getAccount(Long id);

    AccountDto createAccount(CreateAccountRequest request);

    CreditCalculationResponse calculateCredit(CreditCalculationRequest request);

    TransferResponse transfer(TransferRequest request);

    void deleteAccount(Long id);
}
