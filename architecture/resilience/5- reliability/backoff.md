# Backoff — Espacement progressif des tentatives

## 1. Définition

Le **Backoff** est une stratégie qui consiste à augmenter le délai entre les tentatives successives d'une opération qui a échoué.

Il est utilisé pour éviter qu'une application continue à solliciter un service indisponible ou surchargé à un rythme trop élevé.

Par exemple, lorsqu'un microservice appelle une API partenaire et reçoit une erreur temporaire, il peut attendre avant de réessayer :

- Première tentative : immédiatement.
- Première reprise : après 1 seconde.
- Deuxième reprise : après 2 secondes.
- Troisième reprise : après 4 secondes.

Cette approche permet de laisser du temps au service distant pour récupérer et réduit la pression exercée sur celui-ci.

Le Backoff est généralement associé au **Retry**, qui définit la possibilité de réessayer une opération après un échec.

## 2. Pourquoi utiliser le Backoff ?

Considérons une application Spring Boot qui appelle un service externe de tarification.

```text
Service Spring Boot
        |
        | Requête HTTP
        v
Service de tarification
        |
        | Erreur temporaire
        v
Retry immédiat
        |
        v
Nouvelle erreur
        |
        v
Nouvelle tentative
```

Si plusieurs instances effectuent des retries immédiats, elles risquent de maintenir le service distant sous pression, même lorsque celui-ci tente de récupérer.

Ce phénomène peut provoquer :

- Une augmentation du nombre de requêtes.
- Une saturation des connexions et des threads.
- Une augmentation de la latence.
- Une propagation des erreurs vers d'autres microservices.
- Une indisponibilité prolongée du service distant.

Le Backoff permet d'espacer les tentatives pour limiter ces effets.

## 3. Les principales stratégies de Backoff

### 3.1. Backoff fixe

Le délai reste identique après chaque échec.

Exemple avec un délai de 2 secondes :

| Tentative | Délai avant la tentative suivante |
|---|---:|
| 1 | 2 s |
| 2 | 2 s |
| 3 | 2 s |
| 4 | Arrêt après le nombre maximal de tentatives |

Cette stratégie est simple à comprendre et à configurer.

Elle convient aux situations où un délai constant est suffisant. Cependant, si de nombreux clients réessaient simultanément, ils peuvent continuer à solliciter le service aux mêmes intervalles.

### 3.2. Backoff exponentiel

Le délai augmente de façon exponentielle à chaque échec.

La formule classique est :

\[
d_n = \min(d_{\max}, d_0 \times 2^{n-1})
\]

Avec :

- \(d_n\) : délai avant la prochaine tentative.
- \(d_0\) : délai initial.
- \(n\) : numéro de la tentative de reprise.
- \(d_{\max}\) : délai maximal autorisé.

Exemple avec un délai initial de 1 seconde et un maximum de 8 secondes :

| Tentative de reprise | Délai |
|---|---:|
| 1 | 1 s |
| 2 | 2 s |
| 3 | 4 s |
| 4 | 8 s |
| 5 | 8 s |

Le délai est plafonné pour éviter des attentes excessives.

### 3.3. Backoff exponentiel avec jitter

Le **jitter** ajoute une variation aléatoire au délai calculé.

Pourquoi est-ce utile ?

Supposons que 100 instances d'une application rencontrent une panne simultanément. Avec un Backoff exponentiel sans jitter, elles peuvent toutes réessayer après 1 seconde, puis 2 secondes, puis 4 secondes.

Les requêtes se regroupent alors en vagues.

Le jitter répartit les tentatives dans le temps.

Exemple :

```text
Sans jitter :
1 s -> 2 s -> 4 s -> 8 s

Avec jitter :
0,7 s -> 1,8 s -> 3,2 s -> 6,9 s
```

Ces valeurs sont illustratives. La distribution exacte dépend de la stratégie de jitter choisie.

Le Backoff exponentiel avec jitter est généralement une bonne option pour les appels distribués, lorsque plusieurs clients peuvent subir les mêmes erreurs au même moment.

## 4. Backoff et Retry : quelle différence ?

Ces deux notions sont complémentaires, mais ne désignent pas la même chose.

