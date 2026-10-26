# Event Streaming — Communication événementielle avec Apache Kafka

## 1. Objectif

L'Event Streaming est un mécanisme de communication asynchrone permettant de publier, stocker et traiter des événements en continu entre plusieurs applications ou microservices.

Contrairement à un appel REST synchrone, le producteur d'un événement n'a pas besoin d'attendre que les consommateurs le traitent.

Les objectifs sont :

- Découpler les microservices.
- Diffuser un même événement à plusieurs applications.
- Traiter les événements de manière asynchrone.
- Conserver les événements pour permettre leur relecture.
- Construire des architectures réactives et évolutives.
- Alimenter des systèmes de reporting, d'audit et d'analyse de données.

## 2. Comprendre le concept d'Event Streaming

### 2.1. Qu'est-ce qu'un événement ?

Un événement représente un fait qui s'est produit dans un système.

Exemples :

- Une simulation financière a été créée.
- Un dossier de crédit a été validé.
- Un client a été créé.
- Un paiement a été effectué.
- Une demande de financement a changé de statut.

Un événement décrit généralement un fait passé. Par exemple, `SimulationCreated` signifie qu'une simulation a été créée.

### 2.2. Comment fonctionne l'Event Streaming ?

Le fonctionnement repose sur trois composants principaux :

