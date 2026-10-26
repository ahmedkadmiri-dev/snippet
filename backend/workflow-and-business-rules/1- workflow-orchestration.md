# Workflow Orchestration — Orchestration des processus métier

## 1. Objectif

L'orchestration des workflows consiste à coordonner l'exécution de plusieurs étapes métier afin d'atteindre un objectif défini.

Dans une architecture microservices, elle permet de piloter un processus métier impliquant plusieurs services, de gérer les conditions de passage entre les étapes et de suivre l'état d'avancement du processus.

Les objectifs sont :

- Structurer les étapes d'un processus métier.
- Définir l'ordre d'exécution des tâches.
- Gérer les conditions, les décisions et les branches alternatives.
- Coordonner plusieurs microservices.
- Gérer les erreurs, les délais et les reprises.
- Assurer la traçabilité du processus métier.

## 2. Qu'est-ce qu'un workflow ?

Un workflow est un enchaînement d'activités permettant de réaliser un processus métier.

Il peut être simple, avec des étapes exécutées successivement, ou complexe, avec des conditions, des traitements parallèles, des validations humaines et des mécanismes de reprise.

Exemples :

- Gestion d'une demande de financement.
- Ouverture d'un compte bancaire.
- Validation d'un dossier client.
- Traitement d'un paiement.
- Gestion d'une demande de crédit.

## 3. Qu'est-ce que l'orchestration ?

L'orchestration consiste à disposer d'un composant central qui connaît les étapes du processus et décide quand déclencher chaque traitement.

Prenons l'exemple d'une demande de financement :

1. Recevoir la demande.
2. Vérifier les informations du client.
3. Calculer la capacité financière.
4. Évaluer le risque du dossier.
5. Déterminer la décision.
6. Notifier le client.

L'orchestrateur pilote ces étapes en fonction des résultats obtenus.

```text
                 +----------------------+
                 | Demande de financement|
                 +----------------------+
                            |
                            v
                 +----------------------+
                 | Vérification client  |
                 +----------------------+
                            |
                            v
                 +----------------------+
                 | Calcul de capacité   |
                 +----------------------+
                            |
                            v
                 +----------------------+
                 | Évaluation du risque |
                 +----------------------+
                            |
                            v
                 +----------------------+
                 | Décision de dossier  |
                 +----------------------+
                            |
                            v
                 +----------------------+
                 | Notification client  |
                 +----------------------+
```

L'orchestrateur ne réalise pas nécessairement lui-même les calculs métier. Il coordonne les services responsables de ces traitements.

## 4. Orchestration vs chorégraphie

Il existe deux grandes approches pour coordonner les microservices.

### 4.1. Orchestration

Un composant central pilote le processus et appelle les services ou déclenche leurs traitements.

```text
                +------------------+
                |   Orchestrateur  |
                +------------------+
                  /      |       \
                 v       v        v
             Service A Service B Service C
```

**Avantages :**

- Vue centralisée du processus métier.
- Suivi de l'état du workflow.
- Gestion explicite des conditions et des erreurs.
- Processus plus facile à auditer.
- Coordination adaptée aux processus longs et complexes.

**Inconvénients :**

- Introduction d'un composant supplémentaire.
- Conception et exploitation plus complexes.
- Risque de concentrer trop de logique métier dans l'orchestrateur.

### 4.2. Chorégraphie

Chaque microservice réagit aux événements publiés par les autres services, sans orchestrateur central qui pilote toutes les étapes.

```text
+------------------+
| Service Simulation|
+------------------+
         |
         | SimulationCreated
         v
+------------------+
| Service Scoring  |
+------------------+
         |
         | ScoringCompleted
         v
+------------------+
| Service Decision |
+------------------+
```

Cette approche s'appuie souvent sur un système de messagerie comme RabbitMQ ou une plateforme d'Event Streaming comme Kafka.

**Avantages :**

- Faible couplage direct entre les services.
- Autonomie des microservices.
- Bonne adaptation à la diffusion d'événements.

**Inconvénients :**

- Vue globale du processus moins évidente.
- Traçabilité et débogage plus difficiles lorsque le nombre de services augmente.
- Gestion des erreurs et des événements dupliqués à prévoir dans chaque service concerné.

### 4.3. Comparaison

| Critère                   | Orchestration                                    | Chorégraphie                                            |
| ------------------------- | ------------------------------------------------ | ------------------------------------------------------- |
| Pilotage                  | Centralisé                                       | Distribué                                               |
| Connaissance du processus | L'orchestrateur connaît le workflow              | Le processus émerge des réactions aux événements        |
| Couplage                  | Les services sont coordonnés par l'orchestrateur | Les services sont reliés par des contrats événementiels |
| Suivi global              | Généralement plus simple                         | Nécessite une traçabilité distribuée                    |
| Cas d'usage               | Processus longs, complexes, avec validations     | Réactions événementielles et traitements découplés      |

Les deux approches peuvent coexister dans une même architecture.

## 5. Workflow et microservices

