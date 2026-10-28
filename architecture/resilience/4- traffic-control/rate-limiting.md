# Rate Limiting — Limiter le nombre de requêtes

## 1. Définition

Le **Rate Limiting** est un mécanisme de contrôle du trafic qui limite le nombre de requêtes qu'un client, un utilisateur ou un service peut effectuer pendant une période donnée.

Il permet de protéger une application contre les pics de trafic, les abus, les appels excessifs et certaines formes de surcharge.

Exemple : une API peut autoriser un client à effectuer au maximum 100 requêtes par minute. Au-delà de cette limite, les nouvelles requêtes sont rejetées ou retardées selon la stratégie choisie.

**Objectifs principaux :**
- Protéger les ressources de l'application.
- Limiter la surcharge des services et des bases de données.
- Garantir une répartition plus équitable des ressources.
- Contrôler le trafic entrant.
- Réduire certains risques d'abus et de déni de service.

Le Rate Limiting est principalement un mécanisme de contrôle du trafic. Il contribue à la résilience, mais ne remplace pas les Timeouts, les Bulkheads ou les Circuit Breakers.

## 2. Pourquoi utiliser le Rate Limiting ?

Considérons une API bancaire exposant plusieurs fonctionnalités :

```text
                    Clients
                       |
                       v
                  API Gateway
                       |
                       v
                 Rate Limiting
                       |
          +------------+------------+
          |            |            |
          v            v            v
      Simulation   Référentiel   Authentification
          |            |            |
          v            v            v
        Oracle      PostgreSQL   Service IAM
```

Supposons qu'un client envoie des milliers de requêtes de simulation en quelques secondes.

Sans limitation :
- Les threads peuvent être saturés.
- Les connexions à la base de données peuvent être épuisées.
- La latence augmente pour les autres utilisateurs.
- Les coûts d'infrastructure peuvent augmenter.
- Les services dépendants peuvent être surchargés.

Avec un Rate Limiting, le trafic excessif est limité avant d'atteindre les ressources critiques.

## 3. Les principales stratégies de Rate Limiting

### 3.1. Fixed Window

La stratégie *Fixed Window* divise le temps en fenêtres fixes.

Exemple : maximum 100 requêtes par minute.

```text
Fenêtre 1 : 10:00:00 - 10:00:59
Limite    : 100 requêtes

Fenêtre 2 : 10:01:00 - 10:01:59
Limite    : 100 requêtes
```

**Avantages :**
- Facile à comprendre et à implémenter.
- Faible coût de calcul.
- Compteur simple à gérer.

**Limites :**

Un client peut envoyer un grand nombre de requêtes à la fin d'une fenêtre puis un autre grand nombre au début de la suivante. Le trafic instantané peut donc dépasser la limite intuitive attendue.

### 3.2. Sliding Window

La stratégie *Sliding Window* prend en compte une fenêtre temporelle glissante.

Au lieu de réinitialiser simplement le compteur à une heure fixe, elle contrôle les requêtes sur une période qui se déplace avec le temps.

Exemple : maximum 100 requêtes sur les 60 dernières secondes.

**Avantages :**
- Limitation plus régulière du trafic.
- Réduction des pics liés aux frontières entre fenêtres.

**Limites :**
- Implémentation potentiellement plus complexe.
- Coût de stockage et de calcul dépendant de l'algorithme.

### 3.3. Token Bucket

Le *Token Bucket* utilise un réservoir de jetons.

Chaque requête consomme un jeton. Les jetons sont ajoutés progressivement jusqu'à une capacité maximale.

Exemple :
- Capacité du réservoir : 20 jetons.
- Régénération : 5 jetons par seconde.
- Une requête consomme un jeton.

Un client peut envoyer un petit burst de requêtes si des jetons sont disponibles, mais son trafic soutenu est limité par le rythme de régénération.

**Avantages :**
- Autorise des pointes de trafic contrôlées.
- Limite le débit moyen.
- Convient à de nombreuses API.

### 3.4. Leaky Bucket

Le *Leaky Bucket* traite les requêtes selon un débit contrôlé, comme un réservoir qui se vide à une vitesse constante.

Selon l'implémentation, les requêtes peuvent être mises en file d'attente ou rejetées lorsque la capacité est dépassée.

**Avantages :**
- Permet de lisser le trafic.
- Utile lorsque le service cible doit recevoir les requêtes à un rythme stable.

**Limites :**
- Une file d'attente peut augmenter la latence.
- Une file non bornée peut consommer trop de mémoire.
- Les requêtes peuvent être rejetées si la capacité est atteinte.

