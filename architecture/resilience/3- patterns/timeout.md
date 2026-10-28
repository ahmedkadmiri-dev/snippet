# Timeout — Limiter le temps d’attente d’une opération

## 1. Définition

Le **Timeout** est un mécanisme de résilience qui impose une durée maximale d’attente à une opération.

Lorsqu’un service appelle une API distante, une base de données ou un autre composant, il ne doit pas attendre indéfiniment une réponse. Si le délai configuré est dépassé, l’appel est considéré comme ayant expiré et une erreur est remontée au code appelant.

L’objectif est de :
- Éviter les requêtes bloquées trop longtemps.
- Libérer les ressources occupées par les appels en attente.
- Limiter la propagation des ralentissements entre services.
- Permettre l’activation d’autres mécanismes de résilience, comme le Retry ou le Circuit Breaker.

**Exemple :** le service `moteur-financier` appelle un service de tarification. Si ce dernier ne répond pas dans les deux secondes, l’appel expire au lieu de bloquer le traitement pendant une durée indéterminée.

## 2. Pourquoi utiliser un Timeout ?

Considérons une architecture composée de plusieurs services :

```text
Client
  |
  v
API Gateway
  |
  v
moteur-financier
  |
  v
Service de tarification
  |
  v
Base de données
```

Si la base de données devient lente, le service de tarification peut mettre plusieurs secondes à répondre. Les requêtes s’accumulent alors dans les services en amont.

Sans Timeout, les conséquences possibles sont :
- L’épuisement des threads disponibles.
- L’augmentation du nombre de requêtes simultanées.
- L’augmentation de la latence globale.
- La dégradation des autres fonctionnalités.
- Une indisponibilité progressive de toute la chaîne d’appels.

Le Timeout permet de borner l’attente, mais il ne garantit pas à lui seul que l’opération distante s’arrête réellement.

## 3. Les principaux types de Timeout

### 3.1. Connection Timeout

Le **Connection Timeout** définit le délai maximal pour établir une connexion avec le service distant.

Exemple :
- Le service cible est inaccessible.
- Le réseau ne répond pas.
- L’établissement de la connexion TCP prend trop de temps.

Si la connexion ne peut pas être établie dans le délai prévu, l’appel échoue.

### 3.2. Read Timeout

Le **Read Timeout** définit le délai maximal d’attente de données lors de la lecture de la réponse.

La connexion peut être établie correctement, mais le serveur peut mettre trop de temps à envoyer les données.

Exemple :

```text
moteur-financier
       |
       | Requête envoyée
       v
Service de tarification
       |
       | Traitement trop long
       v
Timeout de lecture
```

Selon le client HTTP utilisé, le Read Timeout peut représenter le délai maximal d'inactivité entre deux lectures, et non la durée totale de l'opération.

### 3.3. Response Timeout

Le **Response Timeout** désigne le délai maximal d'attente d'une réponse selon les garanties offertes par le client HTTP utilisé.

Sa sémantique dépend de l'implémentation : il ne faut pas supposer qu'il couvre automatiquement la résolution DNS, l'établissement de la connexion, l'envoi complet du corps ou toutes les phases de l'appel.

### 3.4. Timeout global ou Deadline

Un **Timeout global**, souvent appelé *deadline*, définit le temps maximal autorisé pour l'ensemble d'une opération.

Par exemple, si le traitement d'une requête doit durer au maximum trois secondes, ce budget doit tenir compte :
- De l'appel au premier service.
- Des éventuelles tentatives supplémentaires.
- Des délais d'attente entre les tentatives.
- Des autres appels effectués en cascade.
- Du temps nécessaire pour construire et retourner la réponse.

Il est donc important de distinguer le délai d'une connexion ou d'une lecture du budget global d'une requête.

## 4. Fonctionnement du Timeout

Le principe est simple :