Un workflow métier peut coordonner plusieurs microservices indépendants.

Exemple pour un dossier de financement :

| Étape                   | Microservice responsable | Résultat attendu    |
| ----------------------- | ------------------------ | ------------------- |
| 1. Création du dossier  | `granting-backend`       | Dossier enregistré  |
| 2. Calcul financier     | `moteur-financier`       | Capacité calculée   |
| 3. Évaluation du risque | `scoring-service`        | Résultat du scoring |
| 4. Décision métier      | Service de décision      | Décision produite   |
| 5. Notification         | `notification-service`   | Client informé      |

L'orchestrateur coordonne les appels ou les traitements et conserve l'état du processus.

Chaque microservice reste responsable de sa propre logique métier et de ses données.

## 6. Gestion de l'état du workflow

Un workflow peut durer quelques secondes ou plusieurs jours. Il est donc nécessaire de conserver son état lorsqu'il doit reprendre après une interruption.

Exemples d'états :

- `STARTED` : processus démarré.
- `IN_PROGRESS` : processus en cours.
- `WAITING_FOR_VALIDATION` : attente d'une validation humaine.
- `COMPLETED` : processus terminé.
- `FAILED` : processus en erreur.
- `CANCELLED` : processus annulé.

Ces statuts sont des exemples de modélisation : les états réels dépendent du moteur et des règles métier.

Pour les processus longs, il faut prévoir la persistance de l'état, la reprise après redémarrage et la gestion des délais.

## 7. Gestion des erreurs et des reprises

Une étape peut échouer pour différentes raisons :

- Service temporairement indisponible.
- Timeout lors d'un appel HTTP.
- Erreur technique en base de données.
- Données métier invalides.
- Refus métier lors d'une étape de validation.

Les stratégies possibles comprennent :

- Réessayer une opération en cas d'erreur temporaire.
- Appliquer un délai progressif entre les tentatives.
- Suspendre le processus pour intervention humaine.
- Déclencher une procédure de compensation.
- Arrêter le workflow si une condition métier l'impose.

### Compensation et Saga

Dans une architecture distribuée, une transaction unique ne couvre généralement pas les bases de données de tous les microservices.

Le pattern Saga permet de coordonner une suite de transactions locales et, lorsque nécessaire, d'exécuter des actions compensatoires.

Exemple :

1. Réserver un montant.
2. Créer un dossier.
3. Une étape ultérieure échoue.
4. Exécuter une compensation pour libérer la réservation.

Une compensation n'est pas nécessairement un rollback technique : elle consiste à exécuter une nouvelle opération métier destinée à corriger les effets précédents.

## 8. Technologies associées

Plusieurs technologies permettent de mettre en œuvre des workflows.

| Technologie ou standard | Rôle                                                                                            |
| ----------------------- | ----------------------------------------------------------------------------------------------- |
| BPMN                    | Standard de modélisation des processus métier                                                   |
| Camunda                 | Moteur et plateforme d'automatisation de processus, selon la version et les composants utilisés |
| Temporal                | Orchestration de workflows durables principalement définis en code                              |
| AWS Step Functions      | Orchestration de workflows dans l'écosystème AWS                                                |
| Spring State Machine    | Gestion de machines à états dans une application Java                                           |
| RabbitMQ                | Transport et routage de messages asynchrones                                                    |
| Apache Kafka            | Publication, conservation et consommation de flux d'événements                                  |

RabbitMQ et Kafka ne sont pas, à eux seuls, des moteurs d'orchestration métier : ils fournissent des mécanismes de communication qui peuvent être utilisés dans un workflow.

## 9. Quand utiliser un moteur de workflow ?

Un moteur d'orchestration est particulièrement utile lorsque :

- Le processus comporte de nombreuses étapes.
- Les étapes dépendent de résultats précédents.
- Des validations humaines sont nécessaires.
- Le processus peut rester en attente pendant plusieurs heures ou plusieurs jours.
- Les erreurs doivent être suivies et les traitements repris.
- L'entreprise doit visualiser l'état de chaque dossier.
- Les règles et le déroulement du processus doivent évoluer sans réécrire toute l'application.

Pour un traitement simple et immédiat, un service Spring Boot classique peut suffire. Un moteur de workflow devient intéressant lorsque la coordination, la durée, la traçabilité ou la complexité du processus le justifient.

## 10. À retenir

L'orchestration des workflows permet de piloter un processus métier à travers plusieurs étapes et plusieurs microservices.

- **Workflow** : enchaînement d'activités métier.
- **Orchestration** : pilotage centralisé de cet enchaînement.
- **Chorégraphie** : coordination distribuée fondée sur les interactions et les événements.
- **BPMN** : notation graphique pour modéliser un processus.
- **DMN** : notation pour formaliser les décisions métier.
- **Camunda** : technologie permettant notamment d'exécuter des processus modélisés, selon la version choisie.

Ces notions constituent la base avant d'aborder la modélisation BPMN, les tables de décision DMN et leur implémentation avec Camunda et Spring Boot.