## 4. Rate Limiting et autres mécanismes de résilience

| Mécanisme | Objectif |
|---|---|
| Rate Limiting | Limiter le nombre ou le débit des requêtes entrantes. |
| Bulkhead | Limiter les ressources consommées par une dépendance. |
| Timeout | Limiter la durée d'attente d'une opération. |
| Circuit Breaker | Éviter les appels répétés vers un service qui échoue. |
| Backpressure | Adapter le flux de données à la capacité de traitement du consommateur. |

Ces mécanismes sont complémentaires.

Par exemple, un Rate Limiting peut empêcher un client d'envoyer trop de requêtes, tandis qu'un Bulkhead protège les ressources internes si les requêtes acceptées deviennent coûteuses.

## 5. Implémentation avec Spring Boot et Resilience4j

Resilience4j fournit un module `RateLimiter` permettant de contrôler le nombre d'autorisations disponibles pendant des périodes définies.

### 5.1. Dépendance Maven

```xml
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
</dependency>
```

La version doit être compatible avec la version de Spring Boot du projet. L'intégration par annotation nécessite également la configuration AOP adaptée.

### 5.2. Configuration dans `application.yml`

```yaml
resilience4j:
  ratelimiter:
    instances:
      simulation:
        limitForPeriod: 10
        limitRefreshPeriod: 1s
        timeoutDuration: 0
```

Dans cet exemple :
- `limitForPeriod: 10` autorise dix permissions par période.
- `limitRefreshPeriod: 1s` définit une période de renouvellement d'une seconde.
- `timeoutDuration: 0` n'accorde aucun délai d'attente pour obtenir une permission.

Il s'agit d'un exemple de configuration, pas d'une recommandation universelle. Les valeurs doivent être adaptées à la capacité du service et aux exigences métier.

Attention : le RateLimiter de Resilience4j contrôle les permissions au niveau de l'application. Il ne constitue pas automatiquement un quota global partagé entre toutes les instances d'un déploiement.

### 5.3. Utilisation avec `@RateLimiter`

```java
@RestController
@RequestMapping("/simulations")
public class SimulationController {

    @GetMapping("/{id}")
    @RateLimiter(name = "simulation")
    public SimulationDto getSimulation(@PathVariable Long id) {
        return simulationService.getSimulation(id);
    }
}
```

Dans cet exemple, le RateLimiter limite les invocations de la méthode protégée.

Lorsque le nombre de permissions disponibles est épuisé, une exception de limitation peut être levée, notamment `RequestNotPermitted`.

Pour une API publique, il faut traduire cette exception en une réponse HTTP adaptée, généralement `429 Too Many Requests`, au moyen d'un gestionnaire d'exceptions.

L'annotation dépend de l'interception Spring AOP : l'appel doit passer par le proxy correspondant.

**Important :** cette configuration protège les invocations de la méthode dans une instance de l'application. Pour appliquer une limite par utilisateur, par clé API ou globalement à travers plusieurs instances, une solution supplémentaire peut être nécessaire.

## 6. Exemple de réponse HTTP 429

Lorsque la limite est dépassée, une API peut retourner :

```http
HTTP/1.1 429 Too Many Requests
Content-Type: application/json
Retry-After: 1
```

```json
{
  "errorCode": "RATE_LIMIT_EXCEEDED",
  "message": "Trop de requêtes. Veuillez réessayer ultérieurement."
}
```

Le header `Retry-After` peut indiquer au client quand réessayer, lorsque cette information est connue.

Le délai fourni doit être cohérent avec la politique de limitation réellement appliquée. Il ne faut pas annoncer un délai arbitraire.

## 7. Où appliquer le Rate Limiting ?

### 7.1. Au niveau de l'API Gateway

C'est souvent le meilleur emplacement pour limiter le trafic entrant avant qu'il ne consomme les ressources des services métier.

Exemples :
- Limiter les requêtes par adresse IP.
- Appliquer des quotas par clé API.
- Limiter les appels d'un utilisateur authentifié.
- Définir des limites différentes selon les endpoints.

Avantage : le trafic excessif peut être rejeté avant d'atteindre les services backend.

### 7.2. Au niveau du service métier

Un Rate Limiter local peut protéger une opération particulièrement coûteuse, même si le trafic a déjà traversé la Gateway.

Exemples :
- Calculs financiers intensifs.
- Génération de documents.
- Requêtes de recherche complexes.
- Appels à un fournisseur externe limité en capacité.