| Concept | Rôle |
|---|---|
| Retry | Décide de relancer une opération après un échec |
| Backoff | Détermine le délai avant la nouvelle tentative |
| Timeout | Limite la durée d'attente d'une opération |
| Circuit Breaker | Interrompt temporairement les appels vers un service défaillant |
| Idempotence | Évite de reproduire plusieurs fois les effets métier d'une même opération |

Exemple :

Une API partenaire renvoie une erreur temporaire.

1. Le Retry décide qu'une nouvelle tentative est autorisée.
2. Le Backoff détermine le délai d'attente.
3. Le Timeout limite la durée de cette nouvelle tentative.
4. Le Circuit Breaker peut empêcher les appels si le service est jugé trop défaillant.
5. L'idempotence sécurise les répétitions lorsque l'opération peut produire des effets métier.

Ces mécanismes doivent être configurés ensemble pour éviter les comportements incontrôlés.

## 5. Implémentation avec Spring Boot et Resilience4j

Resilience4j fournit un module Retry qui permet de configurer le nombre de tentatives, les délais et les exceptions concernées.

### 5.1. Dépendance Maven

Pour une application Spring Boot utilisant Spring AOP et Resilience4j :

```xml
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
    <version>${resilience4j.version}</version>
</dependency>
```

La propriété `resilience4j.version` doit correspondre à une version compatible avec le projet. Dans une application utilisant Spring Cloud, il convient également de vérifier la compatibilité avec les versions de Spring Boot et de Spring Cloud retenues.

### 5.2. Configuration YAML

```yaml
resilience4j:
  retry:
    instances:
      tarification:
        maxAttempts: 4
        waitDuration: 1s
        enableExponentialBackoff: true
        exponentialBackoffMultiplier: 2
        enableRandomizedWait: true
        randomizedWaitFactor: 0.5
```

Explication :

- `maxAttempts: 4` : quatre tentatives au total, première exécution comprise.
- `waitDuration: 1s` : délai initial d'une seconde.
- `enableExponentialBackoff: true` : augmentation exponentielle du délai.
- `exponentialBackoffMultiplier: 2` : multiplication du délai par deux.
- `enableRandomizedWait: true` : ajout d'une variation aléatoire.
- `randomizedWaitFactor: 0.5` : facteur de variation du délai.

Avec cette configuration, les délais exacts dépendent du calcul du Backoff et du jitter appliqué par Resilience4j. Il ne faut pas les interpréter comme des valeurs fixes garanties.

### 5.3. Utilisation dans un service

```java
@Service
public class TarificationService {

    private final TarificationClient tarificationClient;

    public TarificationService(TarificationClient tarificationClient) {
        this.tarificationClient = tarificationClient;
    }

    @Retry(name = "tarification")
    public TarificationResponse calculerTarification(
            TarificationRequest request) {

        return tarificationClient.calculer(request);
    }
}
```

Lorsque la méthode échoue avec une exception éligible à la stratégie Retry, Resilience4j peut relancer l'appel selon la configuration.

**Attention :** par défaut, Retry traite les exceptions selon ses règles configurées. Il est recommandé de préciser les exceptions qui justifient une nouvelle tentative et celles qui doivent provoquer un échec immédiat.

Par exemple, une erreur de validation HTTP 400 ne doit généralement pas être réessayée, contrairement à certaines erreurs réseau temporaires ou à certaines réponses 5xx.

## 6. Contrôler les exceptions réessayées

Toutes les erreurs ne sont pas temporaires.

Il est préférable de distinguer :

- Les erreurs réseau transitoires.
- Les timeouts.
- Certaines réponses HTTP 5xx.
- Les réponses HTTP 429, selon les règles de l'API distante.
- Les erreurs métier définitives.
- Les erreurs de validation 4xx.

Exemple de configuration :

```yaml
resilience4j:
  retry:
    instances:
      tarification:
        maxAttempts: 4
        waitDuration: 1s
        retryExceptions:
          - java.io.IOException
        ignoreExceptions:
          - com.example.exception.ValidationException
```

Cette configuration illustre le principe de sélection des exceptions. Dans une intégration HTTP réelle, les erreurs peuvent être encapsulées dans des exceptions propres au client utilisé, par exemple OpenFeign ou WebClient. Il faut donc adapter les types d'exceptions à l'implémentation effective.

