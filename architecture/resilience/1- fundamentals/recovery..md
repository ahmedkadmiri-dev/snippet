# Recovery — Récupération après une défaillance

## 1. Définition

Le **Recovery** désigne l'ensemble des mécanismes et procédures permettant à un système de retrouver un fonctionnement normal après une défaillance.

Dans une architecture distribuée, un incident peut affecter une API partenaire, une base de données, un service d'authentification ou une infrastructure réseau.

La récupération consiste à détecter la fin de l'incident, rétablir les traitements concernés et, si nécessaire, restaurer les données ou les opérations interrompues.

Le recovery est une composante importante de la résilience : il ne suffit pas de limiter l'impact d'une panne, il faut également pouvoir revenir à un état opérationnel cohérent.

## 2. Pourquoi le recovery est-il nécessaire ?

Prenons une application Spring Boot qui appelle un service partenaire pour calculer une tarification.

```text
Application Spring Boot
          |
          v
   API partenaire
          |
          X
   Service indisponible
```

Lorsque le partenaire devient indisponible, l'application peut :

- interrompre les appels après expiration des timeouts ;
- ouvrir le circuit breaker ;
- retourner une erreur contrôlée ou une réponse dégradée autorisée ;
- continuer à traiter les opérations qui ne dépendent pas de ce partenaire.

Lorsque le partenaire redevient disponible, il faut reprendre les appels sans provoquer une nouvelle surcharge et traiter les éventuelles opérations restées incomplètes.

Sans mécanisme de récupération adapté, le service peut rester dégradé même après la disparition de la cause initiale.

## 3. Les principales étapes du recovery

### 3.1. Détection de la défaillance

Le système identifie une anomalie à partir de signaux tels que :

- des timeouts répétés ;
- des erreurs HTTP `5xx` ;
- des échecs de connexion ;
- une hausse importante de la latence ;
- des contrôles de santé négatifs.

La détection peut être effectuée par l'application, la plateforme d'orchestration ou les outils de supervision.

### 3.2. Confinement de l'incident

Le système limite les conséquences de la panne afin d'éviter sa propagation.

Les mécanismes possibles comprennent :

- Circuit Breaker ;
- Bulkhead ;
- Rate Limiter ;
- Timeout ;
- Fallback.

Le confinement protège les autres composants pendant que le service défaillant est indisponible.

### 3.3. Détection du rétablissement

Le système vérifie si le composant peut à nouveau recevoir des requêtes.

Cette vérification peut reposer sur :

- un health check ;
- des sondes de disponibilité ;
- le passage du circuit breaker en mode semi-ouvert ;
- des appels de test contrôlés.

Une seule réponse positive ne garantit pas toujours que le service a retrouvé sa capacité nominale. La reprise doit tenir compte de la stabilité du composant.

### 3.4. Reprise progressive

Après le rétablissement, les appels peuvent reprendre progressivement.

Il faut éviter que toutes les requêtes accumulées soient envoyées simultanément au service rétabli.

Selon l'architecture, cette reprise peut s'appuyer sur :

- une limitation du trafic ;
- un backoff ;
- une file de messages ;
- une remise en traitement contrôlée ;
- un circuit breaker qui autorise progressivement de nouveaux appels.

### 3.5. Réconciliation des opérations

Certaines opérations peuvent avoir été interrompues alors que leur résultat est incertain.

Par exemple, une application envoie une demande de création à un partenaire, puis subit un timeout. Le partenaire peut avoir effectué la création sans que l'application ait reçu la réponse.

Il est alors nécessaire de déterminer si l'opération a réellement été exécutée avant de la relancer.

Les solutions possibles incluent :

- une clé d'idempotence ;
- la consultation du statut de l'opération ;
- un mécanisme de rapprochement ;
- une procédure de compensation métier.

## 4. Les principaux mécanismes de récupération

| Mécanisme | Rôle |
|---|---|
| Health Check | Détecte si un composant est opérationnel. |
| Circuit Breaker | Permet de suspendre les appels et de tester ensuite la disponibilité du service. |
| Retry avec Backoff | Retente une opération en espaçant les tentatives. |
| Failover | Bascule vers une ressource de secours. |
| Queue et Redelivery | Permettent de conserver et de retraiter des messages selon une politique définie. |
| Idempotency | Réduit le risque de produire plusieurs fois les mêmes effets métier. |
| Compensation | Corrige les effets d'une opération déjà exécutée lorsqu'une transaction distribuée échoue. |
| Backup et Restore | Permettent de restaurer des données après une perte ou une corruption. |

Ces mécanismes répondent à des situations différentes. Par exemple, un retry peut suffire pour une erreur réseau temporaire, mais il ne remplace pas une restauration de base de données après une corruption.

