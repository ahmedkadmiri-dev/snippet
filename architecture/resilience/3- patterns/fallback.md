# Fallback — Fournir une solution de secours en cas d'échec

## 1. Définition

Le **Fallback** est un mécanisme de résilience qui permet de fournir une réponse alternative lorsqu'une opération échoue ou qu'un service distant devient indisponible.

Au lieu de laisser systématiquement une erreur interrompre le traitement, l'application peut utiliser une solution de secours adaptée au besoin métier.

Le Fallback peut consister à :
- Retourner une valeur par défaut.
- Utiliser des données précédemment mises en cache.
- Fournir une réponse partielle.
- Appeler un service alternatif.
- Informer le client que la fonctionnalité est temporairement indisponible.

**Objectif principal :** maintenir un niveau de service acceptable malgré la défaillance d'une dépendance.

## 2. Pourquoi utiliser un Fallback ?

Considérons une application bancaire qui calcule une simulation de financement et appelle un service externe pour récupérer des informations complémentaires.

```text
Client
  |
  v
moteur-financier
  |
  +----> Service de tarification
  |
  +----> Service de référentiel
  |
  +----> Service de notification
```

Supposons que le service de notification soit indisponible.

Si l'envoi du SMS n'est pas indispensable au calcul de la simulation, l'application peut poursuivre le traitement et enregistrer la notification pour une tentative ultérieure.

```text
moteur-financier
       |
       v
Service de notification
       |
       v
     Échec
       |
       v
     Fallback
       |
       v
Enregistrer la notification
pour traitement ultérieur
```

La simulation peut ainsi être terminée sans que l'indisponibilité du service SMS bloque l'ensemble du parcours.

Attention : un Fallback n'est approprié que si la réponse de secours respecte les règles métier. Il ne faut pas masquer une erreur en retournant arbitrairement un résultat présenté comme valide.

## 3. Les principaux types de Fallback

### 3.1. Valeur par défaut

L'application retourne une valeur prédéfinie lorsque l'opération échoue.

Exemple : une application de consultation peut retourner une liste vide si le domaine fonctionnel autorise explicitement ce comportement.

```java
public List<ProduitDto> getProduits() {
    try {
        return produitClient.getProduits();
    } catch (ServiceUnavailableException exception) {
        log.warn("Service produits indisponible", exception);
        return List.of();
    }
}
```

Cette approche est simple, mais elle peut être trompeuse : une liste vide peut signifier « aucun produit disponible » alors que le service n'a pas répondu.

Il faut donc choisir une valeur par défaut qui ne crée pas d'ambiguïté métier.

### 3.2. Données en cache

Lorsque le service distant est indisponible, l'application peut retourner des données précédemment récupérées.

Exemple :

```text
Application
    |
    v
Service de référentiel
    |
    +----> Disponible : récupérer les données
    |
    +----> Indisponible : utiliser le cache
```

Cette solution est particulièrement utile pour les données relativement stables :
- Les catégories socio-professionnelles.
- Les référentiels de produits.
- Les paramètres d'affichage.
- Certaines informations de configuration.

Il faut toutefois définir une durée de validité maximale et accepter explicitement le risque de données obsolètes.

Pour les informations financières sensibles, les soldes ou les résultats qui doivent être exacts à l'instant de la demande, l'utilisation d'un cache comme Fallback exige des règles métier strictes.

### 3.3. Service alternatif

L'application peut utiliser une autre dépendance lorsque le service principal échoue.

Exemple :

```text
Application
    |
    v
Service principal
    |
    +----> Succès : utiliser la réponse
    |
    +----> Échec : appeler le service alternatif
```

Cette stratégie nécessite que le service alternatif fournisse des données ou des fonctionnalités suffisamment compatibles.

Elle ne doit pas créer une surcharge supplémentaire en cascade lorsque plusieurs services sont déjà en difficulté.

### 3.4. Réponse partielle

L'application retourne les informations disponibles sans attendre toutes les dépendances facultatives.

Par exemple, une page de simulation peut afficher le résultat principal même si le service de recommandations est indisponible, à condition que le résultat de simulation reste complet et valide.

### 3.5. Report du traitement

Pour les opérations qui ne doivent pas être perdues, l'application peut enregistrer le travail à effectuer dans une file persistante ou une table d'outbox, puis le traiter ultérieurement.

