# Monitoring — Surveillance et suivi d'un système

## 1. Définition

Le **Monitoring** est l'ensemble des mécanismes permettant de collecter, mesurer et analyser les informations relatives au fonctionnement d'une application et de son infrastructure.

Il permet de comprendre l'état du système, de détecter les anomalies et d'identifier les dégradations de performance avant qu'elles ne provoquent une indisponibilité majeure.

Dans une architecture de microservices Spring Boot, le monitoring permet notamment de suivre :

- Le taux de réussite et d'échec des requêtes HTTP.
- Le temps de réponse des API.
- Le nombre de requêtes traitées.
- La consommation CPU et mémoire.
- Le nombre de connexions aux bases de données.
- Les erreurs lors des appels entre microservices.
- L'état des mécanismes de résilience comme Retry, Circuit Breaker et Bulkhead.

Le monitoring ne sert donc pas uniquement à vérifier si une application est démarrée. Il permet aussi de comprendre comment elle se comporte en fonctionnement réel.

## 2. Pourquoi le Monitoring est-il indispensable ?

Prenons une architecture composée d'une Gateway, d'un moteur financier et d'un service de tarification.

```text id="y1e9g8"
Clients
   |
   v
Selfcare Gateway
   |
   v
Moteur financier
   |
   v
Service de tarification
   |
   v
Base de données
```

Supposons que le temps de réponse du moteur financier passe progressivement de 200 ms à 5 secondes.

Sans monitoring, le problème peut être découvert uniquement lorsque les utilisateurs signalent des lenteurs.

Avec un monitoring adapté, il devient possible de détecter :

- Une augmentation du temps de réponse.
- Une hausse du taux d'erreur.
- Une saturation du pool de connexions.
- Une augmentation des appels vers le service de tarification.
- Une accumulation des requêtes en attente.

Le monitoring aide ainsi à détecter les problèmes, à en rechercher les causes et à vérifier l'efficacité des actions correctives.

## 3. Les trois piliers de l'observabilité

Le monitoring s'inscrit dans une démarche plus large appelée **observabilité**.

Trois catégories de données sont généralement utilisées.

### 3.1. Les métriques

Les métriques sont des mesures numériques collectées dans le temps.

Exemples :

- Nombre de requêtes HTTP par seconde.
- Pourcentage de réponses HTTP 5xx.
- Temps de réponse médian et percentile 95.
- Consommation mémoire.
- Nombre de connexions actives à la base de données.
- Nombre d'appels refusés par un Bulkhead.

Elles sont particulièrement utiles pour détecter les tendances, suivre les performances et déclencher des alertes.

### 3.2. Les logs

Les logs enregistrent les événements produits par l'application.

Exemple :

```text id="51mkxa"
2026-10-28 10:15:21 INFO  SimulationService - Début du calcul
2026-10-28 10:15:22 WARN  TarificationClient - Timeout partenaire
2026-10-28 10:15:23 INFO  Retry - Nouvelle tentative
2026-10-28 10:15:24 ERROR TarificationClient - Appel impossible
```

Les logs permettent d'examiner le détail d'un événement et de comprendre ce qui s'est passé.

Ils doivent contenir suffisamment de contexte pour faciliter l'analyse, sans exposer de données sensibles.

### 3.3. Les traces distribuées

Une trace distribuée représente le parcours d'une requête à travers plusieurs composants.

Exemple :

```text id="vkwu5s"
Requête HTTP
    |
    +-- Gateway : 50 ms
    |
    +-- Moteur financier : 300 ms
    |
    +-- Tarification : 2 500 ms
    |
    +-- Base de données : 80 ms
```

Les traces permettent de localiser les opérations qui contribuent le plus à la latence totale.

Elles sont particulièrement utiles lorsque plusieurs microservices interviennent dans une même opération métier.

## 4. Les indicateurs essentiels

Pour surveiller un système distribué, quatre grandes catégories d'indicateurs sont particulièrement utiles.

### 4.1. Latence

La latence mesure le temps nécessaire pour répondre à une requête.

Il ne faut pas se limiter à la moyenne : quelques requêtes très lentes peuvent être masquées par une moyenne satisfaisante.

Les percentiles permettent d'observer différentes parties de la distribution :

- **P50** : la moitié des requêtes répond plus vite que cette valeur.
- **P95** : 95 % des requêtes répondent en moins de ce délai.
- **P99** : 99 % des requêtes répondent en moins de ce délai.

Exemple :

| Indicateur | Valeur |
|---|---:|
| P50 | 150 ms |
| P95 | 800 ms |
| P99 | 2 500 ms |

Dans cet exemple, la majorité des requêtes est rapide, mais une minorité présente des temps de réponse nettement plus élevés.

