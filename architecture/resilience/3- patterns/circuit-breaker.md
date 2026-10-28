# Circuit Breaker — Protection contre les services défaillants

## 1. Définition

Le **Circuit Breaker** (disjoncteur logiciel) est un pattern de résilience qui interrompt temporairement les appels vers un service distant lorsque celui-ci présente trop d'échecs.

Son objectif est d'éviter de solliciter continuellement un service défaillant, de limiter la propagation des incidents et de laisser au service distant le temps de récupérer.

Le principe est similaire à celui d'un disjoncteur électrique : lorsqu'une anomalie dépasse un seuil défini, le circuit s'ouvre et bloque temporairement le passage.

Dans une architecture distribuée, ce mécanisme est particulièrement utile pour les appels HTTP vers des API partenaires, les microservices et les systèmes externes.

## 2. Pourquoi utiliser un Circuit Breaker ?

Considérons une application Spring Boot qui appelle un service partenaire pour calculer une tarification.

```text
Client
  |
  v
API Simulation
  |
  v
Service Tarification
  |
  v
API Partenaire
```

Si l'API partenaire devient indisponible, chaque requête peut échouer après plusieurs secondes d'attente.

Lorsque le volume de requêtes augmente, cette situation peut provoquer :

- une occupation excessive des threads ;
- une augmentation de la latence ;
- une multiplication des erreurs ;
- une saturation des ressources ;
- une propagation de la panne vers les autres composants.

Le circuit breaker surveille les résultats des appels et peut bloquer temporairement les nouvelles tentatives vers le service défaillant.

```text
                 Appels
                   |
                   v
             Circuit Breaker
                   |
          +--------+--------+
          |                 |
     Circuit fermé     Circuit ouvert
          |                 |
          v                 v
   API partenaire      Appel bloqué
```

Lorsque le circuit est ouvert, l'appel est généralement refusé localement, sans attendre une réponse du service distant.

## 3. Les trois états du Circuit Breaker

Un circuit breaker possède généralement trois états.

### 3.1. CLOSED — Circuit fermé

Le circuit autorise les appels vers le service distant.

Il collecte des statistiques sur les résultats des appels, notamment les échecs et les temps de réponse.

Si les échecs dépassent les seuils configurés, il peut passer à l'état `OPEN`.

### 3.2. OPEN — Circuit ouvert

Le circuit bloque les appels pendant une durée configurée.

L'application évite ainsi de solliciter inutilement un service qui semble défaillant.

Selon l'implémentation, une exception spécifique est levée et peut être traitée par un fallback.

Après le délai d'attente, le circuit peut passer à l'état `HALF_OPEN`.

### 3.3. HALF_OPEN — Circuit semi-ouvert

Le circuit autorise un nombre limité d'appels de test afin de vérifier si le service est rétabli.

- Si les appels de test réussissent selon les critères configurés, le circuit revient à `CLOSED`.
- Si les échecs persistent, le circuit repasse à `OPEN`.

### Cycle de fonctionnement

```text
                Échecs au-dessus du seuil
        +------------------------------------+
        |                                    v
    CLOSED ------------------------------> OPEN
        ^                                    |
        |                                    |
        |                              Délai écoulé
        |                                    |
        |                                    v
        +-------------------------------- HALF_OPEN
                  Tests réussis
```

Le passage entre les états dépend de la configuration et des résultats observés.

## 4. Comment les échecs sont-ils évalués ?

Un circuit breaker peut s'appuyer sur différentes stratégies de mesure.

### 4.1. Fenêtre basée sur le nombre d'appels

Le système analyse les derniers appels, par exemple les dix dernières requêtes.

Cette approche est appelée `COUNT_BASED`.

### 4.2. Fenêtre basée sur le temps

Le système analyse les appels effectués pendant une période donnée, par exemple les dix dernières secondes.

Cette approche est appelée `TIME_BASED`.

### 4.3. Taux d'échec

Le circuit peut s'ouvrir lorsque le pourcentage d'échecs atteint un seuil configuré.

Par exemple, avec un seuil de 50 %, le circuit peut s'ouvrir lorsque la moitié des appels évalués échouent, sous réserve du nombre minimal d'appels requis.

### 4.4. Taux d'appels lents

Certains circuit breakers peuvent également tenir compte des appels qui dépassent une durée considérée comme acceptable.

Un service qui répond systématiquement très lentement peut être problématique même si ses réponses finissent par réussir.

Il faut distinguer ce mécanisme du timeout : le circuit breaker détecte les appels lents selon ses critères, tandis que le timeout limite le temps d'attente d'une opération.

## 5. Circuit Breaker et Retry : quelle différence ?

Ces deux mécanismes sont complémentaires, mais leurs objectifs diffèrent.

| Circuit Breaker | Retry |
|---|---|
| Bloque temporairement les appels vers un service défaillant. | Retente un appel après un échec. |
| Évite de solliciter continuellement un service indisponible. | Permet de récupérer après une erreur temporaire. |
| S'appuie sur des statistiques et des seuils. | S'appuie sur un nombre de tentatives et une politique de délai. |
| Peut rester ouvert pendant une durée définie. | Effectue de nouvelles tentatives selon sa configuration. |

Un retry mal configuré peut aggraver une panne en multipliant les appels. Un circuit breaker permet de limiter cette pression lorsque les échecs deviennent trop fréquents.

L'ordre d'application des deux mécanismes est important : selon la configuration, un circuit breaker peut comptabiliser chaque tentative individuelle ou un appel logique incluant plusieurs retries.

