package com.example.testing_poc.repositories;

import com.example.testing_poc.entities.BankTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BankTransactionRepository extends JpaRepository<BankTransaction, Long> {

    Optional<BankTransaction> findByTransactionReference(String transactionReference);

    boolean existsBySourceAccount_IdOrDestinationAccount_Id(
            Long sourceAccountId,
            Long destinationAccountId
    );

    List<BankTransaction> findBySourceAccount_IdOrDestinationAccount_Id(
            Long sourceAccountId,
            Long destinationAccountId
    );
}