# Bulkhead — Isoler les ressources pour limiter les pannes

## 1. Définition

Le **Bulkhead** est un pattern de résilience qui consiste à isoler les ressources utilisées par différentes parties d'une application afin qu'une panne ou une surcharge dans un composant ne compromette pas l'ensemble du système.

Le terme vient de la construction navale : un navire est divisé en compartiments étanches pour éviter qu'une inondation dans un compartiment ne provoque le naufrage de tout le navire.

En architecture logicielle, le principe est similaire : chaque dépendance ou fonctionnalité dispose de ressources limitées et isolées.

Les ressources concernées peuvent être :
- Les threads.
- Les connexions HTTP.
- Les connexions à une base de données.
- Les tâches exécutées simultanément.
- Les files d'attente.

**Objectif principal :** empêcher une dépendance lente ou indisponible de consommer toutes les ressources nécessaires aux autres fonctionnalités.

## 2. Pourquoi utiliser un Bulkhead ?

Considérons une application `moteur-financier` qui appelle trois services :

```text
                    moteur-financier
                           |
             +-------------+-------------+
             |             |             |
             v             v             v
       Service A      Service B      Service C
       Simulation     Référentiel    Notification
```

Supposons que le service de notification devienne très lent.

Sans isolation, les appels de notification peuvent monopoliser les threads ou les connexions disponibles. Les appels vers les services de simulation et de référentiel risquent alors d'échouer, même si ces services fonctionnent correctement.

Avec un Bulkhead :

```text
                    moteur-financier
                           |
          +----------------+----------------+
          |                |                |
          v                v                v
    +-----------+    +-----------+    +-----------+
    | Bulkhead A|    | Bulkhead B|    | Bulkhead C|
    | 10 appels |    | 20 appels |    |  5 appels |
    +-----------+    +-----------+    +-----------+
          |                |                |
          v                v                v
     Simulation       Référentiel      Notification
```

Chaque dépendance possède sa propre capacité de traitement.

Si le Bulkhead C atteint sa limite, les appels de notification peuvent être refusés ou retardés selon la stratégie choisie, sans nécessairement empêcher les appels de simulation.

Les capacités indiquées dans cet exemple sont illustratives.

## 3. Les principaux types de Bulkhead

### 3.1. Isolation par pool de threads

Chaque dépendance utilise un pool de threads distinct.

Exemple :

```text
Pool Simulation    : 10 threads
Pool Référentiel   :  5 threads
Pool Notification  :  3 threads
```

Si les trois threads du pool de notification sont occupés par des appels bloqués, les pools des autres services restent disponibles.

Cette stratégie est pertinente lorsque les appels peuvent bloquer des threads.

**Avantages :**
- Isolation claire entre les dépendances.
- Protection contre l'épuisement global des threads.
- Capacité maximale de traitement contrôlée par pool.

**Limites :**
- Consommation mémoire et coût de gestion des threads.
- Risque de saturation propre à chaque pool.
- Nécessité de choisir des tailles adaptées.

### 3.2. Isolation par sémaphore

Un sémaphore limite le nombre d'appels simultanés sans créer nécessairement un pool de threads spécifique.

Exemple :

```text
Service de tarification
       |
       v
Sémaphore : maximum 10 appels simultanés
       |
       +-- Si une place est disponible : appel autorisé
       |
       +-- Si la limite est atteinte : appel refusé
```

Cette approche est utile pour contrôler la concurrence lorsque le modèle d'exécution existant doit être conservé.

**Avantages :**
- Faible coût par rapport à la création d'un pool dédié.
- Limitation simple des appels simultanés.
- Contrôle de la pression exercée sur une dépendance.

**Limites :**
- Ne fournit pas à lui seul un pool de threads isolé.
- Les appels bloquants peuvent toujours occuper les threads qui les exécutent.
- Il faut définir le comportement lorsque la limite est atteinte.

