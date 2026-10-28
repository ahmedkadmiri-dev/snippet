# Resilience — Architecture & Design Patterns

## 1. Présentation

La **résilience logicielle** est la capacité d'un système à continuer de fournir un service acceptable malgré des erreurs, des pannes partielles, des ralentissements ou l'indisponibilité de certaines dépendances.

Dans une architecture distribuée, une API peut dépendre de plusieurs microservices, bases de données ou partenaires externes. La résilience permet de limiter la propagation des incidents, de préserver les fonctionnalités essentielles et de rétablir le fonctionnement normal du système.

Ce dossier présente les principaux concepts et patterns de résilience, avec leurs principes de fonctionnement, leurs cas d'utilisation et leurs implémentations.

## 2. Projet POC Spring Boot

Un **projet POC Spring Boot complet et exécutable** accompagne cette documentation afin d'illustrer concrètement la mise en œuvre des mécanismes de résilience dans une architecture microservices.

Le POC repose notamment sur **Spring Boot, Spring Cloud OpenFeign et Resilience4j**.

### Objectifs du POC

- Illustrer les mécanismes de résilience sur des appels HTTP vers un partenaire externe.
- Comprendre le comportement des patterns face aux erreurs, aux timeouts et aux indisponibilités.
- Disposer d'exemples Java exécutables et de tests permettant de vérifier les comportements attendus.
- Expérimenter les paramètres de configuration et observer leur impact.
- Servir de référence technique pour l'intégration de ces mécanismes dans des applications réelles.

### Architecture du POC

```text
resilience-poc/
│
├── API omnicanale
│   ├── Controller
│   ├── Service
│   └── DTO
│
├── Couche de résilience
│   ├── Circuit Breaker
│   ├── Retry + Backoff
│   ├── Timeout
│   ├── Bulkhead
│   ├── Rate Limiter
│   └── Fallback
│
├── Client HTTP
│   └── OpenFeign
│
├── Partner Simulator
│   └── Simulation des succès, erreurs,
│       lenteurs et indisponibilités
│
├── Configuration
│   └── application.yml
│
└── Tests