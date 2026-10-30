# Event Sourcing

## 1. Définition

**Event Sourcing** est un pattern architectural qui consiste à stocker l'ensemble des événements métier ayant modifié l'état d'une entité, plutôt que de sauvegarder uniquement son état actuel.

Chaque modification est enregistrée sous forme d'un événement immuable dans un stockage appelé **Event Store**.

L'état actuel de l'entité est ensuite reconstruit en rejouant chronologiquement les événements associés.

Contrairement à une architecture CRUD classique, où l'on met à jour directement les données, Event Sourcing conserve l'historique complet des changements.

**Exemple :**

Dans une application bancaire, au lieu de stocker uniquement le solde actuel d'un compte, on conserve les opérations qui ont conduit à ce solde.

| Événement | Montant | Solde résultant |
|---|---:|---:|
| AccountOpened | 1 000 € | 1 000 € |
| MoneyDeposited | 500 € | 1 500 € |
| MoneyWithdrawn | 200 € | 1 300 € |
| MoneyDeposited | 300 € | 1 600 € |

Le solde actuel est calculé en rejouant les événements.

## 2. Principes fondamentaux

### 2.1. Event Store

L'Event Store est le stockage central des événements métier.

Ses caractéristiques :
- Stockage append-only : les événements sont ajoutés, jamais modifiés.
- Conservation de l'historique complet.
- Ordre chronologique des événements par agrégat.
- Possibilité de rejouer les événements pour reconstruire un état.

### 2.2. Domain Events

Un Domain Event représente un fait métier qui s'est déjà produit.

Exemples :
- `AccountOpened`
- `MoneyDeposited`
- `MoneyWithdrawn`
- `OrderCreated`
- `OrderCancelled`
- `PaymentValidated`

Un événement doit exprimer une action métier passée et significative.

Il contient généralement :
- Un identifiant unique.
- Le type d'événement.
- L'identifiant de l'agrégat concerné.
- Les données métier nécessaires.
- La date de création.
- La version de l'événement.
- La version du flux d'événements.

### 2.3. Aggregate

L'Aggregate est l'objet métier chargé de garantir la cohérence des règles métier.

Il permet de :
- Recevoir les commandes.
- Vérifier les règles métier.
- Produire de nouveaux événements.
- Reconstruire son état à partir des événements existants.

### 2.4. Event Replay

Le Replay consiste à rejouer les événements enregistrés afin de reconstruire l'état d'une entité.

Par exemple :

```text
AccountOpened(1000)
       |
       v
MoneyDeposited(500)
       |
       v
MoneyWithdrawn(200)
       |
       v
Current Balance = 1300
```

Le même historique permet également de reconstruire l'état du compte à une date antérieure.

### 2.5. Snapshot

Lorsque le nombre d'événements devient important, rejouer l'intégralité de l'historique peut devenir coûteux.

Un Snapshot consiste à sauvegarder périodiquement l'état calculé d'un agrégat.

La reconstruction devient alors :

```text
Snapshot (version 100)
       +
Events (101 -> 120)
       |
       v
Current State (version 120)
```

Le Snapshot est une optimisation : l'Event Store reste la source de vérité.

## 3. Fonctionnement architectural

Le cycle de traitement d'une commande suit généralement ces étapes :

1. Le client envoie une commande métier.
2. Le Command Handler récupère les événements de l'agrégat.
3. L'agrégat est reconstruit à partir de son historique.
4. Les règles métier sont vérifiées.
5. L'agrégat produit un ou plusieurs nouveaux événements.
6. Les événements sont enregistrés dans l'Event Store.
7. Les événements sont éventuellement publiés vers des consommateurs.
8. Les projections et les systèmes externes sont mis à jour.

```text
             Client
                |
                v
             Command
                |
                v
          Command Handler
                |
                v
             Aggregate
                |
        Load Existing Events
                |
                v
            Event Store
                |
        Validate Business Rules
                |
                v
          Generate Events
                |
                v
          Append Events
                |
                v
          Event Publication
                |
        +-------+--------+
        |                |
        v                v
   Read Models      Other Services
   / Projections    / Integrations
```

## 4. Event Sourcing vs CRUD

| Critère | CRUD classique | Event Sourcing |
|---|---|---|
| Stockage | État actuel | Historique des événements |
| Modification | UPDATE / DELETE | Append-only |
| Historique | Nécessite un mécanisme complémentaire | Natif |
| Reconstruction | À partir des données actuelles | Replay des événements |
| Audit | Journalisation complémentaire | Historique métier |
| Complexité | Faible à modérée | Élevée |

## 5. Avantages et inconvénients

### Avantages

- Historique métier complet et traçabilité.
- Reconstruction de l'état à n'importe quel moment.
- Auditabilité des opérations.
- Possibilité de créer de nouvelles projections à partir de l'historique.
- Meilleure séparation entre les événements métier et leurs consommateurs.
- Possibilité d'analyser les comportements métier dans le temps.

### Inconvénients

- Complexité de conception et de maintenance.
- Gestion de l'évolution du format des événements (Event Versioning).
- Gestion des événements dupliqués et de l'idempotence.
- Reconstruction potentiellement coûteuse sans snapshots.
- Cohérence éventuelle entre l'Event Store et les projections.
- Contraintes particulières concernant les données personnelles et leur suppression réglementaire.

## 6. Implémentation avec Java / Spring Boot

L'exemple suivant illustre une implémentation simplifiée d'Event Sourcing pour un compte bancaire.

