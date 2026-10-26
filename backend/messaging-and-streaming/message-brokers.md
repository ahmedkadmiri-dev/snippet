# Message Broker — Communication asynchrone entre microservices avec RabbitMQ

## 1. Objectif

Un **Message Broker** est un composant intermédiaire qui permet à plusieurs applications ou microservices de communiquer en échangeant des messages, sans nécessiter un appel direct entre le producteur et le consommateur.

Il facilite le découplage des services, la distribution des traitements et la communication asynchrone.

Les principaux objectifs sont :

- Réduire le couplage entre les microservices.
- Exécuter des traitements en arrière-plan.
- Absorber les variations de charge grâce aux files de messages.
- Distribuer des messages à plusieurs consommateurs.
- Améliorer la résilience des systèmes distribués.

## 2. Principe de fonctionnement

Dans une communication synchrone classique, un microservice appelle directement un autre service via HTTP et attend sa réponse.

Avec un Message Broker, le producteur publie un message dans un broker. Le consommateur récupère ensuite le message et exécute le traitement demandé.

### Architecture

```text
                    Communication HTTP
                           |
                           v
                  +------------------+
                  |  API Gateway     |
                  +------------------+
                           |
                           v
                  +------------------+
                  | Microservice A   |
                  |    Producer      |
                  +------------------+
                           |
                           | Publication d'un message
                           v
                  +------------------+
                  |  Message Broker  |
                  |                  |
                  |  Exchange/Queue  |
                  +------------------+
                           |
                           | Consommation du message
                           v
                  +------------------+
                  | Microservice B   |
                  |    Consumer      |
                  +------------------+
```

Le producteur n'a pas besoin d'attendre la fin du traitement métier effectué par le consommateur.

**Attention :** la publication d'un message ne garantit pas automatiquement sa livraison ni la réussite du traitement. Ces garanties dépendent de la configuration du broker et des mécanismes de fiabilité mis en place.

## 3. Producteur, consommateur et broker

### Producer

Le producteur est le composant qui publie un message.

Exemple : le microservice `moteur-financier` publie un événement après la création d'une simulation.

### Message Broker

Le broker reçoit les messages et les distribue selon ses règles de routage.

Il peut gérer les files d'attente, la persistance, les acquittements et les redélivrances, selon la technologie et la configuration.

### Consumer

Le consommateur reçoit le message et exécute le traitement correspondant.

Exemple : `notification-service` consomme un événement pour envoyer un SMS au client.

## 4. RabbitMQ vs Apache Kafka

RabbitMQ et Apache Kafka permettent la communication asynchrone, mais ils reposent sur des modèles différents.

| Critère | RabbitMQ | Apache Kafka |
|---|---|---|
| Modèle | Broker de messages | Plateforme de streaming d'événements |
| Architecture | Exchanges, queues et bindings | Topics, partitions et offsets |
| Cas d'usage courant | Tâches asynchrones et routage | Flux d'événements et intégration de données |
| Consommation | Messages distribués aux consommateurs | Consommateurs pilotés par leurs offsets |
| Relecture | Selon l'état du message et la configuration | Selon les offsets et la rétention |
| Routage | Exchanges et routing keys | Topics et clés de partitionnement |

### Quand utiliser RabbitMQ ?

- Envoyer des SMS ou des e-mails en arrière-plan.
- Exécuter des traitements différés.
- Distribuer des tâches à des workers.
- Gérer des files de commandes.
- Découpler des microservices métier.

### Quand utiliser Kafka ?

- Diffuser des événements métier à plusieurs systèmes.
- Construire des pipelines de données.
- Alimenter des systèmes analytiques.
- Rejouer des événements conservés.
- Traiter des flux d'événements à grande échelle.

Le choix dépend des exigences métier, du volume, du routage, de la rétention et des besoins de rejeu.

## 5. Exemple métier : moteur financier et notification