## 4. Implémentation avec Resilience4j et Spring Boot

Resilience4j propose deux stratégies principales pour le Bulkhead :
- `SemaphoreBulkhead`
- `FixedThreadPoolBulkhead`

Le choix dépend notamment du type d'appel, du modèle d'exécution et du besoin d'isolation.

### 4.1. Dépendance Maven

Pour une application Spring Boot utilisant Resilience4j :

```xml
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
</dependency>
```

La version doit être alignée sur la version de Spring Boot et, idéalement, gérée par le BOM Resilience4j du projet. Pour utiliser les annotations Spring, il faut également disposer des dépendances nécessaires à l'intégration AOP selon la configuration retenue.

### 4.2. Configuration du Bulkhead

Exemple dans `application.yml` :

```yaml
resilience4j:
  bulkhead:
    instances:
      tarification:
        maxConcurrentCalls: 10
        maxWaitDuration: 0
```

Dans cet exemple :
- `maxConcurrentCalls: 10` autorise au maximum dix appels simultanés.
- `maxWaitDuration: 0` signifie qu'aucun temps d'attente supplémentaire n'est accordé pour obtenir une place.

Lorsque la capacité est atteinte, un nouvel appel peut échouer immédiatement avec une exception de saturation, notamment `BulkheadFullException`.

### 4.3. Utilisation avec une annotation

```java
@Service
public class TarificationService {

    private final TarificationClient tarificationClient;

    public TarificationService(TarificationClient tarificationClient) {
        this.tarificationClient = tarificationClient;
    }

    @Bulkhead(name = "tarification", type = Bulkhead.Type.SEMAPHORE)
    public TarificationDto obtenirTarification(Long id) {
        return tarificationClient.getTarification(id);
    }
}
```

L'annotation limite le nombre d'exécutions simultanées de cette méthode via le Bulkhead configuré.

Points d'attention :
- L'interception par annotation nécessite une configuration Spring AOP adaptée.
- L'appel doit généralement passer par le proxy Spring ; un appel interne à une autre méthode de la même instance peut contourner l'interception.
- Un Bulkhead ne remplace pas un Timeout : un appel bloqué peut continuer à occuper une place jusqu'à sa fin ou jusqu'à ce que le mécanisme d'exécution libère la ressource.

### 4.4. Isolation par pool de threads

Pour les traitements adaptés à une exécution isolée dans un pool, Resilience4j propose également un Bulkhead de type `THREADPOOL`.

La configuration peut ressembler à ceci :

```yaml
resilience4j:
  thread-pool-bulkhead:
    instances:
      notification:
        coreThreadPoolSize: 3
        maxThreadPoolSize: 5
        queueCapacity: 10
```

Cette configuration définit un pool de notification avec un nombre limité de threads et une file d'attente bornée.

La capacité effective dépend du nombre de threads, de la taille de la file et du comportement du pool. Une file pleine peut conduire au rejet de nouvelles tâches.

**Important :** le Bulkhead de type `THREADPOOL` ne se comporte pas exactement comme le sémaphore. Il implique une exécution via un pool et s'utilise généralement avec des méthodes ou des API compatibles avec une exécution asynchrone. Il faut adapter le code et la configuration à la version de Resilience4j employée.

## 5. Bulkhead, Timeout et Circuit Breaker

Ces trois patterns sont complémentaires, mais ont des objectifs différents.

| Pattern | Rôle |
|---|---|
| Bulkhead | Limiter la consommation de ressources par une dépendance. |
| Timeout | Limiter la durée d'attente d'un appel. |
| Circuit Breaker | Bloquer temporairement les appels vers une dépendance qui échoue trop souvent. |

Exemple d'une combinaison :

```text
Requête entrante
       |
       v
Bulkhead
       |
       v
Circuit Breaker
       |
       v
Timeout
       |
       v
Service distant
```

Cette représentation est illustrative : l'ordre effectif des décorateurs doit être choisi en fonction du comportement attendu.

