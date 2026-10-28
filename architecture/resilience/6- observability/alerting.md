# Alerting — Détection et notification des incidents

## 1. Définition

L'**Alerting** est un mécanisme qui permet de détecter une situation anormale à partir de métriques, de logs ou d'événements, puis de notifier les personnes ou les systèmes concernés.

Son objectif est de permettre une intervention rapide lorsqu'un service devient indisponible, que ses performances se dégradent ou qu'un risque opérationnel apparaît.

Dans une architecture de microservices Spring Boot, l'alerting peut notamment signaler :

- Une augmentation importante des erreurs HTTP 5xx.
- Une hausse de la latence des API.
- Une indisponibilité d'une base de données.
- Un Circuit Breaker fréquemment ouvert.
- Une saturation du pool de connexions.
- Une augmentation des timeouts vers un service partenaire.
- Une accumulation de messages dans une file d'attente.

L'alerting complète le monitoring : le monitoring mesure et présente les informations, tandis que l'alerting attire l'attention sur les situations qui nécessitent une action.

## 2. Pourquoi mettre en place l'Alerting ?

Considérons une architecture composée de plusieurs microservices.

```text
Clients
   |
   v
API Gateway
   |
   v
Moteur financier
   |
   +------> Base de données
   |
   +------> Service de tarification
```

Supposons que le service de tarification commence à répondre en 8 secondes au lieu de 300 ms.

Cette dégradation peut provoquer :

- Des timeouts dans le moteur financier.
- Une augmentation des retries.
- Une saturation des threads.
- Une dégradation du temps de réponse de la Gateway.
- Une augmentation des erreurs visibles par les utilisateurs.

Sans alerting, l'équipe peut ne découvrir le problème qu'après les premières réclamations.

Avec une règle d'alerte, une notification peut être envoyée dès que le taux d'erreur ou la latence dépasse un seuil défini.

## 3. Comment fonctionne l'Alerting ?

Un système d'alerting comporte généralement quatre étapes.

```text
Collecte des données
        |
        v
Évaluation des règles
        |
        v
Détection d'une anomalie
        |
        v
Notification et intervention
```

### 3.1. Collecte des données

Les données proviennent notamment :

- Des métriques Prometheus.
- Des logs centralisés.
- Des traces distribuées.
- Des Health Checks.
- Des événements produits par les applications.

### 3.2. Évaluation des règles

Une règle définit la condition qui doit déclencher une alerte.

Exemple :

- Le taux d'erreur HTTP 5xx dépasse 5 % pendant cinq minutes.
- La latence P95 dépasse deux secondes pendant plusieurs minutes.
- Le nombre de connexions disponibles dans un pool devient insuffisant.
- Un Circuit Breaker reste ouvert trop longtemps.

Les seuils doivent être adaptés aux objectifs de disponibilité et aux contraintes métier.

### 3.3. Détection

Lorsque la condition est satisfaite, le système fait passer l'alerte dans un état actif.

Une durée minimale peut être définie pour éviter les notifications provoquées par des variations très brèves.

### 3.4. Notification

L'alerte peut être transmise par différents canaux :

- E-mail.
- Microsoft Teams ou Slack.
- Outil de gestion des incidents.
- SMS ou appel pour les incidents critiques.
- Système automatisé de remédiation.

Le canal doit être choisi en fonction de la gravité de l'incident et des procédures d'exploitation.

## 4. Les niveaux de gravité des alertes

Toutes les alertes ne nécessitent pas la même réaction.

| Niveau | Signification | Exemple |
|---|---|---|
| Information | Événement à surveiller | Hausse modérée du trafic |
| Warning | Risque de dégradation | Latence P95 supérieure à l'objectif |
| Critical | Incident nécessitant une intervention rapide | API indisponible ou taux d'échec très élevé |

Il est utile de définir pour chaque alerte :

- Sa gravité.
- Le service concerné.
- La condition de déclenchement.
- L'équipe responsable.
- La procédure de diagnostic.
- Les actions de remédiation éventuelles.

Une alerte critique doit clairement indiquer ce qui est en panne et quelle action est attendue.

## 5. Mettre en place l'Alerting avec Prometheus et Alertmanager

Une architecture courante repose sur Prometheus et Alertmanager.