- **Producer (producteur)** : publie les événements.
- **Event Broker (courtier d'événements)** : reçoit et conserve les événements dans des flux.
- **Consumer (consommateur)** : lit les événements et déclenche les traitements nécessaires.

Exemple d'architecture :

```text
             Microservice Simulation
                       |
                       | SimulationCreated
                       v
              +------------------+
              |  Event Streaming |
              |   Apache Kafka   |
              +------------------+
                 |      |      |
                 v      v      v
             Reporting  Audit  Notification
```

Lorsqu'une simulation est créée, le microservice Simulation publie un événement. Kafka le conserve dans un topic. Plusieurs consommateurs peuvent ensuite lire cet événement indépendamment.

Le microservice Simulation n'a pas besoin de connaître les consommateurs ni de les appeler directement.

### 2.3. Event Streaming et Message Broker : quelle différence ?

Les deux approches permettent une communication asynchrone, mais elles répondent à des besoins différents.

| Critère | Message Broker classique | Event Streaming |
|---|---|---|
| Objectif principal | Distribuer des messages et des tâches | Publier, conserver et traiter des flux d'événements |
| Exemple de technologie | RabbitMQ | Apache Kafka |
| Consommation | Souvent orientée distribution des messages | Lecture d'un flux selon la position du consommateur |
| Conservation | Dépend de la configuration et de la politique de files | Conservation configurable par durée ou taille |
| Relecture | Dépend des mécanismes disponibles | Possible tant que les événements sont conservés |
| Plusieurs consommateurs | Files, échanges et routage | Plusieurs consumer groups indépendants |
| Cas d'usage | Traitement asynchrone de tâches, commandes et routage | Audit, reporting, intégration de données et événements métier |

Cette distinction reste indicative : RabbitMQ peut aussi gérer des flux et Kafka peut être utilisé pour distribuer des tâches. Le choix dépend principalement du besoin fonctionnel et des contraintes d'architecture.

## 3. Apache Kafka : implémentation de l'Event Streaming

### 3.1. Qu'est-ce qu'Apache Kafka ?

Apache Kafka est une plateforme distribuée d'Event Streaming permettant de publier, stocker et consommer des événements à grande échelle.

Kafka peut servir de plateforme centrale d'échange événementiel entre les microservices d'une application.

Ses principales caractéristiques sont :

- Stockage durable des événements selon une politique de rétention.
- Traitement de volumes importants d'événements.
- Répartition des données sur plusieurs partitions.
- Consommation indépendante par plusieurs applications.
- Relecture des événements conservés.
- Réplication des données pour améliorer la disponibilité.

### 3.2. Les concepts fondamentaux de Kafka

#### Topic

Un topic est un flux nommé dans lequel les événements sont publiés.

Exemples :

```text
simulation.created
simulation.transformed
customer.created
payment.completed
```

#### Producer

Le producer publie les événements dans un topic.

Exemple : le microservice Simulation publie `SimulationCreated` après la création d'une simulation.

#### Consumer

Le consumer lit les événements d'un topic et exécute un traitement.

Exemple : le microservice Notification lit `simulation.created` pour envoyer un SMS au client.

#### Partition

Un topic peut être divisé en plusieurs partitions afin de répartir les données et le traitement.

L'ordre des événements est garanti au sein d'une même partition, mais pas globalement entre toutes les partitions.

#### Offset

Chaque événement possède une position dans sa partition, appelée offset.

Kafka permet au consommateur de suivre sa progression et de reprendre la lecture à partir d'un offset donné, tant que les événements nécessaires sont encore conservés.

#### Consumer Group

Un consumer group regroupe des consommateurs qui se partagent le traitement des partitions d'un topic.

Plusieurs consumer groups peuvent lire indépendamment le même topic.

Exemple :

```text
Topic: simulation.created
              |
      +-------+-------+
      |       |       |
      v       v       v
  Groupe    Groupe   Groupe
 Reporting  Audit  Notification
```

Chaque groupe peut traiter le même événement pour un objectif différent.

Dans un même groupe, une partition est attribuée à un seul consommateur à la fois.

## 4. Exemple métier : création d'une simulation financière

Dans une architecture de microservices, on peut avoir :

- `moteur-financier` : calcule et enregistre la simulation.
- `notification-service` : envoie un SMS ou un e-mail.
- `reporting-service` : alimente les indicateurs de pilotage.
- `audit-service` : conserve une trace des événements métier.

Sans Event Streaming, le moteur financier devrait appeler directement chacun des services, ce qui crée du couplage.

Avec Kafka :

1. Le moteur financier crée et enregistre la simulation.
2. Il publie l'événement `SimulationCreated`.
3. Kafka conserve cet événement dans le topic `simulation.created`.
4. Les services Notification, Reporting et Audit consomment l'événement indépendamment.
5. Si un consommateur est temporairement indisponible, il peut reprendre le traitement ultérieurement, dans les limites de la rétention et de sa configuration.

Le succès de la publication dans Kafka ne signifie pas nécessairement que tous les consommateurs ont terminé leur traitement.

## 5. Installer Kafka avec Docker Compose

Exemple local utilisant l'image officielle Apache Kafka en mode KRaft, sans ZooKeeper.

```yaml
services:
  kafka:
    image: apache/kafka:latest
    container_name: kafka
    ports:
      - "9092:9092"
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_LISTENERS: PLAINTEXT://:9092,CONTROLLER://:9093
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@localhost:9093
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS: 0
```

Démarrer Kafka :

```bash
docker compose up -d
```

Vérifier le conteneur :

```bash
docker compose ps
```

Créer un topic de test :

```bash
docker exec kafka /opt/kafka/bin/kafka-topics.sh \
  --create \
  --topic simulation.created \
  --bootstrap-server localhost:9092 \
  --partitions 3 \
  --replication-factor 1
```

Lister les topics :

```bash
docker exec kafka /opt/kafka/bin/kafka-topics.sh \
  --list \
  --bootstrap-server localhost:9092
```

**Important :** cette configuration est destinée au développement local. Pour la production, il faut notamment prévoir la sécurité, la réplication adaptée, la supervision, la persistance des données et une configuration réseau appropriée.

## 6. Intégration de Kafka avec Spring Boot

### 6.1. Ajouter la dépendance Maven

Dans le `pom.xml` :

```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

Avec Spring Boot, il est préférable de laisser le BOM de Spring Boot gérer la version compatible de Spring Kafka.

### 6.2. Configurer `application.yml`

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092

    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer

    consumer:
      group-id: notification-service
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "com.example.events"
```

Explications :

- `bootstrap-servers` : adresse du cluster Kafka.
- `group-id` : identifiant du groupe de consommateurs.
- `auto-offset-reset: earliest` : lorsque le groupe n'a aucun offset valide, commencer par le plus ancien événement encore disponible.
- `JsonSerializer` : sérialise les objets Java en JSON.
- `JsonDeserializer` : désérialise le JSON en objet Java.

La configuration de sérialisation doit être compatible entre le producteur et le consommateur. En production, il est recommandé de maîtriser explicitement les types autorisés et la stratégie de versionnement des événements.

### 6.3. Définir le modèle d'événement

Créer une classe ou un record représentant l'événement métier.

```java
package com.example.events;

import java.math.BigDecimal;
import java.time.Instant;

public record SimulationCreatedEvent(
        Long simulationId,
        BigDecimal montant,
        Integer duree,
        Instant dateCreation
) {
}
```

L'événement contient les informations nécessaires aux consommateurs, sans exposer inutilement toutes les données internes de l'entité JPA.

### 6.4. Publier un événement avec `KafkaTemplate`

Le producteur utilise `KafkaTemplate` pour envoyer l'événement au topic.

```java
package com.example.messaging;

import com.example.events.SimulationCreatedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class SimulationEventProducer {

    private static final String TOPIC = "simulation.created";

    private final KafkaTemplate<String, SimulationCreatedEvent> kafkaTemplate;

    public SimulationEventProducer(
            KafkaTemplate<String, SimulationCreatedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(SimulationCreatedEvent event) {
        kafkaTemplate.send(TOPIC, event.simulationId().toString(), event);
    }
}
```

Le deuxième argument de `send` est la clé de l'événement. Kafka utilise cette clé pour déterminer la partition, selon le partitionneur configuré. Une clé stable comme l'identifiant de simulation permet généralement de conserver l'ordre des événements d'une même simulation dans une partition donnée.

L'appel à `send` est asynchrone : pour gérer correctement les erreurs, il faut observer le résultat du `CompletableFuture` retourné ou configurer des mécanismes adaptés de journalisation et de reprise.

### 6.5. Publier après la création d'une simulation

Exemple simplifié dans un service métier :

```java
@Service
public class SimulationService {

    private final SimulationRepository simulationRepository;
    private final SimulationEventProducer eventProducer;

    public SimulationService(
            SimulationRepository simulationRepository,
            SimulationEventProducer eventProducer) {
        this.simulationRepository = simulationRepository;
        this.eventProducer = eventProducer;
    }

    @Transactional
    public Simulation creerSimulation(Simulation simulation) {
        Simulation saved = simulationRepository.save(simulation);

        SimulationCreatedEvent event = new SimulationCreatedEvent(
                saved.getId(),
                saved.getMontant(),
                saved.getDuree(),
                Instant.now()
        );

        eventProducer.publish(event);

        return saved;
    }
}
```

**Attention à la cohérence transactionnelle :** si l'événement est publié alors que la transaction en base de données n'est pas encore validée, il est possible que Kafka contienne un événement relatif à une opération finalement annulée.

Pour une application critique, il est préférable d'étudier le **Transactional Outbox Pattern** : l'événement est enregistré dans la même transaction que les données métier, puis publié vers Kafka par un processus dédié.

### 6.6. Consommer un événement avec `@KafkaListener`

Le microservice Notification peut écouter le topic :

```java
package com.example.messaging;

import com.example.events.SimulationCreatedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class SimulationEventConsumer {

    @KafkaListener(
        topics = "simulation.created",
        groupId = "notification-service"
    )
    public void consume(SimulationCreatedEvent event) {
        System.out.println(
            "Nouvelle simulation créée : " + event.simulationId()
        );

        // Déclencher ici le traitement de notification.
    }
}
```

Le consommateur reçoit l'événement et peut déclencher un traitement métier.

Dans un environnement réel, il faut aussi configurer les stratégies de gestion des erreurs, les tentatives de reprise, les éventuelles dead-letter topics et le mode de validation des offsets.

## 7. Fiabilité et bonnes pratiques

### 7.1. Idempotence des consommateurs

Un même événement peut être traité plusieurs fois, par exemple à la suite d'une reprise après erreur.

Le consommateur doit donc être conçu pour éviter les effets de bord dupliqués.

Exemples :

- Enregistrer l'identifiant d'événement déjà traité.
- Garantir qu'une notification ne soit pas envoyée plusieurs fois pour le même événement.
- Utiliser une contrainte d'unicité en base pour les traitements qui doivent être uniques.

### 7.2. Gestion des erreurs

Prévoir :

- Des tentatives de reprise limitées.
- Un délai entre les tentatives lorsque cela est pertinent.
- Une stratégie pour les événements qui échouent définitivement.
- Un mécanisme d'alerte et de retraitement.
- Une surveillance des consumer lag et des erreurs de désérialisation.

### 7.3. Évolution des événements

Les événements constituent un contrat entre plusieurs services.

Il est recommandé de :

- Versionner les schémas lorsque nécessaire.
- Éviter les changements incompatibles sans stratégie de migration.
- Ne pas publier des données personnelles ou sensibles inutiles.
- Documenter la signification des champs et les règles métier.

### 7.4. Sécurité

En production, utiliser une configuration de sécurité adaptée :

- Authentification des clients Kafka.
- Chiffrement TLS des communications.
- Autorisations par topic et par groupe de consommateurs.
- Gestion sécurisée des secrets.
- Traçabilité et supervision des accès.

## 8. Quand utiliser Kafka ?

Kafka est particulièrement pertinent lorsque l'application doit :

- Diffuser les événements métier à plusieurs systèmes.
- Conserver les événements pendant une durée définie.
- Permettre aux consommateurs de relire les événements.
- Traiter un volume important d'événements.
- Alimenter des plateformes de reporting, d'audit ou de données.
- Découpler les producteurs des consommateurs à l'échelle de plusieurs microservices.

Pour une simple distribution de tâches ou un routage flexible de messages, RabbitMQ peut être plus adapté. Pour un flux d'événements partagé, conservé et consommé par plusieurs applications indépendantes, Kafka constitue une option particulièrement intéressante.

## 9. À retenir

L'Event Streaming est un **concept architectural** qui permet de publier et de traiter des événements en continu.

Apache Kafka est une **technologie qui implémente ce concept** en fournissant des topics, des partitions, une conservation configurable et des mécanismes de consommation indépendants.

Dans une architecture microservices, Kafka permet aux services de communiquer par événements sans dépendre directement les uns des autres. La fiabilité du système repose néanmoins sur la cohérence transactionnelle, l'idempotence, la gestion des erreurs, la sécurité et la supervision.
