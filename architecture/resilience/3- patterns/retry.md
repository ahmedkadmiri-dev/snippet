# Retry — Nouvelle tentative après un échec

## 1. Définition

Le **Retry** est un pattern de résilience qui consiste à réexécuter une opération lorsqu'elle échoue, dans l'espoir que l'erreur soit temporaire.

Dans une architecture distribuée, un appel peut échouer à cause d'une interruption réseau, d'une indisponibilité momentanée d'un service distant ou d'une limitation temporaire du nombre de requêtes.

Le retry permet de retenter l'opération sans obliger l'utilisateur à relancer manuellement sa demande.

Il doit cependant être utilisé avec précaution : répéter une opération qui échoue systématiquement peut augmenter la charge et aggraver l'incident.

## 2. Pourquoi utiliser le Retry ?

Prenons une application Spring Boot qui appelle une API partenaire pour obtenir une tarification.

```text
Application Spring Boot
          |
          v
     API partenaire
          |
          X
   Échec temporaire
          |
          v
     Nouvelle tentative
          |
          v
     Réponse reçue
```

Si le premier appel échoue à cause d'une interruption réseau momentanée, une nouvelle tentative peut réussir.

Sans retry, l'application retourne immédiatement une erreur à l'utilisateur, même si le problème aurait pu être résolu en quelques instants.

Le retry est particulièrement utile lorsque :

- l'erreur est potentiellement temporaire ;
- le service distant peut redevenir disponible rapidement ;
- l'opération peut être répétée sans produire d'effets métier indésirables ;
- le nombre de tentatives et les délais sont maîtrisés.

## 3. Les principales stratégies de Retry

### 3.1. Retry immédiat

L'opération est réexécutée immédiatement après l'échec.

```text
Tentative 1 : échec
       |
       v
Tentative 2 : échec
       |
       v
Tentative 3 : succès
```

Cette stratégie peut être utile pour des erreurs très brèves, mais elle risque de surcharger un service qui rencontre déjà des difficultés.

### 3.2. Retry avec délai fixe

Un délai constant est appliqué entre les tentatives.

Exemple avec un délai de deux secondes :

```text
Tentative 1
    |
  Échec
    |
 Attente 2 s
    |
Tentative 2
    |
  Échec
    |
 Attente 2 s
    |
Tentative 3
```

Cette stratégie est simple à configurer, mais plusieurs clients peuvent continuer à envoyer leurs nouvelles tentatives au même moment.

### 3.3. Retry avec Backoff exponentiel

Le délai augmente après chaque échec.

Par exemple :

| Tentative suivante | Délai d'attente |
|---|---:|
| 2 | 1 seconde |
| 3 | 2 secondes |
| 4 | 4 secondes |
| 5 | 8 secondes |

Le délai réel dépend de la configuration, du plafond défini et éventuellement d'une composante aléatoire.

Le backoff exponentiel réduit la pression exercée sur un service temporairement indisponible.

### 3.4. Retry avec jitter

Le **jitter** ajoute une variation aléatoire au délai entre les tentatives.

Cette variation évite que de nombreux clients réessaient exactement au même instant.

Il est particulièrement utile lorsque plusieurs instances d'une application subissent simultanément une panne d'un même service partenaire.

## 4. Quelles erreurs faut-il retenter ?

Toutes les erreurs ne justifient pas une nouvelle tentative.

| Type d'erreur | Retry recommandé ? | Explication |
|---|---|---|
| Interruption réseau temporaire | Souvent | Le problème peut disparaître rapidement. |
| Timeout | Parfois | Le résultat de l'opération peut être incertain. |
| HTTP `429 Too Many Requests` | Selon le contrat | Respecter `Retry-After` lorsqu'il est fourni et appliquer une politique de limitation. |
| HTTP `502`, `503`, `504` | Souvent, avec limites | L'indisponibilité peut être temporaire. |
| HTTP `400 Bad Request` | Généralement non | La requête doit être corrigée. |
| HTTP `401 Unauthorized` | Généralement non | Il faut résoudre le problème d'authentification avant de réessayer. |
| HTTP `403 Forbidden` | Généralement non | Répéter la requête ne corrige pas les droits manquants. |
| Erreur métier définitive | Non | Une nouvelle tentative ne change normalement pas le résultat. |

La stratégie exacte doit respecter le contrat du service distant. Certaines erreurs `5xx` peuvent être définitives, tandis que certains échecs de connexion peuvent indiquer un problème durable.

## 5. Retry et idempotence

L'idempotence est un point essentiel lorsque l'on applique le retry.

Une opération est idempotente si son exécution plusieurs fois produit le même effet métier que son exécution une seule fois.