Imaginons une application bancaire composée de deux microservices :

- `moteur-financier` : gère les simulations de financement.
- `notification-service` : envoie les notifications SMS ou e-mail.

Lorsqu'une simulation est créée, le moteur financier publie un événement `SimulationCreatedEvent`.

Le service de notification consomme cet événement et envoie le message au client.

```text
moteur-financier
       |
       | SimulationCreatedEvent
       v
    RabbitMQ
       |
       | simulation.created
       v
notification-service
       |
       v
 Envoi du SMS
```

Le moteur financier peut terminer sa requête HTTP sans attendre la fin de l'envoi du SMS.

## 6. Implémentation avec Spring Boot

### 6.1. Prérequis

- Java 21.
- Spring Boot 3.x.
- Maven.
- Docker.
- RabbitMQ.

Les deux microservices sont des applications Spring Boot indépendantes.

### 6.2. Démarrer RabbitMQ avec Docker Compose

Créer un fichier `docker-compose.yml` :

```yaml
services:
  rabbitmq:
    image: rabbitmq:4-management
    container_name: rabbitmq
    hostname: rabbitmq
    ports:
      - "5672:5672"
      - "15672:15672"
    environment:
      RABBITMQ_DEFAULT_USER: appuser
      RABBITMQ_DEFAULT_PASS: ${RABBITMQ_PASSWORD:?Define RABBITMQ_PASSWORD}
    volumes:
      - rabbitmq_data:/var/lib/rabbitmq
    restart: unless-stopped

volumes:
  rabbitmq_data:
```

Définir la variable d'environnement `RABBITMQ_PASSWORD` avant le démarrage.

Lancer le broker :

```bash
docker compose up -d
```

Interfaces locales :

- Console d'administration : `http://localhost:15672`
- Port AMQP : `localhost:5672`

Pour un environnement de production, utiliser des secrets sécurisés, TLS et des comptes disposant de permissions limitées.

### 6.3. Dépendance Maven

Ajouter cette dépendance au `pom.xml` des deux microservices :

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

Spring Boot gère la version de la dépendance lorsque le projet utilise son parent ou son BOM.

Cette dépendance fournit notamment `RabbitTemplate` et l'annotation `@RabbitListener`.

### 6.4. Configuration applicative

Dans `application.yml` :

```yaml
spring:
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USERNAME:appuser}
    password: ${RABBITMQ_PASSWORD}
    virtual-host: /
    connection-timeout: 5s
```

En environnement conteneurisé, remplacer `localhost` par le nom réseau du broker, par exemple `rabbitmq`.

Ne pas stocker les mots de passe de production dans le dépôt Git.

### 6.5. Définir le contrat de message

Créer la classe `SimulationCreatedEvent.java`.

Dans un projet réel, ce contrat peut être partagé par les deux microservices dans une bibliothèque dédiée, ou défini dans un schéma d'événements versionné.

```java
package com.example.messaging.event;

import java.math.BigDecimal;
import java.time.Instant;

public record SimulationCreatedEvent(
        String eventId,
        Long simulationId,
        String email,
        BigDecimal montant,
        Instant createdAt
) {
}
```

Exemple de message JSON :

```json
{
  "eventId": "evt-123456",
  "simulationId": 9798429,
  "email": "client@example.com",
  "montant": 500000,
  "createdAt": "2026-10-26T10:00:00Z"
}
```

L'attribut `eventId` permet d'identifier l'événement et de mettre en place une stratégie d'idempotence.

### 6.6. Déclarer l'exchange, la queue et le binding

Créer `RabbitMQConfig.java` dans le producteur :

```java
package com.example.financial.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "simulation.exchange";
    public static final String QUEUE = "simulation.notification.queue";
    public static final String ROUTING_KEY = "simulation.created";

    @Bean
    public TopicExchange simulationExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(QUEUE).build();
    }

    @Bean
    public Binding notificationBinding(
            Queue notificationQueue,
            TopicExchange simulationExchange) {

        return BindingBuilder.bind(notificationQueue)
                .to(simulationExchange)
                .with(ROUTING_KEY);
    }
}
```

