# Resilience — Concepts et implémentations

## 1. Présentation

La **résilience logicielle** désigne la capacité d'un système à continuer de fournir un service acceptable malgré des pannes, des erreurs, des ralentissements ou l'indisponibilité de certaines dépendances.

Dans une architecture distribuée, elle vise à limiter la propagation des incidents, à préserver les fonctionnalités essentielles et à faciliter le rétablissement du service.

Ce répertoire a pour objectif de **documenter les principaux concepts, patterns et mécanismes de résilience**, ainsi que leurs principes de fonctionnement et leurs cas d'utilisation.

## 2. Arborescence de la documentation

Les fichiers Markdown sont organisés par domaine afin de faciliter la compréhension et la consultation des concepts.

```text
resilience/
├── README.md
│
├── fundamentals/
│   ├── fault-tolerance.md
│   ├── resilience.md
│   └── recovery.md
│
├── availability/
│   ├── high-availability.md
│   ├── redundancy.md
│   └── failover.md
│
├── patterns/
│   ├── circuit-breaker.md
│   ├── retry.md
│   ├── timeout.md
│   ├── bulkhead.md
│   ├── fallback.md
│   ├── graceful-degradation.md
│   └── fault-isolation.md
│
├── traffic-control/
│   ├── rate-limiting.md
│   └── backpressure.md
│
├── reliability/
│   ├── idempotency.md
│   ├── backoff.md
│   └── health-check.md
│
└── observability/
    ├── monitoring.md
    └── alerting.md
```

### 2.1. Fundamentals — Fondamentaux

Cette section présente les principes généraux de la résilience, la tolérance aux pannes et les mécanismes de récupération après incident.

### 2.2. Availability — Disponibilité

Cette section traite des approches permettant de maintenir un service accessible, notamment la haute disponibilité, la redondance et le basculement vers une ressource de secours.

### 2.3. Patterns — Patterns de résilience

Cette section décrit les principaux mécanismes de protection des applications distribuées : circuit breaker, retry, timeout, bulkhead, fallback, dégradation gracieuse et isolation des pannes.

### 2.4. Traffic Control — Contrôle du trafic

Cette section présente les mécanismes permettant de réguler les flux de requêtes et de données afin de préserver les capacités de traitement des services.

### 2.5. Reliability — Fiabilité

Cette section aborde l'idempotence, les stratégies de temporisation entre tentatives et les contrôles de santé des services.

### 2.6. Observability — Observabilité

Cette section présente les mécanismes de supervision, de collecte des métriques et de déclenchement des alertes pour détecter et analyser les incidents.

## 3. Projet POC Spring Boot

En complément de cette documentation conceptuelle, un **projet POC Spring Boot utilisant Resilience4j et Spring Cloud OpenFeign** permet d'illustrer et d'implémenter concrètement les principaux mécanismes de résilience.

L'objectif est de disposer d'un projet exécutable servant de référence technique pour comprendre le comportement de ces mécanismes dans un scénario d'appels HTTP entre services.

### 3.1. Objectifs du POC

- Mettre en œuvre les patterns de résilience avec Resilience4j.
- Utiliser OpenFeign pour appeler un service partenaire.
- Simuler des erreurs HTTP, des timeouts, des lenteurs et des indisponibilités.
- Observer le comportement des mécanismes de protection.
- Valider les scénarios à l'aide de tests automatisés.
- Fournir des exemples de configuration et de code réutilisables dans des applications Spring Boot.

### 3.2. Architecture du projet

```text
resilience-poc/
├── pom.xml
│
├── src/main/java/com/example/resilience/
│   ├── controller/
│   │   └── SimulationController.java
│   │
│   ├── service/
│   │   ├── SimulationService.java
│   │   └── PartnerResilienceService.java
│   │
│   ├── client/
│   │   └── PartnerFeignClient.java
│   │
│   ├── simulator/
│   │   └── PartnerSimulatorController.java
│   │
│   ├── config/
│   │   └── FeignConfig.java
│   │
│   └── dto/
│       ├── SimulationRequest.java
│       └── SimulationResponse.java
│
├── src/main/resources/
│   └── application.yml
│
└── src/test/java/com/example/resilience/
    ├── CircuitBreakerTest.java
    ├── RetryTest.java
    ├── TimeoutTest.java
    └── FallbackTest.java
```

### 3.3. Rôle des composants

| Composant | Responsabilité |
|---|---|
| `SimulationController` | Exposer l'API de démonstration. |
| `SimulationService` | Orchestrer le traitement métier. |
| `PartnerResilienceService` | Appliquer les mécanismes de résilience aux appels sortants. |
| `PartnerFeignClient` | Réaliser les appels HTTP vers le partenaire. |
| `PartnerSimulatorController` | Simuler différents comportements du partenaire. |
| `FeignConfig` | Configurer le client HTTP, notamment les timeouts. |
| `application.yml` | Définir les paramètres de Resilience4j et de l'application. |
| `src/test` | Vérifier les comportements attendus lors des incidents. |

### 3.4. Concepts illustrés par le POC

Le POC permet notamment de mettre en œuvre :

- **Circuit Breaker** : interrompre les appels vers un partenaire défaillant.
- **Retry et Backoff** : réessayer les opérations après certains échecs, avec des délais adaptés.
- **Timeout** : limiter la durée d'attente des appels HTTP.
- **Bulkhead** : limiter le nombre d'appels concurrents.
- **Rate Limiter** : contrôler le nombre d'appels autorisés.
- **Fallback** : proposer une réponse alternative lorsqu'un appel échoue.

Les autres concepts, comme la haute disponibilité, le failover, l'idempotence, la backpressure et l'alerting, peuvent nécessiter des composants ou des scénarios complémentaires.

## 4. Relation entre la documentation et le POC

La documentation Markdown et le projet POC ont des rôles complémentaires :

- **Documentation** : expliquer les concepts, leurs objectifs, leurs différences, leurs avantages et leurs limites.
- **POC** : illustrer leur mise en œuvre technique avec Spring Boot, Resilience4j et OpenFeign, ainsi que des scénarios exécutables et des tests.

Cette séparation permet de conserver une documentation indépendante des choix technologiques tout en disposant d'une implémentation concrète pour expérimenter les mécanismes.

## 5. À retenir

Ce répertoire constitue une base de référence sur les concepts de résilience en architecture logicielle. Il est complété par un POC Spring Boot permettant d'expérimenter les principaux patterns et de comprendre leur application dans un environnement de communication entre services.