Pour OpenFeign, les erreurs HTTP sont notamment représentées par `FeignException`. Il peut être nécessaire de filtrer les codes HTTP au moyen d'une stratégie personnalisée plutôt que de réessayer toutes les exceptions indistinctement.

## 7. Gérer le délai maximal et le temps total

Le Backoff ne doit pas entraîner une attente illimitée.

Il faut définir :

- Un nombre maximal de tentatives.
- Un délai initial.
- Un délai maximal entre les tentatives.
- Un timeout par tentative.
- Une durée totale maximale acceptable pour l'opération.

Exemple :

```text
Nombre maximal de tentatives : 4
Délai initial : 1 seconde
Délai maximal : 8 secondes
Timeout par appel : 3 secondes
```

Le temps total peut inclure les durées d'exécution des appels et les périodes d'attente entre les tentatives.

Il est donc important de vérifier que la stratégie de Retry reste compatible avec le timeout du client, le timeout de la Gateway et le délai maximal attendu par l'utilisateur.

Un Retry peut être techniquement correct tout en étant inutile si la réponse arrive après que le client a déjà abandonné sa requête.

## 8. Backoff et Circuit Breaker

Le Backoff et le Circuit Breaker répondent à des problèmes différents.

Le Backoff réduit la fréquence des tentatives. Le Circuit Breaker limite les appels lorsqu'un service présente des signes de défaillance.

Exemple :

```text
Application
    |
    v
Retry + Backoff
    |
    v
Circuit Breaker
    |
    v
Service distant
```

Cet ordre est un exemple courant, pas une règle universelle. L'ordre des décorateurs Resilience4j change le comportement global.

Il faut notamment éviter une configuration où les retries continuent à générer des appels inutiles alors que le service distant est déjà considéré comme indisponible.

Pour un service durablement défaillant, le Circuit Breaker peut interrompre les appels avant que chaque requête ne consomme tout son budget de tentatives.

## 9. Backoff dans une architecture distribuée

Dans une architecture composée de plusieurs microservices, les retries peuvent se multiplier à chaque niveau.

Exemple :

```text
Gateway
   |
   | 3 tentatives
   v
Service A
   |
   | 3 tentatives
   v
Service B
   |
   | 3 tentatives
   v
Service C
```

Si chaque niveau effectue trois tentatives au total, le nombre maximal d'appels vers le service C peut atteindre 27 pour une seule requête initiale, dans un scénario où toutes les tentatives sont déclenchées.

Cette multiplication peut aggraver une panne.

Bonnes pratiques :

- Éviter de configurer des retries indépendants à chaque niveau sans analyser leur cumul.
- Définir clairement le niveau responsable des nouvelles tentatives.
- Limiter le nombre total de tentatives.
- Prévoir des timeouts cohérents entre les services.
- Utiliser le jitter lorsque de nombreux clients peuvent réessayer simultanément.
- Ne pas réessayer automatiquement les opérations non idempotentes sans mécanisme de protection.
- Respecter, lorsque c'est pertinent, les indications de temporisation communiquées par le service distant, comme `Retry-After`.

## 10. Bonnes pratiques

- Privilégier le Backoff exponentiel avec jitter pour les appels distribués sujets aux pannes simultanées.
- Limiter le nombre de tentatives.
- Éviter les retries sur les erreurs définitives.
- Garder une durée totale compatible avec les exigences métier.
- Ne pas multiplier les retries à tous les niveaux de l'architecture.
- Vérifier l'idempotence avant de réessayer une opération de création ou de transformation.
- Combiner le Backoff avec des timeouts et, lorsque c'est utile, un Circuit Breaker.
- Mesurer le nombre de tentatives, les erreurs persistantes et le temps total des opérations.
- Tester les scénarios de panne pour vérifier que les délais et les limites fonctionnent comme prévu.

## 11. À retenir

Le Backoff permet d'espacer les tentatives afin de réduire la pression sur les services en difficulté.

Dans une application Spring Boot, Resilience4j permet de mettre en œuvre cette stratégie avec une configuration déclarative.

**Principe clé :** réessayer peut améliorer la fiabilité, mais réessayer trop vite ou trop souvent peut aggraver une panne.