Les composants déclarés sont :

- **Exchange** : `simulation.exchange`
- **Queue** : `simulation.notification.queue`
- **Routing key** : `simulation.created`

L'exchange de type `topic` permet de router les messages en fonction de leur routing key.

Le consommateur doit disposer de la même déclaration d'infrastructure, ou utiliser une infrastructure gérée séparément.

### 6.7. Configurer la conversion JSON

Ajouter un convertisseur JSON dans la configuration RabbitMQ.

Avec les versions de Spring AMQP qui le prennent en charge, utiliser le convertisseur Jackson adapté à la version du projet.

Exemple :

```java
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;

@Bean
public MessageConverter jacksonMessageConverter() {
    return new JacksonJsonMessageConverter();
}
```

Le `RabbitTemplate` et les listeners configurés par Spring Boot utilisent ce convertisseur pour sérialiser et désérialiser les objets.

Vérifier le nom de la classe disponible dans la version de Spring AMQP utilisée.

### 6.8. Créer le producteur

Créer `SimulationEventPublisher.java` dans `moteur-financier` :

```java
package com.example.financial.messaging;

import com.example.financial.config.RabbitMQConfig;
import com.example.messaging.event.SimulationCreatedEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class SimulationEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public SimulationEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(SimulationCreatedEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.ROUTING_KEY,
                event
        );
    }
}
```

La méthode `convertAndSend()` publie le message vers l'exchange en utilisant la routing key spécifiée.

Pour une publication critique, configurer les publisher confirms et les retours de messages non routables. Un appel sans exception à `convertAndSend()` ne constitue pas à lui seul une confirmation de persistance par le broker.

### 6.9. Appeler le producteur depuis le service métier

Exemple simplifié :

```java
@Service
public class SimulationService {

    private final SimulationRepository repository;
    private final SimulationEventPublisher publisher;

    public SimulationService(
            SimulationRepository repository,
            SimulationEventPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    public Simulation creerSimulation(Simulation simulation) {

        Simulation saved = repository.save(simulation);

        SimulationCreatedEvent event = new SimulationCreatedEvent(
                java.util.UUID.randomUUID().toString(),
                saved.getId(),
                saved.getEmail(),
                saved.getMontant(),
                java.time.Instant.now()
        );

        publisher.publish(event);

        return saved;
    }
}
```

Ce code illustre l'enchaînement métier, mais il ne garantit pas la cohérence entre l'enregistrement en base et la publication.

Si la transaction de base est validée et que RabbitMQ devient indisponible avant la publication, l'événement peut être perdu. Pour les traitements critiques, utiliser le pattern **Transactional Outbox**.

### 6.10. Créer le consommateur

Dans `notification-service`, déclarer les mêmes noms d'exchange, de queue et de routing key.

Créer `SimulationCreatedListener.java` :

```java
package com.example.notification.messaging;

import com.example.messaging.event.SimulationCreatedEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class SimulationCreatedListener {

    @RabbitListener(queues = "simulation.notification.queue")
    public void onSimulationCreated(SimulationCreatedEvent event) {

        System.out.println(
                "Simulation reçue : " + event.simulationId()
        );

        envoyerNotification(event);
    }

    private void envoyerNotification(SimulationCreatedEvent event) {
        // Appeler ici le service d'envoi SMS ou e-mail.
        System.out.println(
                "Notification pour : " + event.email()
        );
    }
}
```

Lorsque le message arrive dans la queue, Spring AMQP déclenche le listener.

Le traitement est indépendant de la requête HTTP initiale.

Dans une application réelle, utiliser un service de notification dédié et une journalisation structurée plutôt que `System.out.println`.

