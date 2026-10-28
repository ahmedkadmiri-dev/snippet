# Graceful Degradation — Maintenir le service en mode dégradé

## 1. Définition

La **Graceful Degradation**, ou dégradation progressive, est une stratégie de résilience qui consiste à maintenir les fonctionnalités essentielles d'une application lorsqu'une partie de ses composants devient indisponible ou fonctionne moins bien.

Au lieu de laisser une panne secondaire provoquer l'indisponibilité de toute l'application, le système réduit volontairement certaines fonctionnalités non essentielles pour préserver les fonctions prioritaires.

**Objectif principal :** continuer à fournir un service utile, même lorsque toutes les fonctionnalités ne peuvent plus être assurées normalement.

Exemples :
- Une application bancaire permet de consulter les comptes, mais désactive temporairement les statistiques avancées.
- Un moteur de simulation retourne le résultat principal, mais ne fournit pas les recommandations complémentaires.
- Un site e-commerce conserve la consultation des produits alors que le service de recommandations est indisponible.

## 2. Pourquoi utiliser la Graceful Degradation ?

Considérons une application de simulation financière qui dépend de plusieurs services :

```text
                  Application bancaire
                          |
             +------------+------------+
             |            |            |
             v            v            v
        Simulation    Référentiel   Recommandations
        financière    obligatoire     facultatives
```

Le service de simulation et le référentiel sont nécessaires au calcul. En revanche, les recommandations sont une fonctionnalité complémentaire.

Si le service de recommandations tombe en panne, deux stratégies sont possibles.

**Sans dégradation progressive :**

```text
Service de recommandations indisponible
                  |
                  v
          Échec de la requête
                  |
                  v
       Simulation non disponible
```

**Avec dégradation progressive :**

```text
Service de recommandations indisponible
                  |
                  v
       Désactivation des recommandations
                  |
                  v
       Simulation toujours disponible
```

La seconde approche préserve la fonctionnalité essentielle sans prétendre que l'ensemble du système fonctionne normalement.

## 3. Les niveaux de dégradation

La dégradation peut être organisée selon plusieurs niveaux.

### 3.1. Niveau 1 — Fonctionnement normal

Toutes les fonctionnalités sont disponibles.

Exemple :
- Calcul de simulation.
- Consultation des référentiels.
- Recommandations.
- Notifications.

### 3.2. Niveau 2 — Fonctionnalités secondaires désactivées

Les fonctionnalités essentielles restent opérationnelles, mais certaines fonctions complémentaires sont supprimées.

Exemple :
- La simulation fonctionne.
- Les recommandations sont désactivées.
- Les statistiques avancées ne sont plus affichées.

### 3.3. Niveau 3 — Fonctionnement limité

Le système continue à fonctionner avec des données ou des capacités réduites, dans les limites autorisées par le métier.

Exemple :
- Un référentiel non critique est consulté depuis un cache dont la fraîcheur est acceptable.
- Les traitements non urgents sont reportés.
- Certaines opérations coûteuses sont temporairement désactivées.

### 3.4. Niveau 4 — Mode de secours

Seules les fonctionnalités strictement nécessaires restent disponibles.

Exemple :
- Consultation d'informations essentielles.
- Affichage d'un message d'indisponibilité pour les opérations sensibles.
- Mise en attente des traitements différables.

Le niveau de dégradation doit être défini explicitement. Il ne faut pas laisser l'application produire des résultats incorrects simplement pour rester disponible.

## 4. Comment mettre en œuvre la Graceful Degradation ?

Plusieurs mécanismes peuvent être combinés.

### 4.1. Identifier les fonctionnalités essentielles

Chaque fonctionnalité doit être classée selon son importance métier.

Exemple :

| Fonctionnalité | Priorité | Comportement en cas de panne |
|---|---|---|
| Calcul financier principal | Critique | Échec explicite si le calcul n'est pas fiable |
| Consultation d'un référentiel obligatoire | Élevée | Utiliser un cache uniquement si les règles l'autorisent |
| Recommandations complémentaires | Faible | Désactiver temporairement |
| Notification SMS | Variable | Reporter l'envoi si possible |
| Statistiques avancées | Faible | Désactiver temporairement |

Cette classification dépend du métier. Une notification peut être facultative dans un parcours, mais obligatoire dans un autre pour des raisons réglementaires ou de sécurité.

### 4.2. Utiliser des Fallbacks adaptés

Le Fallback permet de remplacer une réponse indisponible par une solution de secours.

Exemple :