### 4.2. Trafic

Le trafic représente le volume de requêtes traitées.

Exemples :

- Requêtes HTTP par seconde.
- Messages consommés par seconde.
- Nombre de simulations créées par minute.
- Nombre d'appels vers un partenaire.

Une hausse du trafic peut expliquer une dégradation des performances, mais elle n'est pas nécessairement un problème si le système dispose de capacités suffisantes.

### 4.3. Erreurs

Le taux d'erreur mesure la proportion d'opérations qui échouent.

Exemples :

- Réponses HTTP 5xx.
- Timeouts vers les services externes.
- Erreurs SQL.
- Échecs de traitement des messages.
- Rejets du Circuit Breaker.

Il est important de distinguer les erreurs techniques des erreurs métier attendues. Une réponse HTTP 400 liée à une saisie invalide ne doit pas nécessairement être interprétée comme une panne de l'application.

### 4.4. Saturation

La saturation indique si une ressource approche de sa capacité maximale.

Exemples :

- CPU proche de sa limite.
- Mémoire insuffisante.
- Pool de connexions JDBC saturé.
- File de tâches presque pleine.
- Nombre maximal d'appels concurrents atteint.

La saturation est particulièrement importante pour anticiper les dégradations avant que les erreurs ne deviennent massives.

## 5. Monitoring dans Spring Boot avec Actuator

Spring Boot Actuator fournit des endpoints permettant d'exposer des informations sur l'application.

### 5.1. Ajouter la dépendance

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

Actuator expose notamment des informations de santé et des métriques.

### 5.2. Configurer les endpoints

Exemple de configuration dans `application.yml` :

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus

  endpoint:
    health:
      show-details: never

  metrics:
    tags:
      application: moteur-financier
```

L'exposition de `prometheus` nécessite également la dépendance Micrometer Prometheus appropriée, généralement `micrometer-registry-prometheus`.

Il est recommandé de n'exposer que les endpoints nécessaires et de protéger les endpoints de gestion en environnement de production.

### 5.3. Consulter les métriques

Exemples d'endpoints :

```text
/actuator/health
/actuator/metrics
/actuator/metrics/jvm.memory.used
/actuator/metrics/http.server.requests
/actuator/prometheus
```

Les métriques disponibles dépendent des bibliothèques utilisées et de la configuration.

L'endpoint `metrics` permet de consulter des mesures individuelles, tandis que l'endpoint `prometheus` expose les données dans un format exploitable par Prometheus.

## 6. Prometheus et Grafana

Un ensemble courant pour le monitoring des applications Spring Boot repose sur Prometheus et Grafana.

<box gap={3}>
  <row align=start gap={3}>
    <AsyncImage query="Prometheus monitoring system official logo" aspectRatio="1:1" maxWidth="84px" />
    <box flex="1" gap={1}>
      **Prometheus**

      Collecte les métriques exposées par les applications et les conserve sous forme de séries temporelles. Il peut également évaluer des règles d'alerte.
    </box>
  </row>
  <divider color="subtle" />
  <row align=start gap={3}>
    <AsyncImage query="Grafana observability official logo" aspectRatio="1:1" maxWidth="84px" />
    <box flex="1" gap={1}>
      **Grafana**

      Permet de créer des tableaux de bord pour visualiser les métriques, comparer les périodes et suivre les performances.
    </box>
  </row>
</box>

### 6.1. Architecture générale

```text id="54g0r8"
Application Spring Boot
        |
        v
Spring Boot Actuator
        |
        v
Endpoint Prometheus
        |
        v
Prometheus
        |
        v
Grafana
        |
        v