## 7. Gestion des erreurs

Une implémentation de production doit prévoir :

- Des retries avec un nombre maximal de tentatives.
- Des délais entre les tentatives.
- Une Dead Letter Exchange (DLX) et une Dead Letter Queue (DLQ).
- Des logs corrélés à l'identifiant de l'événement.
- Des alertes sur les messages non traités.
- Une procédure de rejeu après correction de l'erreur.

Avec l'acquittement automatique du listener Spring, les erreurs peuvent entraîner des redélivrances ou des rejets selon la configuration du conteneur.

Il faut configurer explicitement les politiques de retry et de dead lettering, et éviter les redélivrances infinies.

## 8. Idempotence du consommateur

RabbitMQ peut redélivrer un message. Le consommateur doit donc être conçu pour supporter les doublons.

Le principe consiste à vérifier l'identifiant de l'événement avant de répéter un traitement métier.

Pour une implémentation robuste :

- Enregistrer les événements traités dans une table dédiée.
- Ajouter une contrainte unique sur `eventId`.
- Rendre l'enregistrement du traitement atomique.
- Gérer les effets externes, notamment l'envoi de SMS, avec une stratégie d'idempotence adaptée.

Une simple vérification suivie d'une insertion n'est pas suffisante en présence de traitements concurrents ou de pannes.

## 9. Transactional Outbox

Le pattern Transactional Outbox permet de résoudre le problème de cohérence entre une transaction en base de données et la publication d'un événement.

Principe :

1. Enregistrer la simulation dans la base.
2. Enregistrer l'événement dans une table `outbox_event` au sein de la même transaction.
3. Un processus dédié publie les événements de cette table vers RabbitMQ.
4. Marquer les événements comme publiés après confirmation.
5. Reprendre la publication après un incident.

Ce mécanisme réduit le risque de perdre un événement entre l'enregistrement métier et la publication. Le consommateur doit néanmoins rester idempotent, car une publication peut être répétée.

## 10. Sécurité et supervision

### Sécurité

- Utiliser TLS pour les connexions réseau.
- Créer des comptes dédiés avec des permissions minimales.
- Gérer les secrets hors du code source.
- Limiter l'exposition de la console d'administration.
- Éviter de publier des données personnelles ou bancaires inutiles.

### Supervision

Surveiller notamment :

- Le nombre de messages en attente.
- Les messages non acquittés.
- Le débit de publication et de consommation.
- Les erreurs et redélivrances.
- La taille des DLQ.
- La latence de traitement.
- L'état des connexions au broker.

La console RabbitMQ est utile pour l'inspection. En production, compléter avec des métriques, des dashboards et des alertes.

## 11. Bonnes pratiques

- Utiliser des contrats de messages versionnés.
- Garder les messages compacts.
- Prévoir l'idempotence dès la conception.
- Configurer les confirmations de publication.
- Définir des retries bornés et une DLQ.
- Surveiller les files et les consommateurs.
- Utiliser Transactional Outbox lorsque la cohérence métier l'exige.
- Ne pas considérer la publication comme la preuve que le traitement du consommateur est terminé.

## 12. Conclusion

Un Message Broker facilite la communication asynchrone entre les microservices en séparant la publication d'un message de son traitement.

RabbitMQ est particulièrement adapté aux scénarios de distribution de tâches, de routage et de traitements différés. Kafka répond à d'autres besoins, notamment la conservation et la relecture de flux d'événements.

Dans un système bancaire, la fiabilité dépend autant de la conception des transactions, de l'idempotence et de la gestion des erreurs que du choix du broker.

## 13. Références officielles

- RabbitMQ : https://www.rabbitmq.com/docs
- Spring AMQP : https://docs.spring.io/spring-amqp/reference/
- Spring Boot AMQP : https://docs.spring.io/spring-boot/reference/messaging/amqp.html
- Apache Kafka : https://kafka.apache.org/documentation/