Exemple : si le service SMS est indisponible, la notification est enregistrée avec un statut « à envoyer ».

Cette stratégie nécessite une gestion fiable des nouvelles tentatives, de l'idempotence et des erreurs définitives.

## 4. Implémenter un Fallback avec Spring Boot et Resilience4j

### 4.1. Configuration Maven

Pour une application Spring Boot compatible avec Resilience4j :

```xml
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
</dependency>
```

La version doit être compatible avec celle de Spring Boot et avec le BOM Resilience4j utilisé par le projet.

L'intégration par annotation nécessite également une configuration Spring AOP appropriée.

### 4.2. Exemple avec `@CircuitBreaker`

Resilience4j permet de définir une méthode de secours avec le paramètre `fallbackMethod`.

```java
@Service
public class ReferentielService {

    private final ReferentielClient referentielClient;

    public ReferentielService(ReferentielClient referentielClient) {
        this.referentielClient = referentielClient;
    }

    @CircuitBreaker(
        name = "referentiel",
        fallbackMethod = "getCategoriesFallback"
    )
    public List<CategorieDto> getCategories() {
        return referentielClient.getCategories();
    }

    private List<CategorieDto> getCategoriesFallback(Exception exception) {
        log.warn("Impossible de récupérer les catégories", exception);
        return List.of();
    }
}
```

Dans cet exemple :
- La méthode principale appelle le service distant.
- Si une exception est prise en charge par le mécanisme de résilience, la méthode de secours peut être appelée.
- Le Fallback retourne une liste vide.

La liste vide n'est qu'un exemple technique. En production, il faut vérifier qu'elle constitue une réponse métier acceptable.

La méthode de secours doit avoir une signature compatible avec la méthode principale et recevoir, en dernier paramètre, l'exception appropriée. Il est également possible de déclarer une méthode de secours sans paramètre d'exception selon les signatures acceptées par la version utilisée.

**Point important :** `@CircuitBreaker` peut déclencher le Fallback lorsque l'appel échoue, mais le Fallback ne remplace pas un Timeout. Pour borner la durée d'attente, il faut configurer un Timeout adapté au client HTTP ou utiliser un mécanisme de résilience approprié.

### 4.3. Fallback avec un cache

Une autre approche consiste à utiliser explicitement un cache de secours :

```java
@Service
public class ProduitService {

    private final ProduitClient produitClient;
    private final ProduitCache produitCache;

    public ProduitService(
            ProduitClient produitClient,
            ProduitCache produitCache) {
        this.produitClient = produitClient;
        this.produitCache = produitCache;
    }

    public List<ProduitDto> getProduits() {
        try {
            List<ProduitDto> produits = produitClient.getProduits();

            produitCache.put(produits);

            return produits;
        } catch (ServiceUnavailableException exception) {
            log.warn("Service produits indisponible, utilisation du cache", exception);

            List<ProduitDto> produitsEnCache = produitCache.get();

            if (produitsEnCache != null) {
                return produitsEnCache;
            }

            throw exception;
        }
    }
}
```

Cet exemple suppose que `ProduitCache` implémente les opérations de lecture et d'écriture nécessaires. Il ne montre pas la gestion de l'expiration ni les détails de synchronisation.

En pratique, il est préférable de :
- Vérifier la fraîcheur des données.
- Distinguer une valeur absente d'une valeur vide.
- Limiter les types d'erreurs qui déclenchent le Fallback.
- Préserver une erreur explicite lorsque les données de secours ne sont pas disponibles.

## 5. Fallback et Retry

Le Retry et le Fallback ont des objectifs différents.

| Mécanisme | Objectif |
|---|---|
| Retry | Réessayer une opération après un échec temporaire. |
| Fallback | Fournir une alternative lorsque l'opération principale échoue. |

Exemple :

```text
Appel du service
       |
       v
Échec temporaire
       |
       v
Retry
       |
       +----> Succès : retourner le résultat
       |
       +----> Échec définitif : Fallback
```

Le nombre de tentatives doit rester limité. Si toutes les tentatives échouent, le Fallback peut prendre le relais.

Il faut éviter de réessayer les erreurs qui ne sont pas temporaires, comme certaines erreurs de validation ou d'autorisation.