```java
public SimulationOutputDto calculerSimulation(SimulationInputDto input) {
    SimulationOutputDto resultat = simulationClient.calculer(input);

    try {
        List<RecommandationDto> recommandations =
                recommandationClient.getRecommandations(resultat);

        resultat.setRecommandations(recommandations);
    } catch (RecommandationServiceUnavailableException exception) {
        log.warn("Recommandations indisponibles : mode dégradé activé");
        resultat.setRecommandations(List.of());
    }

    return resultat;
}
```

Cet exemple illustre le principe : l'indisponibilité du service de recommandations n'empêche pas de retourner le résultat principal.

Le code suppose que l'absence de recommandations est autorisée par le contrat métier. En production, il faut également gérer les timeouts et les autres exceptions pertinentes, sans masquer les erreurs de calcul ni les erreurs de validation.

### 4.3. Utiliser le Circuit Breaker

Le Circuit Breaker évite de continuer à appeler une dépendance qui échoue de manière répétée.

Associé à un Fallback, il peut permettre à l'application de basculer vers un mode dégradé lorsque le circuit est ouvert.

```text
Appel du service secondaire
           |
           v
     Circuit Breaker
           |
      +----+----+
      |         |
   Fermé      Ouvert
      |         |
      v         v
 Appel réel   Fallback
      |         |
      v         v
  Résultat   Mode dégradé
```

Le Circuit Breaker ne définit pas lui-même les fonctionnalités à désactiver. Cette décision appartient à la logique applicative.

### 4.4. Utiliser des données en cache

Un cache peut maintenir certaines fonctions disponibles lorsque leur source est momentanément inaccessible.

Cette stratégie convient notamment aux référentiels stables et aux informations dont l'ancienneté maximale acceptable est connue.

Il faut définir :
- Une durée de validité.
- Les données pouvant être utilisées en mode dégradé.
- Le comportement lorsque le cache est vide.
- Les conséquences métier de données obsolètes.

Dans un moteur financier, un cache ne doit pas servir à contourner les exigences d'exactitude d'un calcul ou à utiliser des paramètres réglementaires périmés sans autorisation explicite.

### 4.5. Reporter les traitements non urgents

Certaines opérations peuvent être exécutées plus tard sans affecter la réponse principale.

Exemple :
- Enregistrer une notification dans une file persistante.
- Générer un document après la réponse initiale si le processus métier le permet.
- Reporter une synchronisation avec un système externe.

Le report doit être fiable et supervisé. Il faut prévoir les doublons, les échecs définitifs et les tentatives de reprise.

## 5. Exemple avec Spring Boot et Resilience4j

La Graceful Degradation est une stratégie d'architecture : elle ne correspond pas à une annotation unique.

Elle peut être mise en œuvre en combinant les mécanismes de Resilience4j avec la logique métier de l'application.

Exemple avec un Circuit Breaker :

```java
@Service
public class SimulationService {

    private final SimulationClient simulationClient;
    private final RecommandationClient recommandationClient;

    public SimulationService(
            SimulationClient simulationClient,
            RecommandationClient recommandationClient) {
        this.simulationClient = simulationClient;
        this.recommandationClient = recommandationClient;
    }

    public SimulationOutputDto simuler(SimulationInputDto input) {
        SimulationOutputDto resultat = simulationClient.simuler(input);

        List<RecommandationDto> recommandations =
                obtenirRecommandations(resultat);

        resultat.setRecommandations(recommandations);

        return resultat;
    }

    @CircuitBreaker(
        name = "recommandations",
        fallbackMethod = "recommandationsIndisponibles"
    )
    public List<RecommandationDto> obtenirRecommandations(
            SimulationOutputDto resultat) {
        return recommandationClient.getRecommandations(resultat);
    }

    private List<RecommandationDto> recommandationsIndisponibles(
            SimulationOutputDto resultat,
            Exception exception) {
        log.warn("Mode dégradé : recommandations indisponibles", exception);
        return List.of();
    }
}
```

Cet exemple illustre la séparation entre le calcul principal et la fonctionnalité secondaire.

**Attention à Spring AOP :** dans cet exemple, `obtenirRecommandations()` est appelée depuis une autre méthode de la même instance. Avec une interception par proxy Spring, cet appel interne peut contourner l'annotation `@CircuitBreaker`.

Pour que le Circuit Breaker soit effectivement appliqué, il est préférable de placer la méthode annotée dans un bean distinct et de l'appeler à travers son proxy Spring, ou d'utiliser explicitement un décorateur Resilience4j.

Par ailleurs, la signature du Fallback doit correspondre à la méthode principale et être compatible avec l'exception reçue.