### 6.1. Définition des événements

```java
public interface DomainEvent {
    LocalDateTime occurredAt();
}

public record AccountOpened(
        BigDecimal initialBalance,
        LocalDateTime occurredAt
) implements DomainEvent {}

public record MoneyDeposited(
        BigDecimal amount,
        LocalDateTime occurredAt
) implements DomainEvent {}

public record MoneyWithdrawn(
        BigDecimal amount,
        LocalDateTime occurredAt
) implements DomainEvent {}
```

### 6.2. Aggregate

L'agrégat reconstruit son état à partir des événements et applique les règles métier avant de produire de nouveaux événements.

```java
public class AccountAggregate {

    private BigDecimal balance = BigDecimal.ZERO;

    public void apply(DomainEvent event) {

        switch (event) {
            case AccountOpened e -> balance = e.initialBalance();
            case MoneyDeposited e -> balance = balance.add(e.amount());
            case MoneyWithdrawn e -> balance = balance.subtract(e.amount());
        }
    }

    public MoneyWithdrawn withdraw(BigDecimal amount) {

        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Invalid amount");
        }

        if (balance.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient balance");
        }

        return new MoneyWithdrawn(amount, LocalDateTime.now());
    }

    public BigDecimal getBalance() {
        return balance;
    }
}
```

### 6.3. Event Store

L'Event Store conserve les événements et permet de les récupérer dans leur ordre de création.

```java
public interface EventStore {

    void append(
        String aggregateId,
        List<DomainEvent> events,
        long expectedVersion
    );

    List<DomainEvent> getEvents(String aggregateId);
}
```

L'implémentation réelle doit garantir :
- L'atomicité de l'enregistrement.
- Le contrôle de concurrence optimiste avec `expectedVersion`.
- L'ordre des événements.
- L'unicité des identifiants d'événements.

### 6.4. Reconstruction de l'agrégat

```java
public AccountAggregate loadAccount(String accountId) {

    List<DomainEvent> events =
            eventStore.getEvents(accountId);

    AccountAggregate account = new AccountAggregate();

    events.forEach(account::apply);

    return account;
}
```

### 6.5. Traitement d'une commande

```java
@Service
@RequiredArgsConstructor
public class AccountCommandService {

    private final EventStore eventStore;

    public void withdraw(String accountId, BigDecimal amount) {

        List<DomainEvent> history =
                eventStore.getEvents(accountId);

        AccountAggregate account = new AccountAggregate();

        history.forEach(account::apply);

        MoneyWithdrawn event = account.withdraw(amount);

        long expectedVersion = history.size();

        eventStore.append(
                accountId,
                List.of(event),
                expectedVersion
        );
    }
}
```

**Remarque :** cet exemple est pédagogique. Une implémentation en production doit notamment gérer les identifiants d'événements, la concurrence, les transactions, la sérialisation et le versionnement des événements. Le `expectedVersion` doit correspondre à une véritable version du flux, contrôlée atomiquement par l'Event Store.

## 7. Technologies et solutions

| Technologie | Utilisation |
|---|---|
| Axon Framework | Framework Java pour Event Sourcing et CQRS |
| EventStoreDB | Base spécialisée dans le stockage d'événements |
| PostgreSQL | Implémentation possible d'un Event Store personnalisé |
| Apache Kafka | Distribution et consommation des événements |
| Spring Boot | Développement des services et handlers |

**Attention :** Kafka est principalement une plateforme de streaming et de distribution d'événements. Il ne remplace pas automatiquement un Event Store garantissant les fonctionnalités attendues pour les flux d'agrégats, leur concurrence et leur reconstruction.

## 8. Bonnes pratiques

- Concevoir des événements métier orientés intention plutôt que de simples modifications techniques.
- Ne jamais modifier un événement déjà enregistré.
- Mettre en place une stratégie de versionnement des événements.
- Garantir l'idempotence des consommateurs.
- Utiliser le contrôle de concurrence optimiste pour éviter les écritures concurrentes incohérentes.
- Mettre en place des snapshots lorsque les flux deviennent volumineux.
- Prévoir une stratégie de gestion des données personnelles (RGPD).
- Séparer les événements métier internes des événements d'intégration lorsque nécessaire.
- Tester le replay, les règles métier et les projections.

## 9. Cas d'utilisation

Event Sourcing est particulièrement adapté aux domaines nécessitant une traçabilité métier forte :

- **Banking :** historique des mouvements et opérations sur les comptes.
- **Payment :** cycle de vie des paiements et remboursements.
- **Order Management :** historique des commandes et changements de statut.
- **Insurance :** historique des contrats et des modifications de garanties.
- **Booking :** réservations, annulations et disponibilités.
- **Financial Systems :** historique des décisions et des opérations financières.

Il est généralement déconseillé pour les applications CRUD simples qui n'ont pas besoin de reconstruire leur historique métier.

## 10. Relation avec CQRS

Event Sourcing et CQRS sont deux patterns distincts, mais ils sont fréquemment utilisés ensemble.

- **Event Sourcing :** définit comment les changements métier sont stockés.
- **CQRS :** définit comment les opérations de lecture et d'écriture sont séparées.

Dans une architecture combinant les deux :

- Le modèle d'écriture traite les commandes et enregistre les événements.
- Le modèle de lecture consomme ces événements et construit des projections optimisées pour les requêtes.

Il est important de noter que CQRS ne nécessite pas obligatoirement Event Sourcing, et inversement.