<box gap={3}>
  <row align=start gap={3}>
    <AsyncImage query="Prometheus monitoring system official logo" aspectRatio="1:1" maxWidth="76px" />
    <box flex="1" gap={1}>
      **Prometheus**

      Collecte les métriques et évalue les règles d'alerte. Il détermine si les conditions définies sont satisfaites.
    </box>
  </row>
  <divider color="subtle" />
  <row align=start gap={3}>
    <AsyncImage query="Prometheus Alertmanager official logo" aspectRatio="1:1" maxWidth="76px" />
    <box flex="1" gap={1}>
      **Alertmanager**

      Reçoit les alertes, les regroupe, les déduplique et les distribue aux canaux de notification configurés.
    </box>
  </row>
</box>

### 5.1. Architecture générale

```text
Application Spring Boot
        |
        v
Spring Boot Actuator
        |
        v
Prometheus
        |
        | Évaluation des règles
        v
Alertmanager
        |
        +------> E-mail
        |
        +------> Teams / Slack
        |
        +------> Gestion des incidents
```

Prometheus détecte les conditions anormales. Alertmanager organise les notifications et les transmet aux destinataires.

### 5.2. Exemple de règle d'alerte

Voici une règle Prometheus qui déclenche une alerte lorsque le taux de réponses HTTP 5xx dépasse 5 % pendant cinq minutes.

```yaml
groups:
  - name: spring-boot-alerts
    rules:
      - alert: HighHttp5xxRate
        expr: |
          (
            sum(rate(http_server_requests_seconds_count{
              status=~"5.."
            }[5m]))
            /
            clamp_min(
              sum(rate(http_server_requests_seconds_count[5m])),
              0.001
            )
          ) > 0.05
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "Taux élevé d'erreurs HTTP 5xx"
          description: "Le taux d'erreurs HTTP 5xx dépasse 5 % pendant cinq minutes."
```

Cette règle suppose que les métriques utilisées sont bien exposées et collectées par Prometheus sous ces noms. Selon la version de Micrometer et la configuration de l'application, les noms et labels disponibles peuvent différer.

La clause `for: 5m` impose que la condition reste vraie pendant cinq minutes avant que l'alerte soit considérée comme déclenchée.

### 5.3. Exemple de règle de latence

```yaml
groups:
  - name: spring-boot-latency
    rules:
      - alert: HighApiLatency
        expr: |
          histogram_quantile(
            0.95,
            sum by (le) (
              rate(http_server_requests_seconds_bucket[5m])
            )
          ) > 2
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Latence API élevée"
          description: "Le percentile 95 du temps de réponse dépasse deux secondes."
```

Cette règle suppose que les buckets d'histogramme nécessaires sont disponibles. La configuration Micrometer doit être vérifiée pour garantir que ces séries sont réellement exposées.

## 6. Alerting dans une application Spring Boot

Spring Boot Actuator expose les métriques et les informations de santé, mais il ne fournit pas à lui seul toute la chaîne de notification.

Une configuration classique comprend :

1. Spring Boot Actuator pour exposer les métriques.
2. Micrometer pour instrumenter l'application.
3. Prometheus pour collecter les données.
4. Alertmanager pour distribuer les alertes.

Dépendances Maven typiques :

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

Configuration :

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus

  endpoint:
    health:
      show-details: never