Par exemple, un Timeout peut empêcher une requête d'attendre trop longtemps, tandis que le Bulkhead limite le nombre d'appels simultanés. Le Circuit Breaker peut ensuite éviter de continuer à appeler une dépendance qui accumule les échecs.

Une configuration incorrecte peut néanmoins laisser des threads bloqués, augmenter les files d'attente ou provoquer des rejets excessifs.

## 6. Bonnes pratiques

### 6.1. Isoler les dépendances critiques

Il est pertinent d'isoler les services qui présentent des profils de latence ou de disponibilité différents.

Exemples :
- Service de tarification.
- Service de notification SMS.
- API externe.
- Service de génération de documents.
- Appels vers des systèmes bancaires tiers.

Un service de notification lent ne devrait pas empêcher une simulation financière d'être exécutée si cette notification n'est pas indispensable à la simulation.

### 6.2. Choisir des limites réalistes

Les limites doivent être déterminées à partir :
- Du nombre de requêtes simultanées attendu.
- De la latence du service distant.
- De la capacité réelle de ce service.
- Du nombre d'instances de l'application.
- Des limites de connexion et de threads disponibles.

Une limite trop élevée ne protège pas suffisamment la dépendance. Une limite trop faible provoque des rejets inutiles.

Il faut également tenir compte du fait que chaque instance de l'application peut appliquer ses propres limites : dix appels par instance ne signifient pas dix appels au total lorsque plusieurs instances sont déployées.

### 6.3. Éviter les files d'attente non bornées

Une file d'attente non bornée peut accumuler des tâches pendant une surcharge et provoquer une consommation excessive de mémoire.

Il est préférable de définir une capacité maximale et un comportement explicite en cas de saturation :
- Refuser rapidement la requête.
- Retourner une erreur contrôlée.
- Utiliser un Fallback si cela a du sens.
- Reporter le traitement dans un système asynchrone lorsque le métier le permet.

### 6.4. Combiner avec des Timeouts

Un Bulkhead limite le nombre d'appels simultanés, mais il ne garantit pas que chaque appel se termine rapidement.

Un appel distant bloqué peut monopoliser une place dans le Bulkhead.

Il est donc recommandé de définir des Timeouts adaptés aux dépendances et de vérifier que les ressources sont libérées correctement.

### 6.5. Surveiller la saturation

Les métriques utiles incluent :
- Le nombre d'appels actifs.
- La capacité maximale configurée.
- Le nombre de rejets pour saturation.
- La taille de la file d'attente.
- La durée des appels.
- Le taux de timeouts.

Ces indicateurs permettent de distinguer une dépendance lente d'une capacité de Bulkhead insuffisante.

## 7. Limites du Bulkhead

Le Bulkhead ne répare pas une dépendance indisponible et ne garantit pas la réussite des appels.

Il peut également introduire :
- Des rejets lorsque la capacité est atteinte.
- Une configuration plus complexe.
- Une consommation supplémentaire de ressources avec des pools dédiés.
- Une mauvaise répartition des capacités si les limites sont mal choisies.

Il doit être conçu avec les autres mécanismes de résilience et testé dans des conditions de charge réalistes.

## 8. À retenir

Le Bulkhead est un mécanisme d'isolation qui empêche une dépendance lente ou surchargée de monopoliser toutes les ressources de l'application.

Les points essentiels sont :

1. Isoler les ressources par dépendance ou fonctionnalité.
2. Limiter le nombre d'appels simultanés.
3. Choisir entre sémaphore et pool de threads selon le modèle d'exécution.
4. Combiner le Bulkhead avec des Timeouts et, si nécessaire, un Circuit Breaker.
5. Surveiller les rejets et ajuster les limites à partir de mesures réelles.

**Règle d'or :** une dépendance en difficulté ne doit pas pouvoir consommer toutes les ressources nécessaires au reste de l'application.