## 6. Graceful Degradation et autres patterns

| Pattern | Rôle |
|---|---|
| Fallback | Fournir une réponse ou une solution alternative après un échec. |
| Circuit Breaker | Limiter les appels vers une dépendance qui échoue trop souvent. |
| Bulkhead | Isoler les ressources pour éviter qu'une dépendance sature l'application. |
| Timeout | Limiter le temps d'attente d'une opération. |
| Graceful Degradation | Définir quelles fonctionnalités restent disponibles lorsque le système est dégradé. |

La Graceful Degradation peut donc utiliser plusieurs de ces patterns.

Par exemple, une application peut :
1. Détecter un ralentissement grâce au Timeout.
2. Limiter les appels via le Circuit Breaker.
3. Utiliser un Fallback pour une fonctionnalité secondaire.
4. Continuer à fournir le service principal en mode dégradé.

## 7. Bonnes pratiques

### 7.1. Définir les modes dégradés à l'avance

Les fonctionnalités à conserver et celles à désactiver doivent être définies avant l'incident.

Pour chaque dépendance, documenter :
- Les fonctionnalités qui en dépendent.
- Le caractère obligatoire ou facultatif de ces fonctionnalités.
- Le comportement attendu en cas d'indisponibilité.
- Les données de secours éventuellement autorisées.
- Les conditions de retour au fonctionnement normal.

### 7.2. Préserver les invariants métier

Le mode dégradé ne doit pas violer les règles métier.

Exemple : si le calcul d'une capacité d'emprunt nécessite des taux et des paramètres fiables, l'application doit refuser de produire un résultat présenté comme définitif lorsque ces informations sont indisponibles.

Il vaut mieux retourner une indisponibilité explicite que fournir une réponse financière incorrecte.

### 7.3. Informer clairement les utilisateurs

Lorsque certaines fonctionnalités sont indisponibles, l'application doit fournir une information compréhensible.

Exemples :
- « Les recommandations sont temporairement indisponibles. »
- « La simulation est disponible, mais le document PDF ne peut pas être généré pour le moment. »
- « Cette opération ne peut pas être effectuée actuellement. Veuillez réessayer ultérieurement. »

Il faut éviter les messages génériques qui donnent l'impression que toute l'application est indisponible alors que seule une fonctionnalité secondaire est concernée.

### 7.4. Automatiser le retour au fonctionnement normal

La dégradation ne doit pas nécessairement rester active après le rétablissement du service.

Selon le mécanisme utilisé, le retour à la normale peut s'appuyer sur :
- La fermeture progressive du Circuit Breaker.
- La réussite des appels de contrôle.
- La récupération des dépendances.
- La validation des données actualisées.
- La reprise des traitements différés.

Le retour au fonctionnement normal doit être contrôlé afin d'éviter une surcharge brutale du service qui vient de récupérer.

### 7.5. Tester les scénarios de panne

Les tests doivent couvrir :
- L'indisponibilité d'une dépendance facultative.
- La lenteur d'une dépendance obligatoire.
- L'absence de données en cache.
- La saturation des ressources.
- Le rétablissement après une panne.
- La reprise des traitements différés.

Il est également utile de vérifier que les fonctionnalités essentielles restent disponibles lorsque les composants secondaires sont volontairement désactivés.

## 8. Limites

La Graceful Degradation ne garantit pas que l'application puisse toujours continuer à fonctionner.

Elle peut être impossible lorsque :
- Une dépendance est indispensable au calcul ou à la validation.
- Les données de secours ne sont pas fiables.
- Une règle de sécurité ou de conformité interdit le fonctionnement dégradé.
- Les ressources nécessaires au traitement principal sont elles-mêmes saturées.

Elle demande également une conception métier plus approfondie : toutes les fonctionnalités ne peuvent pas être remplacées par un simple résultat par défaut.

## 9. À retenir

La Graceful Degradation consiste à préserver les fonctionnalités essentielles lorsqu'une partie du système est indisponible.

Les points essentiels sont :

1. Identifier les fonctions critiques et les fonctions facultatives.
2. Définir les modes dégradés avant les incidents.
3. Utiliser les Fallbacks, caches et mécanismes de résilience appropriés.
4. Ne jamais sacrifier la justesse métier ou la sécurité pour maintenir artificiellement la disponibilité.
5. Tester le mode dégradé et le retour au fonctionnement normal.

**Règle d'or :** lorsqu'une partie du système échoue, conserver ce qui peut fonctionner correctement et désactiver ce qui ne peut plus être garanti.