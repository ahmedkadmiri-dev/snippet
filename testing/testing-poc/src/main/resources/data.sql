-- =====================================================
-- BANK ACCOUNTS
-- =====================================================

INSERT INTO bank_account (
    account_number,
    owner_name,
    balance,
    currency,
    status
) VALUES
      ('BANK-00000001', 'Ahmed Kadmiri', 15000.00, 'MAD', 'ACTIVE'),
      ('BANK-00000002', 'Mohamed Alami', 25000.00, 'MAD', 'ACTIVE'),
      ('BANK-00000003', 'Fatima Zahra', 5000.00, 'MAD', 'BLOCKED'),
      ('BANK-00000004', 'Youssef Amrani', 50000.00, 'MAD', 'ACTIVE'),
      ('BANK-00000005', 'Sara Bennani', 12000.00, 'MAD', 'ACTIVE');


-- =====================================================
-- BANK TRANSACTIONS
-- =====================================================

INSERT INTO bank_transaction (
    transaction_reference,
    source_account_id,
    destination_account_id,
    amount,
    currency,
    status,
    description,
    execution_date
) VALUES
      (
          'TRX-000001',
          1,
          2,
          1000.00,
          'MAD',
          'EXECUTED',
          'Virement de test',
          CURRENT_TIMESTAMP
      ),
      (
          'TRX-000002',
          2,
          4,
          2500.00,
          'MAD',
          'EXECUTED',
          'Virement bancaire',
          CURRENT_TIMESTAMP
      ),
      (
          'TRX-000003',
          4,
          5,
          500.00,
          'MAD',
          'EXECUTED',
          'Paiement de test',
          CURRENT_TIMESTAMP
      );


-- =====================================================
-- BANK REFERENTIALS
-- =====================================================

-- Currencies

INSERT INTO bank_referentiel (
    type,
    code,
    libelle,
    description
) VALUES
      ('CURRENCY', 'MAD', 'Dirham marocain', 'Devise marocaine'),
      ('CURRENCY', 'EUR', 'Euro', 'Devise européenne'),
      ('CURRENCY', 'USD', 'Dollar américain', 'Devise américaine');


-- Account statuses

INSERT INTO bank_referentiel (
    type,
    code,
    libelle,
    description
) VALUES
      ('ACCOUNT_STATUS', 'ACTIVE', 'Actif', 'Compte actif'),
      ('ACCOUNT_STATUS', 'BLOCKED', 'Bloqué', 'Compte bloqué');