## 6. Circuit Breaker et Fallback

Le circuit breaker décide si un appel doit être autorisé. Le fallback définit le comportement alternatif lorsqu'un appel échoue ou est bloqué.

Par exemple :

```text
Appel métier
     |
     v
Circuit Breaker
     |
     +---- Circuit fermé ----> Appel partenaire
     |                              |
     |                         Succès / Échec
     |
     +---- Circuit ouvert
                   |
                   v
                Fallback
                   |
                   v
          Réponse alternative
```

Le fallback peut fournir une réponse dégradée autorisée ou convertir l'incident en une erreur applicative contrôlée.

Dans un système financier, il ne doit jamais retourner une tarification inventée ou un montant présenté comme fiable alors que le calcul n'a pas été effectué.

## 7. Implémentation avec Spring Boot et Resilience4j

Resilience4j fournit une implémentation du pattern Circuit Breaker compatible avec les applications Java.

### 7.1. Dépendance Maven

Dans une application Spring Boot utilisant Spring Cloud OpenFeign, ajoute le module Spring Boot de Resilience4j compatible avec la version de Spring Boot du projet.

Exemple de dépendance :

```xml
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
    <version>${resilience4j.version}</version>
</dependency>
```

La propriété `resilience4j.version` doit être définie avec une version compatible. Si le projet utilise un BOM de gestion des dépendances, il est préférable de centraliser la version dans celui-ci.

### 7.2. Client OpenFeign

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

### 7.3. Service protégé

```java
@Service
@RequiredArgsConstructor
public class PartnerResilienceService {

    private final PartnerFeignClient partnerFeignClient;

    @CircuitBreaker(
        name = "partnerApi",
        fallbackMethod = "tarificationFallback"
    )
    public TarificationResponse getTarification() {
        return partnerFeignClient.getTarification();
    }

    private TarificationResponse tarificationFallback(Throwable exception) {
        throw new PartnerUnavailableException(
            "Le service de tarification est temporairement indisponible",
            exception
        );
    }
}
```

Dans cet exemple, `TarificationResponse` et `PartnerUnavailableException` représentent des classes à définir dans le projet.

Le fallback propage une exception métier contrôlée plutôt que de fabriquer un résultat de tarification.

Un gestionnaire global d'exceptions peut ensuite traduire cette exception en réponse HTTP adaptée, par exemple `503 Service Unavailable`.

### 7.4. Configuration dans `application.yml`

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
        automaticTransitionFromOpenToHalfOpenEnabled: true
```

Cette configuration signifie notamment :

- les statistiques sont calculées sur une fenêtre de dix appels ;
- le calcul du taux d'échec commence après au moins cinq appels ;
- le circuit peut s'ouvrir lorsque le taux d'échec atteint 50 % ;
- après quinze secondes, le circuit peut passer en mode semi-ouvert ;
- trois appels de test peuvent être autorisés en mode semi-ouvert.

Le paramètre de transition automatique permet au circuit de passer à `HALF_OPEN` après le délai configuré, même si aucun nouvel appel n'est effectué pendant ce délai.

Sans cette option, le passage à l'état semi-ouvert peut être déclenché par un nouvel appel après expiration du délai.

## 8. Points importants lors de l'utilisation de Resilience4j

### 8.1. Les exceptions comptabilisées

Le circuit breaker ne doit pas nécessairement considérer toutes les exceptions comme des échecs.

Par exemple, une erreur métier indiquant que les données de la requête sont invalides ne signifie pas que le service partenaire est indisponible.

La configuration peut préciser les exceptions à enregistrer ou à ignorer.

### 8.2. Les appels lents

Il est possible de configurer un seuil de durée et un taux d'appels lents afin d'ouvrir le circuit lorsque le service devient trop lent.

Cette protection complète les timeouts, mais ne les remplace pas.

### 8.3. La portée du circuit

Un circuit breaker Resilience4j est généralement local à l'instance de l'application.

Si plusieurs instances Spring Boot sont déployées, chacune peut avoir un état de circuit différent. Le circuit breaker ne constitue donc pas automatiquement un mécanisme distribué de coordination.

### 8.4. Les appels internes

Avec l'intégration Spring AOP, les annotations comme `@CircuitBreaker` doivent être appelées à travers le proxy Spring pour que l'interception s'applique. Un appel direct entre deux méthodes de la même instance peut contourner ce mécanisme.

## 9. Bonnes pratiques

- Configurer des fenêtres et des seuils adaptés au trafic réel.
- Définir un nombre minimal d'appels avant d'évaluer les statistiques.
- Distinguer les erreurs métier des erreurs techniques.
- Associer le circuit breaker à des timeouts explicites.
- Limiter les retries et définir leur ordre d'application.
- Définir un fallback conforme aux règles métier.
- Surveiller les transitions entre les états `CLOSED`, `OPEN` et `HALF_OPEN`.
- Tester le comportement après plusieurs échecs et après le rétablissement du service.
- Vérifier le comportement en environnement multi-instance.
- Éviter de considérer une seule réponse réussie comme une garantie de stabilité durable.

## 10. À retenir

Le **Circuit Breaker** protège une application contre les appels répétés vers un service qui présente trop d'échecs ou de lenteurs.

Ses trois états — `CLOSED`, `OPEN` et `HALF_OPEN` — permettent de contrôler les appels, de limiter la propagation des incidents et de vérifier progressivement le rétablissement.

Avec Spring Boot et Resilience4j, il peut être appliqué aux appels OpenFeign afin d'améliorer la résilience des interactions entre services.