## 6. Fallback et Circuit Breaker

Le Circuit Breaker surveille les échecs et peut empêcher de nouveaux appels vers une dépendance qui échoue trop souvent.

Le Fallback définit ce que l'application doit faire lorsqu'un appel échoue ou est refusé par le Circuit Breaker.

```text
Requête
   |
   v
Circuit Breaker
   |
   +----> Appel autorisé
   |          |
   |          v
   |      Service distant
   |          |
   |          +----> Succès : réponse normale
   |          |
   |          +----> Échec : Fallback
   |
   +----> Circuit ouvert : Fallback
```

Le comportement exact dépend de l'ordre des mécanismes, de la configuration et des exceptions prises en charge.

Le Fallback doit notamment être conçu pour ne pas appeler systématiquement la même dépendance défaillante, ce qui annulerait une partie du bénéfice du Circuit Breaker.

## 7. Bonnes pratiques

### 7.1. Définir un Fallback métier pertinent

Un Fallback doit fournir une réponse utile et cohérente avec le métier.

Exemples :
- Utiliser un référentiel en cache lorsque sa fraîcheur est acceptable.
- Reporter l'envoi d'une notification.
- Afficher un message indiquant qu'une fonctionnalité facultative est temporairement indisponible.
- Retourner une réponse partielle lorsque les informations manquantes ne sont pas indispensables.

### 7.2. Ne pas masquer les erreurs

Un Fallback qui transforme systématiquement toutes les exceptions en succès apparent peut cacher des problèmes graves.

Il faut distinguer :
- Les erreurs temporaires d'infrastructure.
- Les erreurs métier.
- Les erreurs de validation.
- Les erreurs d'authentification et d'autorisation.
- Les erreurs de programmation.

Une erreur de validation ne doit généralement pas être transformée en réponse de secours.

### 7.3. Ne pas utiliser de valeurs financières fictives

Dans un moteur financier, il ne faut pas remplacer un taux d'intérêt, une mensualité ou une capacité d'emprunt manquante par une valeur arbitraire.

Si le calcul principal ne peut pas être effectué correctement, il est préférable de retourner une erreur métier explicite ou un statut d'indisponibilité.

Un Fallback n'est acceptable que si les règles métier autorisent le résultat de substitution.

### 7.4. Rendre les opérations différées fiables

Pour un traitement reporté, il faut prévoir :
- Une persistance durable.
- Des tentatives limitées ou une stratégie de reprise adaptée.
- Une gestion des échecs définitifs.
- L'idempotence pour éviter les doublons.
- Une supervision de la file de traitement.

### 7.5. Surveiller les Fallbacks

Les indicateurs utiles comprennent :
- Le nombre de Fallbacks exécutés.
- Les raisons de leur déclenchement.
- Le nombre de réponses provenant du cache.
- L'ancienneté des données utilisées.
- Le nombre de traitements reportés.
- Les échecs des solutions de secours elles-mêmes.

Une augmentation soudaine des Fallbacks peut révéler une dépendance dégradée, même si les clients ne constatent pas immédiatement d'erreur.

## 8. Limites du Fallback

Le Fallback n'est pas toujours possible.

Il peut être inadapté lorsque :
- Le résultat doit impérativement être exact.
- Aucune donnée alternative fiable n'est disponible.
- La dépendance est obligatoire pour respecter les règles métier.
- La réponse de secours introduit un risque de sécurité ou de cohérence.

Il faut aussi éviter les Fallbacks en cascade, où une solution de secours appelle plusieurs autres services indisponibles et aggrave la situation.

## 9. À retenir

Le Fallback permet de préserver une fonctionnalité ou une partie du service lorsqu'une opération échoue.

Les points essentiels sont :

1. Définir une réponse de secours adaptée au métier.
2. Distinguer les erreurs temporaires des erreurs métier.
3. Combiner le Fallback avec Timeout, Retry et Circuit Breaker lorsque cela est pertinent.
4. Utiliser le cache ou le report de traitement uniquement avec des règles de cohérence explicites.
5. Mesurer les Fallbacks pour détecter les dégradations masquées.

**Règle d'or :** une réponse de secours doit préserver le service sans compromettre la justesse du résultat métier.