### 7.3. Au niveau du fournisseur externe

Si une API tierce impose son propre quota, l'application doit respecter cette limite.

Il peut être nécessaire de partager le quota entre plusieurs instances de l'application, de contrôler les appels sortants et de traiter les réponses `429` du fournisseur.

## 8. Rate Limiting distribué

Dans une application déployée sur plusieurs instances, chaque instance peut avoir son propre compteur.

Exemple :

```text
                API Gateway
                     |
          +----------+----------+
          |          |          |
          v          v          v
       Instance A Instance B Instance C
       10 req/s   10 req/s   10 req/s
```

Avec un Rate Limiter local, la limite théorique cumulée peut atteindre 30 requêtes par seconde si le trafic est réparti sur les trois instances et que chaque instance autorise dix requêtes par seconde.

Si l'objectif est de limiter un client à dix requêtes par seconde sur l'ensemble du système, un compteur local ne suffit pas nécessairement.

Les solutions possibles comprennent :
- Un Rate Limiter distribué.
- Un stockage partagé pour les compteurs.
- Une limitation centralisée à la Gateway.
- Une répartition explicite des quotas entre les instances.

Le choix dépend des exigences de cohérence, de la latence acceptable et de la disponibilité du composant chargé de contrôler les quotas.

## 9. Bonnes pratiques

### 9.1. Définir des quotas adaptés

Les limites peuvent varier selon :
- Le type d'utilisateur.
- Le niveau d'abonnement.
- La clé API.
- Le type d'opération.
- La capacité du service cible.

Une consultation de référentiel peu coûteuse et un calcul financier complexe ne nécessitent pas nécessairement les mêmes quotas.

### 9.2. Ne pas utiliser uniquement l'adresse IP

Une adresse IP peut être partagée par de nombreux utilisateurs, notamment derrière un proxy ou un réseau d'entreprise.

Lorsque cela est pertinent, utiliser une identité authentifiée ou une clé API pour appliquer les quotas.

Il faut également éviter de faire confiance aveuglément à un header transmis par le client pour identifier son adresse IP réelle.

### 9.3. Prévoir des limites spécifiques aux opérations coûteuses

Les endpoints qui mobilisent fortement le CPU, la mémoire ou la base de données peuvent nécessiter des quotas plus stricts.

Il est également utile de combiner Rate Limiting, Timeout et Bulkhead.

### 9.4. Gérer correctement les clients limités

Une API devrait :
- Retourner un code HTTP cohérent.
- Fournir un message d'erreur compréhensible.
- Indiquer un délai de reprise lorsque cela est possible.
- Documenter les quotas applicables.
- Éviter les tentatives automatiques immédiates qui aggraveraient la surcharge.

### 9.5. Surveiller les limites

Les métriques utiles incluent :
- Le nombre de requêtes acceptées.
- Le nombre de requêtes rejetées.
- Le trafic par endpoint.
- Les clients qui dépassent fréquemment leur quota.
- Les réponses `429`.
- La latence des services protégés.

Les logs ne doivent pas exposer inutilement les clés API ou les données sensibles utilisées pour identifier les clients.

## 10. Limites

Le Rate Limiting ne garantit pas à lui seul la disponibilité du système.

Il peut :
- Rejeter des requêtes légitimes si les quotas sont trop stricts.
- Ne pas protéger suffisamment le service si les quotas sont trop élevés.
- Produire des limites incohérentes si les compteurs sont locaux alors qu'un quota global est requis.
- Ajouter une dépendance opérationnelle lorsqu'un stockage distribué est utilisé.

Il faut également distinguer le Rate Limiting des mécanismes de sécurité plus larges : il peut réduire certains abus, mais ne remplace pas les protections contre les attaques distribuées ni les contrôles d'authentification et d'autorisation.

## 11. À retenir

Le Rate Limiting contrôle le volume de requêtes qu'un client ou une application peut effectuer sur une période donnée.

Les points essentiels sont :

1. Choisir une stratégie adaptée : Fixed Window, Sliding Window, Token Bucket ou Leaky Bucket.
2. Définir les quotas en fonction de la capacité réelle des services.
3. Appliquer les limites au bon niveau : Gateway, service métier ou appels sortants.
4. Distinguer les compteurs locaux des quotas distribués.
5. Retourner une réponse HTTP `429` lorsque la politique de l'API le prévoit.
6. Surveiller les rejets pour ajuster les quotas.

**Règle d'or :** contrôler le trafic avant qu'il ne dépasse la capacité de traitement du système.