```

Il faut ensuite configurer Prometheus pour interroger l'endpoint `/actuator/prometheus` et charger les règles d'alerte.

**Important :** exposer les métriques ne suffit pas pour déclencher des notifications. La collecte, les règles et le routage des alertes doivent être configurés séparément.

## 7. Alerting des mécanismes de résilience

Dans une application utilisant Resilience4j, il est pertinent de surveiller les mécanismes de protection.

### 7.1. Circuit Breaker

Alertes possibles :

- Circuit Breaker ouvert depuis une durée anormale.
- Augmentation importante du taux d'échec.
- Nombre élevé d'appels rejetés.

L'ouverture d'un Circuit Breaker n'est pas nécessairement un incident à elle seule : il peut fonctionner normalement en protégeant l'application d'un service distant défaillant. L'alerte doit donc être basée sur la durée, l'impact et les objectifs de service.

### 7.2. Retry

Alertes possibles :

- Forte augmentation du nombre de tentatives.
- Nombre élevé d'opérations échouant après toutes les tentatives.
- Temps de traitement total anormalement élevé.

Une augmentation des retries peut être le premier signe d'une dépendance instable.

### 7.3. Bulkhead

Alertes possibles :

- Augmentation des appels rejetés.
- Saturation prolongée de la capacité concurrente.
- Accumulation de tâches en attente dans les mécanismes associés.

### 7.4. Health Check

Alertes possibles :

- Instance indisponible.
- Nombre d'instances prêtes inférieur au minimum requis.
- Échec persistant de la readiness.
- Application incapable de démarrer correctement.

Les alertes doivent refléter un impact réel. Une alerte sur chaque changement d'état temporaire risque de générer trop de notifications.

## 8. Éviter la fatigue liée aux alertes

Un système d'alerting mal conçu peut produire trop de notifications, au point que les équipes finissent par les ignorer.

Ce phénomène est appelé *alert fatigue*.

Pour l'éviter :

- Ne créer des alertes que lorsqu'une action est nécessaire.
- Éviter les seuils trop sensibles.
- Regrouper les alertes qui ont une même cause.
- Dédupliquer les événements répétitifs.
- Prévoir une temporisation avant le déclenchement.
- Définir des règles de résolution automatique des alertes.
- Séparer les alertes opérationnelles des notifications purement informatives.
- Réviser régulièrement les règles en fonction des incidents réellement observés.

Une alerte utile doit répondre à trois questions : que se passe-t-il, quel est l'impact et que faut-il faire ?

## 9. Alerting et SRE : SLI, SLO et budget d'erreur

Les pratiques de *Site Reliability Engineering* permettent de baser les alertes sur les objectifs de fiabilité.

### 9.1. SLI — Service Level Indicator

Un SLI est une mesure quantitative de la qualité de service.

Exemples :

- Pourcentage de requêtes réussies.
- Latence P95.
- Pourcentage de traitements terminés dans le délai attendu.

### 9.2. SLO — Service Level Objective

Un SLO est un objectif de fiabilité défini sur une période.

Exemple : 99,9 % des requêtes éligibles doivent réussir sur une période de 30 jours.

### 9.3. Budget d'erreur

Le budget d'erreur représente la marge d'indisponibilité ou d'échec compatible avec le SLO.

Si l'objectif est de 99,9 % de disponibilité sur 30 jours, le budget théorique correspond à 0,1 % de la période, soit environ 43 minutes.

Les alertes peuvent être déclenchées lorsqu'un service consomme son budget d'erreur trop rapidement, plutôt que de réagir uniquement à un seuil technique arbitraire.

Cette approche permet de prioriser les incidents selon leur impact sur les engagements de service.

## 10. Bonnes pratiques

### 10.1. Alerter sur l'impact

Privilégier les alertes qui révèlent une dégradation du service rendu aux utilisateurs.

Par exemple, une hausse du taux d'échec des simulations est généralement plus importante qu'une variation CPU isolée, sauf si cette variation constitue un risque imminent.

### 10.2. Définir un responsable

Chaque alerte importante doit être associée à une équipe responsable et à une procédure de diagnostic.

### 10.3. Ajouter du contexte

Inclure autant que possible :

- Le nom du service.
- L'environnement concerné.
- La métrique en cause.
- La valeur observée.
- La durée de l'anomalie.
- Un lien vers le tableau de bord.
- Un lien vers la procédure d'intervention.

### 10.4. Tester les alertes

Vérifier que :

- La règle se déclenche dans les conditions prévues.
- La notification atteint les bons destinataires.
- Les alertes répétées sont regroupées.
- L'alerte est résolue lorsque le problème disparaît.
- Les informations envoyées ne contiennent pas de données sensibles.

### 10.5. Sécuriser la chaîne d'alerting

Protéger les endpoints de gestion, les canaux de notification et les identifiants utilisés pour envoyer des messages.

Éviter d'insérer des données personnelles, des jetons ou des informations confidentielles dans les annotations des alertes.

## 11. Monitoring et Alerting : comparaison

| Monitoring | Alerting |
|---|---|
| Collecte et visualise les mesures | Détecte une condition nécessitant une attention |
| Permet d'analyser les tendances | Déclenche une notification |
| S'appuie sur des tableaux de bord | S'appuie sur des règles et des canaux de notification |
| Aide à comprendre le comportement du système | Aide à réagir rapidement aux incidents |

Les deux mécanismes sont indispensables à une démarche d'observabilité opérationnelle.

## 12. À retenir

L'Alerting transforme les données de supervision en notifications exploitables. Il aide les équipes à détecter rapidement les incidents, à prioriser les actions et à réduire le temps de rétablissement.

Dans une architecture Spring Boot, Actuator, Micrometer, Prometheus et Alertmanager peuvent constituer une chaîne complète de détection et de notification.

**Principe clé :** une bonne alerte ne se contente pas de signaler une anomalie ; elle indique un problème qui mérite une action et fournit les informations nécessaires pour commencer le diagnostic.