Tableaux de bord
```

Prometheus interroge périodiquement l'endpoint exposé par l'application. Grafana utilise ensuite les données collectées pour afficher les graphiques.

Les logs et les traces peuvent être collectés par des outils complémentaires, par exemple Loki pour les logs ou Tempo pour les traces dans l'écosystème Grafana.

## 7. Monitoring des mécanismes Resilience4j

Dans une application utilisant Resilience4j, il est important de surveiller le comportement des mécanismes de résilience.

### 7.1. Circuit Breaker

Indicateurs utiles :

- État du Circuit Breaker.
- Nombre d'appels réussis.
- Nombre d'appels en échec.
- Taux d'échec.
- Nombre de transitions entre les états.

Ces données permettent de comprendre si un service distant est instable et si le Circuit Breaker intervient fréquemment.

### 7.2. Retry

Indicateurs utiles :

- Nombre total de tentatives.
- Nombre de nouvelles tentatives.
- Taux d'échec après toutes les tentatives.
- Temps supplémentaire provoqué par les reprises.

Un taux élevé de retries peut indiquer une dépendance instable ou une configuration inadaptée.

### 7.3. Bulkhead

Indicateurs utiles :

- Nombre d'appels concurrents.
- Nombre de places disponibles.
- Nombre d'appels rejetés.

Ces informations permettent de vérifier si la limite de concurrence est adaptée à la charge réelle.

### 7.4. TimeLimiter et Rate Limiter

Pour le TimeLimiter, surveiller notamment les expirations et les opérations dépassant le budget de temps prévu.

Pour le Rate Limiter, surveiller le nombre de demandes acceptées et refusées.

**Attention :** les métriques Resilience4j doivent être intégrées aux mécanismes de métriques de l'application. Leur disponibilité dépend des dépendances et de la configuration du projet.

## 8. Exemple d'architecture de monitoring pour des microservices

```text
                    +----------------+
                    |     Grafana    |
                    +--------+-------+
                             |
                             v
                    +----------------+
                    |   Prometheus   |
                    +--------+-------+
                             |
               +-------------+-------------+
               |             |             |
               v             v             v
         +-----------+ +-----------+ +-----------+
         | Gateway   | | Moteur    | | Service   |
         | Actuator  | | financier | | partenaire|
         +-----------+ +-----------+ +-----------+
```

Chaque service expose ses métriques. Prometheus les collecte et Grafana permet de visualiser l'état global de l'architecture.

Pour une observabilité plus complète, cette architecture peut être enrichie avec :

- Un système centralisé de logs.
- Un collecteur de traces distribuées.
- Des alertes.
- Des tableaux de bord métier.
- Des corrélations entre les erreurs, la latence et les événements techniques.

## 9. Bonnes pratiques

### 9.1. Choisir des métriques utiles

Éviter de collecter des milliers de métriques sans objectif précis.

Commencer par les indicateurs qui permettent de répondre à des questions concrètes :

- L'API répond-elle dans les délais attendus ?
- Quel service génère le plus d'erreurs ?
- La base de données est-elle saturée ?
- Le nombre de retries augmente-t-il ?
- Une file d'attente grandit-elle continuellement ?

### 9.2. Éviter les labels à forte cardinalité

Dans Prometheus, un label représente une dimension permettant de filtrer les séries temporelles.

Des labels comme `application`, `environment` ou `endpoint` peuvent être utiles.

En revanche, utiliser un identifiant de simulation, un identifiant client ou un identifiant de requête comme label peut créer un nombre très élevé de séries temporelles.

Cela augmente la consommation mémoire et le coût de stockage.

Les identifiants de corrélation doivent plutôt être conservés dans les logs ou les traces.

### 9.3. Corréler les métriques, les logs et les traces

Une métrique indique qu'un problème existe. Un log fournit des détails sur un événement. Une trace permet de localiser l'opération lente dans le parcours d'une requête.

Leur combinaison facilite le diagnostic.

### 9.4. Surveiller les indicateurs métier

Dans un moteur financier, les indicateurs techniques ne suffisent pas.

Il peut être utile de suivre :

- Le nombre de simulations lancées.
- Le nombre de simulations en échec.
- Le taux de transformation des simulations.
- Le temps de traitement moyen d'une simulation.
- Le nombre d'appels au service de tarification.

Ces indicateurs doivent être définis avec les équipes métier et respecter les exigences de confidentialité.

### 9.5. Définir des seuils et des objectifs

Les tableaux de bord sont plus utiles lorsqu'ils s'appuient sur des objectifs explicites.

Par exemple :

- Un objectif de latence P95.
- Un taux d'erreur maximal acceptable.
- Un taux de disponibilité cible.
- Un délai maximal de traitement des messages.

Ces objectifs peuvent être formalisés sous forme de SLI (*Service Level Indicators*) et de SLO (*Service Level Objectives*).

## 10. Monitoring et alerting : différence

Le monitoring collecte et présente les informations.

L'alerting déclenche une notification lorsqu'une condition importante est remplie.

Exemple :

```text id="y68y4u"
Métriques collectées
        |
        v
Détection d'une anomalie
        |
        v
Règle d'alerte
        |
        v
Notification à l'équipe
        |
        v
Diagnostic et correction
```

Un tableau de bord permet de consulter l'état du système. Une alerte permet d'attirer l'attention lorsqu'une intervention est nécessaire.

Les deux mécanismes sont complémentaires.

## 11. À retenir

Le Monitoring permet de mesurer le comportement réel d'une application, de détecter les anomalies et de faciliter le diagnostic.

Dans une architecture Spring Boot, Actuator, Micrometer, Prometheus et Grafana constituent une base solide pour mettre en place une supervision technique.

**Principe clé :** on ne peut pas maîtriser durablement la résilience d'un système si l'on ne mesure pas sa latence, ses erreurs, son trafic et sa saturation.