Considérons une API qui crée un dossier de financement.

```text
Application
    |
    v
POST /dossiers
    |
    v
Dossier créé
    |
    X
Réponse perdue
    |
    v
Nouvelle tentative
    |
    v
Deuxième dossier créé ?
```

Le timeout peut survenir après que le serveur a créé le dossier, mais avant que la réponse soit reçue.

Sans mécanisme de protection, la nouvelle tentative risque de créer un deuxième dossier.

Les solutions possibles comprennent :

- utiliser une clé d'idempotence ;
- vérifier l'existence de l'opération avant de la relancer ;
- utiliser un identifiant métier unique ;
- consulter le statut de la transaction avant de recommencer.

Le retry ne garantit donc pas à lui seul qu'une opération sera exécutée une seule fois.

## 6. Retry et Circuit Breaker : quelle différence ?

| Retry | Circuit Breaker |
|---|---|
| Réessaie une opération après un échec. | Bloque temporairement les appels vers un service défaillant. |
| Cherche à récupérer une erreur potentiellement temporaire. | Cherche à limiter la propagation d'une panne. |
| Utilise un nombre de tentatives et des délais. | Utilise des statistiques, des seuils et des états. |
| Peut augmenter la charge du service distant. | Réduit les appels lorsque le service semble défaillant. |

Les deux mécanismes peuvent être combinés, mais leur ordre et leur configuration doivent être réfléchis.

Un nombre excessif de retries peut faire échouer le circuit breaker plus rapidement ou prolonger la durée de traitement d'une requête.

## 7. Implémentation avec Spring Boot et Resilience4j

### 7.1. Client OpenFeign

```java
@FeignClient(
    name = "partnerClient",
    url = "${partner.api.url}"
)
public interface PartnerFeignClient {

    @GetMapping("/api/partner/tarification")
    TarificationResponse getTarification();
}
```

### 7.2. Service avec Retry

```java
@Service
@RequiredArgsConstructor
public class PartnerService {

    private final PartnerFeignClient partnerFeignClient;

    @Retry(name = "partnerApi")
    public TarificationResponse getTarification() {
        return partnerFeignClient.getTarification();
    }
}
```

L'annotation `@Retry` permet à Resilience4j d'appliquer la politique de nouvelle tentative configurée.

### 7.3. Configuration dans `application.yml`

```yaml
resilience4j:
  retry:
    instances:
      partnerApi:
        maxAttempts: 3
        waitDuration: 1s
        enableExponentialBackoff: true
        exponentialBackoffMultiplier: 2
        retryExceptions:
          - feign.RetryableException
```

Dans cette configuration :

- `maxAttempts: 3` autorise trois tentatives au total, appel initial compris ;
- `waitDuration: 1s` définit le délai initial ;
- le backoff exponentiel multiplie le délai par deux après chaque échec ;
- `retryExceptions` limite les tentatives aux exceptions correspondant à la classe indiquée.

**Point d'attention :** cette configuration illustrative ne retentera pas automatiquement toutes les réponses HTTP `5xx`. Le comportement dépend notamment de la façon dont OpenFeign transforme les réponses et de la classe d'exception produite.

Pour retenter certaines réponses HTTP, il faut configurer explicitement la classification des exceptions et, si nécessaire, un `ErrorDecoder` ou une autre stratégie adaptée.

Les erreurs métier, les requêtes invalides et les opérations non idempotentes doivent être exclues ou protégées explicitement.

## 8. Retry avec Circuit Breaker

Dans une architecture distribuée, il est fréquent de combiner les deux mécanismes.

```text
Appel métier
     |
     v
Retry
     |
     v
Circuit Breaker
     |
     v
API partenaire
```

Cet ordre est illustratif : la disposition effective des aspects dépend de la configuration et de l'ordre d'exécution de Resilience4j.

Le principe est de permettre quelques nouvelles tentatives contrôlées tout en empêchant la poursuite des appels lorsque le service est trop défaillant.

Il faut définir :

- le nombre maximal de tentatives ;
- les délais entre les tentatives ;
- les erreurs qui justifient un retry ;
- les seuils du circuit breaker ;
- le timeout global de l'opération.

La durée cumulée des tentatives doit rester compatible avec le délai maximal acceptable pour la requête utilisateur.

## 9. Bonnes pratiques

- Limiter le nombre de tentatives.
- Utiliser un backoff lorsque le service peut être surchargé.
- Ajouter du jitter dans les scénarios à fort volume.
- Ne retenter que les erreurs potentiellement temporaires.
- Respecter les consignes `Retry-After` lorsqu'elles sont applicables.
- Protéger les opérations non