```text
1. Le service lance un appel distant.
2. Un délai maximal est défini.
3. Le service attend la réponse.
4. Si la réponse arrive à temps :
      -> le traitement continue.
5. Si le délai est dépassé :
      -> une erreur de timeout est remontée.
      -> le code appelant décide de la suite.
```

La suite peut consister à :
- Retourner une erreur au client.
- Déclencher un Retry si l'opération peut être répétée sans risque.
- Utiliser une réponse de secours avec un Fallback.
- Laisser le Circuit Breaker comptabiliser l'échec.

Attention : une expiration côté client ne signifie pas nécessairement que le serveur distant a annulé son traitement. Celui-ci peut continuer à consommer des ressources et, dans certains cas, avoir déjà effectué une opération métier.

## 5. Configurer les Timeouts avec Spring Boot et OpenFeign

### 5.1. Configuration dans `application.yml`

Avec Spring Cloud OpenFeign, une configuration courante permet de définir les délais de connexion et de lecture :

```yaml
spring:
  cloud:
    openfeign:
      client:
        config:
          default:
            connectTimeout: 1000
            readTimeout: 2000
```

Dans cet exemple :
- `connectTimeout: 1000` : délai de connexion de 1 seconde.
- `readTimeout: 2000` : délai de lecture de 2 secondes.

Cette configuration s'applique aux clients Feign par défaut, selon la version et la configuration du projet.

Il est également possible de configurer un client spécifique :

```yaml
spring:
  cloud:
    openfeign:
      client:
        config:
          tarificationClient:
            connectTimeout: 1000
            readTimeout: 2000
```

Le nom `tarificationClient` doit correspondre au nom du client Feign concerné, par exemple à sa propriété `name` ou à son identifiant de configuration selon la version utilisée.

**Important :** les propriétés disponibles et leur comportement dépendent de la version de Spring Cloud OpenFeign et du client HTTP sous-jacent. Il faut vérifier la documentation correspondant aux versions du projet.

### 5.2. Exemple de client Feign

```java
@FeignClient(
    name = "tarificationClient",
    url = "${services.tarification.url}"
)
public interface TarificationClient {

    @GetMapping("/tarifications/{id}")
    TarificationDto getTarification(@PathVariable("id") Long id);
}
```

Le client utilise les délais configurés dans `application.yml`.

Lorsqu'un timeout survient, l'appel peut notamment remonter une exception telle que `RetryableException` dans une configuration OpenFeign courante. Le type exact dépend du client HTTP et du traitement des erreurs.

Le service appelant doit gérer cette situation de façon adaptée :

```java
public TarificationDto obtenirTarification(Long id) {
    try {
        return tarificationClient.getTarification(id);
    } catch (RetryableException exception) {
        log.warn("Timeout ou erreur réseau lors de l'appel à la tarification", exception);
        throw new ServiceUnavailableException(
            "Le service de tarification est temporairement indisponible"
        );
    }
}
```

Cet exemple illustre le traitement d'une erreur réseau ou de timeout ; il faut adapter le type d'exception et la traduction en erreur métier au client HTTP effectivement utilisé.

## 6. Timeout et Retry : comment les combiner ?

Le Timeout et le Retry sont complémentaires :
- Le **Timeout** limite la durée d'une tentative.
- Le **Retry** autorise de nouvelles tentatives après certains échecs.

Exemple :

```text
Tentative 1 : timeout après 1 seconde
       |
       v
Attente avant retry : 200 ms
       |
       v
Tentative 2 : timeout après 1 seconde
       |
       v
Échec final
```

Dans cet exemple, le traitement consomme déjà au moins 2,2 secondes, sans compter les autres délais éventuels.

Il faut donc définir un budget global cohérent.

Si une requête doit répondre en trois secondes, configurer trois tentatives de deux secondes chacune est incohérent : les tentatives peuvent dépasser le budget attendu.

