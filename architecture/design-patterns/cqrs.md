# CQRS (Command Query Responsibility Segregation)

## 1. Définition

**CQRS (Command Query Responsibility Segregation)** est un pattern architectural qui consiste à séparer les opérations de modification des données (*Commands*) des opérations de consultation (*Queries*).

Contrairement à une architecture CRUD classique, où les mêmes modèles et services peuvent être utilisés pour lire et modifier les données, CQRS introduit deux responsabilités distinctes :

- **Command** : représente une opération qui modifie l'état du système (Create, Update, Delete).
- **Query** : représente une opération de consultation qui récupère des données sans modifier l'état du système (Read).

L'objectif est de découpler les modèles de lecture et d'écriture afin de faciliter leur évolution et leur optimisation indépendantes.

## 2. Fonctionnement

CQRS repose sur deux circuits distincts :

### 2.1. Command Side (Write Model)

Responsable de toutes les opérations de modification des données.

1. Le client envoie une commande.
2. Le Command Handler reçoit et traite la commande.
3. Les règles métier sont exécutées et validées.
4. Les modifications sont persistées dans la base de données.

Exemples :
- `CreateSimulationCommand`
- `UpdateSimulationCommand`
- `DeleteSimulationCommand`

### 2.2. Query Side (Read Model)

Responsable exclusivement de la consultation des données.

1. Le client effectue une requête.
2. Le Query Handler reçoit la requête.
3. Les données sont récupérées depuis le modèle de lecture.
4. Un DTO adapté au besoin est retourné au client.

Exemples :
- `GetSimulationQuery`
- `GetSimulationDetailsQuery`
- `GetSavedSimulationsQuery`

### 2.3. Architecture

```text
                   Client / API
                        |
              +---------+---------+
              |                   |
          COMMAND               QUERY
              |                   |
       Command Handler       Query Handler
              |                   |
        Domain Model          Read Model
              |                   |
              v                   v
        Write Database       Read Database
```

Les modèles de lecture et d'écriture peuvent partager la même base de données. Il n'est pas obligatoire d'avoir deux bases distinctes pour implémenter CQRS.

## 3. Implémentation avec Spring Boot

Prenons l'exemple d'un moteur financier permettant de créer et de consulter des simulations de financement.

### 3.1. Organisation des packages

```text
simulation/
├── command/
│   ├── CreateSimulationCommand.java
│   └── SimulationCommandHandler.java
│
├── query/
│   ├── GetSimulationQuery.java
│   └── SimulationQueryHandler.java
│
├── domain/
│   ├── Simulation.java
│   └── SimulationRepository.java
│
├── infrastructure/
│   └── persistence/
│       └── SimulationJpaRepository.java
│
├── dto/
│   └── SimulationDTO.java
│
└── controller/
    └── SimulationController.java
```

### 3.2. Command Side : écriture

**Définition de la commande**

```java
public record CreateSimulationCommand(
    String email,
    BigDecimal montant,
    Integer duree
) {}
```

**Command Handler**

```java
@Service
@RequiredArgsConstructor
public class SimulationCommandHandler {

    private final SimulationRepository repository;

    @Transactional
    public Long handle(CreateSimulationCommand command) {

        Simulation simulation = new Simulation();

        simulation.setEmail(command.email());
        simulation.setMontant(command.montant());
        simulation.setDuree(command.duree());
        simulation.setStatut(Statut.INITIALISEE);

        return repository.save(simulation).getId();
    }
}
```

Le Command Handler centralise le traitement de la commande et garantit l'application des règles métier avant la persistance.

### 3.3. Query Side : lecture

**Définition de la requête**

```java
public record GetSimulationQuery(Long simulationId) {}
```

**Query Handler**

```java
@Service
@RequiredArgsConstructor
public class SimulationQueryHandler {

    private final SimulationJpaRepository repository;

    @Transactional(readOnly = true)
    public SimulationDTO handle(GetSimulationQuery query) {

        return repository.findById(query.simulationId())
            .map(SimulationDTO::from)
            .orElseThrow(() ->
                new EntityNotFoundException("Simulation introuvable")
            );
    }
}
```

Le Query Handler récupère les données et retourne une représentation adaptée au besoin de consultation, sans modifier l'état du domaine.

### 3.4. Controller

```java
@RestController
@RequestMapping("/simulations")
@RequiredArgsConstructor
public class SimulationController {

    private final SimulationCommandHandler commandHandler;
    private final SimulationQueryHandler queryHandler;

    @PostMapping
    public Long create(@RequestBody CreateSimulationCommand command) {
        return commandHandler.handle(command);
    }

    @GetMapping("/{id}")
    public SimulationDTO getById(@PathVariable Long id) {
        return queryHandler.handle(new GetSimulationQuery(id));
    }
}
```