## 5. Recovery et Circuit Breaker

Le circuit breaker est un mécanisme utile pour contrôler la reprise des appels après une défaillance.

Il possède généralement trois états :

```text
        Échecs répétés
CLOSED -----------------> OPEN
  ^                         |
  |                         | Délai écoulé
  |                         v
  +--------------------- HALF_OPEN
      Appels de test réussis
```

- **CLOSED** : les appels sont autorisés normalement.
- **OPEN** : les appels sont bloqués temporairement.
- **HALF_OPEN** : un nombre limité d'appels permet de vérifier si le service est rétabli.

Si les appels de test réussissent, le circuit revient à l'état `CLOSED`. Si les échecs persistent, il repasse à l'état `OPEN`.

Le circuit breaker ne restaure pas le service distant. Il protège l'application appelante et contribue à une reprise contrôlée.

## 6. Exemple avec Spring Boot et Resilience4j

### 6.1. Client OpenFeign

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

### 6.2. Service protégé

```java
@Service
@RequiredArgsConstructor
public class PartnerService {

    private final PartnerClient partnerClient;

    @CircuitBreaker(name = "partnerApi")
    public PartnerResponse getStatus() {
        return partnerClient.getStatus();
    }
}
```

### 6.3. Configuration

```yaml
resilience4j:
  circuitbreaker:
    instances:
      partnerApi:
        slidingWindowType: COUNT_BASED
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        failureRateThreshold: 50
        waitDurationInOpenState: 15s
        permittedNumberOfCallsInHalfOpenState: 3
```

Dans cette configuration :

- le circuit analyse une fenêtre de dix appels ;
- l'évaluation du taux d'échec commence après au moins cinq appels ;
- il peut s'ouvrir à partir d'un taux d'échec de 50 % ;
- après quinze secondes, il peut autoriser trois appels de test en mode semi-ouvert.

Les seuils doivent être adaptés au trafic, aux délais attendus et au comportement du partenaire.

Attention : le circuit breaker n'ajoute pas automatiquement un retry, ne garantit pas la réussite des appels de test et ne restaure pas les opérations métier interrompues. Ces responsabilités doivent être traitées séparément.

## 7. Recovery dans les architectures distribuées

La récupération dépend du type de composant affecté.

### 7.1. API partenaire

Pour une API distante, on peut combiner timeout, circuit breaker, retry limité et reprise progressive.

### 7.2. Base de données

Pour une base de données, la récupération peut nécessiter le rétablissement des connexions, la vérification de la cohérence des données et, dans les cas graves, une restauration à partir d'une sauvegarde.

### 7.3. Traitement asynchrone

Pour un traitement basé sur des messages, il faut définir une politique de redelivery, un nombre maximal de tentatives et, éventuellement, une dead-letter queue pour les messages qui échouent durablement.

### 7.4. Transaction distribuée

Lorsqu'une opération implique plusieurs services, il faut prévoir comment traiter les étapes déjà exécutées si une étape ultérieure échoue.

Le pattern Saga peut utiliser des opérations de compensation pour ramener le système vers un état métier acceptable.

## 8. Bonnes pratiques

- Définir les scénarios de panne et les procédures de récupération.
- Mesurer le temps nécessaire au rétablissement.
- Éviter les reprises massives qui peuvent surcharger un service rétabli.
- Limiter les retries et appliquer un backoff adapté.
- Utiliser l'idempotence pour sécuriser la répétition des opérations.
- Prévoir une stratégie de redelivery pour les traitements asynchrones.
- Vérifier la cohérence métier après une interruption.
- Tester les scénarios de perte de connexion, de redémarrage et de reprise.
- Documenter les procédures de restauration et de failover.
- Surveiller les erreurs qui persistent après le rétablissement apparent du service.

## 9. Indicateurs de récupération

Deux indicateurs sont particulièrement utiles :

- **MTTR (Mean Time To Recovery)** : durée moyenne nécessaire pour rétablir le service après un incident, selon la définition retenue par l'organisation.
- **Taux de récupération réussie** : proportion des opérations ou traitements interrompus qui sont finalement récupérés avec succès.

On peut également suivre le nombre de messages en attente, les échecs de redelivery, le temps de retour à l'état normal et le nombre d'opérations nécessitant une intervention manuelle.

## 10. À retenir

Le **Recovery** couvre les mécanismes permettant de retrouver un fonctionnement normal après une défaillance.

Il comprend la détection du rétablissement, la reprise contrôlée des appels, la réconciliation des opérations et, selon les cas, la restauration des données.

Une stratégie de récupération efficace complète les mécanismes de prévention et de confinement pour construire une architecture réellement résiliente.