Bonnes pratiques :
- Définir un Timeout pour chaque appel distant.
- Limiter le nombre de tentatives.
- Prévoir un budget global pour la requête.
- Tenir compte des délais d'attente entre les tentatives.
- Ne répéter que les opérations dont la répétition est acceptable.
- Éviter les retries simultanés ou excessifs lorsque le service est déjà surchargé.

## 7. Timeout et Circuit Breaker

Les deux mécanismes répondent à des problèmes différents.

| Mécanisme | Objectif |
|---|---|
| Timeout | Limiter la durée d'attente d'un appel. |
| Circuit Breaker | Empêcher temporairement les appels vers un service qui échoue trop souvent. |

Exemple :

```text
moteur-financier
       |
       v
Circuit Breaker
       |
       v
Timeout HTTP
       |
       v
Service distant
```

Le Timeout détecte qu'une tentative dure trop longtemps. Le Circuit Breaker peut comptabiliser cet échec et, selon ses seuils, empêcher les appels suivants pendant une période donnée.

L'ordre exact des décorateurs ou des composants de résilience influence le comportement. Il doit être choisi en fonction du budget de temps, du comptage des échecs et de la stratégie de fallback.

## 8. Bonnes pratiques

### 8.1. Adapter les délais au type d'opération

Un appel de lecture simple ne nécessite généralement pas le même budget qu'une opération métier complexe.

Exemples indicatifs à ajuster selon les mesures réelles :

| Type d'opération | Exemple de délai |
|---|---:|
| Appel de référentiel rapide | 500 ms à 1 s |
| API métier classique | 1 à 3 s |
| Calcul complexe | Plusieurs secondes selon le besoin |
| Traitement asynchrone long | Privilégier un mécanisme asynchrone |

Ces valeurs sont des points de départ, pas des normes universelles.

### 8.2. Respecter les budgets de temps en cascade

Si le Gateway dispose d'un budget de cinq secondes, le service appelé ne devrait pas consommer à lui seul cinq secondes avant de renvoyer une réponse.

Il faut réserver du temps pour :
- Les appels en cascade.
- Le traitement métier.
- Les éventuelles tentatives supplémentaires.
- La construction de la réponse.

### 8.3. Ne pas confondre Timeout et annulation

Un timeout côté client arrête ou interrompt l'attente selon l'implémentation. Il ne garantit pas l'annulation de l'exécution distante.

Pour limiter les traitements inutiles, il peut être nécessaire de propager des deadlines, d'utiliser des mécanismes d'annulation ou de concevoir les traitements longs de manière asynchrone.

### 8.4. Mesurer les Timeouts

Il est recommandé de surveiller :
- Le nombre de timeouts par service.
- Le taux d'échec des appels.
- La latence moyenne et les percentiles, notamment p95 et p99.
- Le nombre de tentatives supplémentaires.
- Les délais de réponse des dépendances.

Un nombre croissant de timeouts peut signaler un problème réseau, une saturation du serveur distant ou une configuration de délais inadaptée.

## 9. Limites du Timeout

Le Timeout ne résout pas toutes les causes d'indisponibilité.

Il ne garantit pas :
- Que le service distant soit réparé.
- Que l'opération distante soit annulée.
- Que la requête puisse être répétée sans effet de bord.
- Que le système reste disponible si toutes les dépendances échouent.

Il doit être combiné, selon le contexte, avec le Retry, le Circuit Breaker, le Bulkhead, le Fallback et l'observabilité.

## 10. À retenir

Le Timeout est un mécanisme fondamental de résilience qui permet de contrôler le temps d'attente des appels distants.

Les points essentiels sont :

1. Définir des délais explicites pour les connexions et les réponses.
2. Distinguer les délais techniques du budget global d'une requête.
3. Coordonner Timeout et Retry pour éviter des traitements trop longs.
4. Ne pas considérer une expiration comme une annulation garantie du traitement distant.
5. Mesurer les timeouts pour ajuster les configurations en fonction du comportement réel du système.

**Règle d'or :** aucun appel distant ne devrait pouvoir bloquer indéfiniment un traitement métier.