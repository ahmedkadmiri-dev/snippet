# Fault Tolerance — Tolérance aux pannes

## 1. Définition

La **Fault Tolerance** (tolérance aux pannes) désigne la capacité d'un système à continuer à fournir un service, éventuellement avec des fonctionnalités réduites, malgré la défaillance d'un ou de plusieurs de ses composants.

Dans une architecture distribuée, une application dépend souvent de plusieurs composants : bases de données, API partenaires, services d'authentification, brokers de messages ou services externes.

La défaillance d'un composant ne doit pas nécessairement entraîner l'arrêt de toute l'application.

L'objectif de la tolérance aux pannes est de limiter l'impact de ces défaillances sur le service global.

## 2. Pourquoi la tolérance aux pannes est-elle nécessaire ?

Un système distribué peut rencontrer différents types de défaillances :

- Un service partenaire devient indisponible.
- Une API met trop de temps à répondre.
- Une base de données refuse temporairement les connexions.
- Un réseau subit une interruption.
- Un composant retourne des erreurs HTTP `5xx`.
- Une surcharge empêche un service de traiter toutes les requêtes.
- Un composant redémarre pendant le traitement d'une opération.

Sans mécanisme de protection, ces incidents peuvent se propager aux autres composants et provoquer une indisponibilité plus large.

### Exemple

Une application de simulation financière appelle trois services :

```text
Client
  |
  v
API Simulation
  |
  +----> Service Client
  |
  +----> Service Tarification
  |
  +----> Service Assurance
```

Si le service d'assurance devient indisponible, plusieurs comportements sont possibles :

- toute la simulation échoue ;
- la simulation continue sans les informations d'assurance, si le métier l'autorise ;
- une réponse alternative est fournie ;
- la requête est mise en attente ou retentée, selon le contexte.

Le choix dépend des exigences métier et du niveau de tolérance aux pannes attendu.

## 3. Les principaux mécanismes

La tolérance aux pannes repose généralement sur plusieurs mécanismes complémentaires.

### 3.1. Timeout

Un timeout impose une durée maximale d'attente pour une opération.

Il évite qu'un thread reste bloqué trop longtemps sur un service lent.

Exemple :

```text
Appel partenaire
      |
      v
Attente de la réponse
      |
      +---- Réponse reçue dans le délai ---> Continuer
      |
      +---- Délai dépassé -----------------> Échec contrôlé
```

Un timeout ne garantit pas que l'opération distante a été annulée. Le serveur distant peut continuer à traiter la requête après l'expiration du délai côté client.

### 3.2. Retry

Le retry permet de réessayer une opération après un échec potentiellement temporaire.

Il peut être utile lorsqu'une erreur résulte d'une interruption réseau momentanée ou d'une indisponibilité passagère.

Cependant, toutes les erreurs ne doivent pas être retentées. Une erreur métier ou une requête invalide ne sera généralement pas corrigée par une nouvelle tentative.

### 3.3. Circuit Breaker

Le circuit breaker surveille les échecs d'un appel distant et peut interrompre temporairement les appels lorsque le service devient trop défaillant.

Il permet d'éviter de continuer à solliciter inutilement un service indisponible.

### 3.4. Fallback

Le fallback fournit un comportement alternatif lorsqu'une opération échoue.

Cet alternative peut être :

- une réponse fonctionnelle dégradée ;
- une valeur de remplacement explicitement autorisée ;
- un message indiquant que le service est temporairement indisponible.

Un fallback ne doit jamais masquer une erreur en produisant une donnée métier fausse ou trompeuse.

### 3.5. Isolation des ressources

Les mécanismes d'isolation, comme le bulkhead, limitent les ressources qu'un composant peut consommer.

Ils empêchent qu'un service lent monopolise toutes les ressources nécessaires au fonctionnement des autres traitements.

### 3.6. Redondance et failover

La redondance consiste à disposer de plusieurs instances ou ressources capables d'assurer une fonction similaire.

Le failover permet de basculer vers une ressource de secours lorsqu'une ressource principale devient indisponible.

Ces mécanismes concernent souvent l'architecture et l'infrastructure, tandis que les timeouts, retries et circuit breakers protègent principalement les interactions entre composants.