Le Controller délègue les opérations aux handlers correspondants, sans porter la logique métier.

## 4. CQRS avec Event-Driven Architecture

CQRS peut être combiné avec une architecture événementielle pour synchroniser les modèles de lecture et d'écriture.

```text
             Client
                |
                v
          Command Handler
                |
                v
          Domain Model
                |
                v
          Write Database
                |
                v
        Domain Event
                |
                v
        Message Broker
         Kafka / RabbitMQ
                |
                v
         Event Consumer
                |
                v
         Read Projection
                |
                v
          Read Database
                |
                v
          Query Handler
                |
                v
             Client
```

Exemple de fonctionnement :

1. Une simulation est créée.
2. Le modèle d'écriture persiste la simulation.
3. Un événement `SimulationCreatedEvent` est publié.
4. Un consommateur récupère cet événement.
5. Le consommateur met à jour le modèle de lecture.
6. Les prochaines consultations utilisent le modèle de lecture actualisé.

Cette approche permet d'avoir des modèles de données indépendants, adaptés respectivement aux opérations d'écriture et de lecture.

**Attention :** lorsque la synchronisation est asynchrone, les deux modèles ne sont pas nécessairement synchronisés immédiatement. Ce comportement est appelé *Eventual Consistency* (cohérence éventuelle).

## 5. CQRS et Event Sourcing

CQRS et Event Sourcing sont deux patterns complémentaires, mais indépendants.

| Critère | CQRS | Event Sourcing |
|---|---|---|
| Objectif | Séparer lecture et écriture | Stocker l'historique des événements métier |
| Stockage | État courant des données possible | Suite d'événements |
| Modification | UPDATE / INSERT / DELETE possibles | Ajout de nouveaux événements |
| Historique | Facultatif | Natif |
| Reconstruction | Depuis les données persistées | Depuis les événements |
| Complexité | Moyenne | Élevée |

À retenir :

- CQRS n'impose pas Event Sourcing.
- Event Sourcing peut être utilisé sans CQRS.
- Les deux patterns peuvent être combinés pour construire une architecture événementielle.

## 6. Avantages et inconvénients

### Avantages

- Séparation claire des responsabilités de lecture et d'écriture.
- Modèles de lecture adaptés aux besoins métier.
- Possibilité de faire évoluer indépendamment les deux modèles.
- Optimisation des performances des requêtes de consultation.
- Scalabilité indépendante des opérations de lecture et d'écriture.
- Facilite l'intégration avec une architecture événementielle.

### Inconvénients

- Complexité architecturale supplémentaire.
- Duplication potentielle des données.
- Gestion de la cohérence entre les modèles.
- Introduction possible d'une latence de synchronisation.
- Tests, monitoring et maintenance plus complexes.

## 7. Bonnes pratiques

1. Ne pas appliquer CQRS systématiquement à tous les microservices.
2. Commencer par une séparation logique entre Command et Query au sein d'une même application.
3. Utiliser une base de données commune tant que les besoins ne justifient pas une séparation physique.
4. Introduire Kafka ou RabbitMQ uniquement lorsque la communication asynchrone apporte une réelle valeur.
5. Garantir l'idempotence des consommateurs d'événements.
6. Utiliser le pattern Transactional Outbox pour fiabiliser la publication des événements.
7. Surveiller le délai de synchronisation entre les modèles de lecture et d'écriture.
8. Ne pas exécuter de logique métier modifiant l'état du domaine dans un Query Handler.

## 8. Quand utiliser CQRS ?

| Contexte | Pertinence |
|---|---|
| Application CRUD classique | Faible |
| Application avec beaucoup de lectures | Élevée |
| Système avec des règles métier complexes | Élevée selon les besoins |
| Architecture microservices événementielle | Intéressante |
| Besoin de plusieurs modèles de consultation | Très élevée |
| Microservice simple avec peu de traitements | Généralement inutile |

## 9. Références

- [Martin Fowler - CQRS](https://martinfowler.com/bliki/CQRS.html)
- [Microsoft - CQRS Pattern](https://learn.microsoft.com/en-us/azure/architecture/patterns/cqrs)
- [Spring Framework Documentation](https://docs.spring.io/spring-framework/reference/)

---

**À retenir :** CQRS est un pattern architectural qui sépare les responsabilités de lecture et d'écriture. Il permet d'adapter chaque modèle à ses besoins spécifiques sans imposer nécessairement une séparation physique des bases de données ou l'utilisation d'une architecture événementielle.