## 4. Fault Tolerance et Resilience : quelle différence ?

Ces deux notions sont liées, mais ne sont pas strictement identiques.

| Notion | Objectif |
|---|---|
| Fault Tolerance | Continuer à assurer un service malgré une panne. |
| Resilience | Absorber les incidents, limiter leurs conséquences et retrouver un fonctionnement normal. |
| High Availability | Réduire les interruptions et maintenir le service accessible. |
| Recovery | Restaurer le fonctionnement après une défaillance. |

La tolérance aux pannes est donc l'une des dimensions d'une architecture résiliente.

## 5. Exemple avec Spring Boot et Resilience4j

Considérons un appel vers un service partenaire effectué avec OpenFeign.

Le principe est de protéger cet appel par un circuit breaker et de définir un fallback adapté au métier.

### 5.1. Client OpenFeign

```java
@FeignClient(
    name = "partnerClient",
    url = "${partner.api.url}"
)
public interface PartnerClient {

    @GetMapping("/api/partner/status")
    PartnerResponse getStatus();
}
```

### 5.2. Service protégé

```java
@Service
@RequiredArgsConstructor
public class PartnerService {

    private final PartnerClient partnerClient;

    @CircuitBreaker(
        name = "partnerService",
        fallbackMethod = "getStatusFallback"
    )
    public PartnerResponse getStatus() {
        return partnerClient.getStatus();
    }

    public PartnerResponse getStatusFallback(Throwable exception) {
        return PartnerResponse.unavailable();
    }
}
```

Dans cet exemple, `PartnerResponse.unavailable()` représente une méthode de construction d'une réponse alternative, à adapter au DTO réel.

La configuration du circuit breaker peut être définie dans `application.yml` :

```yaml
resilience4j:
  circuitbreaker:
    instances:
      partnerService:
        slidingWindowType: COUNT_BASED
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
```

Cette configuration signifie notamment :

- les statistiques sont calculées sur une fenêtre de dix appels ;
- au moins cinq appels sont nécessaires avant d'évaluer le taux d'échec ;
- le circuit peut s'ouvrir lorsque le taux d'échec atteint 50 % ;
- après dix secondes, le circuit peut passer en mode semi-ouvert pour tester si le service est rétabli.

Le circuit breaker doit être utilisé avec une configuration adaptée au trafic et aux exigences du service.

**Dépendances nécessaires :** le projet doit intégrer les modules Resilience4j compatibles avec sa version de Spring Boot, ainsi que Spring Cloud OpenFeign si ce dernier est utilisé.

## 6. Bonnes pratiques

- Définir des timeouts explicites pour les appels distants.
- Retenter uniquement les erreurs susceptibles d'être temporaires.
- Limiter le nombre de tentatives et utiliser un backoff adapté.
- Éviter les retries sur les opérations non idempotentes sans protection appropriée.
- Utiliser un circuit breaker pour limiter les appels vers un service défaillant.
- Isoler les ressources des dépendances critiques.
- Définir des fallbacks cohérents avec les règles métier.
- Surveiller les taux d'échec, les latences et les changements d'état des circuits.
- Tester les scénarios de panne plutôt que de tester uniquement le chemin nominal.

## 7. Limites et points d'attention

La tolérance aux pannes ne signifie pas qu'une application ne tombera jamais en panne.

Elle vise à maîtriser le comportement du système lorsqu'une défaillance survient.

Un mécanisme mal configuré peut aggraver un incident. Par exemple, des retries trop nombreux peuvent surcharger un service déjà en difficulté, tandis qu'un fallback incorrect peut retourner des résultats métier incohérents.

Il est donc nécessaire de combiner les mécanismes techniques avec des règles métier explicites, des tests et une observabilité adaptée.

## 8. À retenir

La **Fault Tolerance** consiste à concevoir un système capable de continuer à fonctionner malgré certaines défaillances.

Les principaux mécanismes comprennent les timeouts, les retries, les circuit breakers, les fallbacks, l'isolation des ressources et la redondance.

Le choix de ces mécanismes dépend de la nature de la panne, des exigences métier et du niveau de disponibilité attendu.
