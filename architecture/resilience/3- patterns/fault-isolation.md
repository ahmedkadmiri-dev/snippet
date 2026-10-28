# Fault Isolation — Isoler les pannes entre composants

## 1. Définition

La **Fault Isolation**, ou isolation des pannes, est un principe d'architecture qui consiste à limiter la propagation d'une défaillance d'un composant vers les autres composants du système.

L'objectif est de faire en sorte qu'une panne reste confinée à une zone aussi limitée que possible, plutôt que de provoquer une dégradation générale de l'application.

Ce principe peut être appliqué à plusieurs niveaux :
- Entre les microservices.
- Entre les modules d'une application.
- Entre les pools de threads.
- Entre les connexions aux bases de données.
- Entre les processus et les infrastructures.
- Entre les fonctionnalités critiques et les fonctionnalités secondaires.

**Objectif principal :** empêcher qu'une défaillance locale entraîne une panne généralisée.

## 2. Pourquoi isoler les pannes ?

Considérons une architecture bancaire composée de plusieurs services :

```text
                    API Gateway
                         |
                         v
                  moteur-financier
                         |
             +-----------+-----------+
             |           |           |
             v           v           v
        Simulation   Référentiel   Notification
             |           |           |
             v           v           v
         Oracle       PostgreSQL   Service SMS
```

Supposons que le service SMS devienne indisponible.

Sans isolation, les requêtes de notification peuvent occuper les threads du moteur financier, provoquer des attentes prolongées et affecter les simulations.

Avec une bonne isolation :
- Les appels SMS sont limités.
- Les ressources du calcul financier restent disponibles.
- Les notifications peuvent être reportées.
- Les erreurs du service SMS ne compromettent pas les opérations indépendantes.

L'objectif n'est pas d'empêcher toutes les pannes, mais d'en limiter le rayon d'impact, appelé *blast radius*.

## 3. Les principaux mécanismes d'isolation

### 3.1. Isolation par service

Chaque microservice possède son propre processus et son propre cycle de vie.

Une panne du service de notification ne provoque donc pas nécessairement l'arrêt du moteur financier.

Cependant, cette séparation ne suffit pas à garantir l'isolation : les services peuvent toujours partager une base de données, un réseau, un système d'authentification ou d'autres ressources communes.

### 3.2. Isolation des ressources avec Bulkhead

Le Bulkhead limite la quantité de ressources qu'une dépendance peut consommer.

Exemple :

```text
                 moteur-financier
                        |
             +----------+----------+
             |          |          |
             v          v          v
         Pool A      Pool B      Pool C
         Simulation  Référentiel Notification
```

Si le pool de notification est saturé, les autres pools peuvent continuer à traiter leurs propres requêtes.

Le Bulkhead est donc l'un des mécanismes les plus courants pour concrétiser l'isolation des pannes au niveau des ressources d'exécution.

### 3.3. Isolation avec Circuit Breaker

Le Circuit Breaker limite les appels vers une dépendance qui échoue de façon répétée.

Lorsqu'un circuit est ouvert, les appels peuvent être refusés immédiatement au lieu de continuer à solliciter un service défaillant.

Cela évite notamment que des appels voués à l'échec consomment inutilement des ressources.

Le Circuit Breaker ne constitue toutefois pas une isolation complète : il doit être associé à des limites de ressources, des délais d'attente et une gestion adaptée des erreurs.

### 3.4. Isolation avec Timeout

Un Timeout limite le temps d'attente d'un appel.

Il permet de réduire le risque que des threads ou des connexions restent mobilisés trop longtemps par une dépendance lente.

Attention : un timeout côté client ne garantit pas l'annulation du traitement côté serveur. Une isolation efficace nécessite donc également de contrôler les ressources et les traitements du service distant.

### 3.5. Isolation des données

Les données peuvent également être isolées pour réduire les effets d'une panne ou d'une surcharge.

Exemples :
- Utiliser des schémas ou bases de données distincts pour certains domaines.
- Limiter les droits d'accès de chaque service.
- Éviter qu'une requête coûteuse bloque toutes les opérations d'une base partagée.
- Séparer les ressources transactionnelles des traitements analytiques.

Cette isolation doit être conçue en tenant compte des contraintes de cohérence, de sécurité et de transaction.

### 3.6. Isolation au niveau infrastructure

L'isolation peut s'appuyer sur :
- Des déploiements indépendants.
- Des limites CPU et mémoire par conteneur.
- Des pools de connexions distincts.
- Des quotas de ressources.
- Des réseaux et politiques de sécurité adaptés.
- Des zones de disponibilité différentes.

Il faut néanmoins identifier les ressources partagées, car un composant commun saturé peut devenir un point de défaillance pour plusieurs services.

## 4. Exemple d'architecture avec isolation des pannes

Prenons le cas d'un moteur de simulation financière.

```text
                       Client
                         |
                         v
                    API Gateway
                         |
                         v
                  moteur-financier
                         |
          +--------------+--------------+
          |              |              |
          v              v              v
     Simulation     Référentiel     Notification
          |              |              |
          v              v              v
     Ressources      Cache dédié    File persistante
      dédiées                         ou service SMS
```

Les mécanismes d'isolation possibles sont les suivants :

- La simulation possède ses propres ressources d'exécution.
- Les appels au référentiel sont limités et peuvent utiliser un cache si les règles métier l'autorisent.
- Les notifications peuvent être traitées de manière asynchrone.
- Les appels externes disposent de Timeouts.
- Les dépendances défaillantes sont protégées par des Circuit Breakers lorsque cela est pertinent.

Ainsi, une panne du service SMS ne doit pas nécessairement empêcher le calcul d'une simulation.

En revanche, si le référentiel contient des paramètres indispensables au calcul, son indisponibilité peut obliger l'application à refuser la simulation plutôt qu'à produire un résultat incertain.

## 5. Mise en œuvre avec Spring Boot et Resilience4j

La Fault Isolation est un principe architectural, et non une annotation unique.

Avec Spring Boot, plusieurs mécanismes peuvent être combinés pour la mettre en œuvre.

### 5.1. Exemple avec un Bulkhead

Configuration :

```yaml
resilience4j:
  bulkhead:
    instances:
      notification:
        maxConcurrentCalls: 5
        maxWaitDuration: 0
```

Utilisation :

```java
@Service
public class NotificationService {

    private final NotificationClient notificationClient;

    public NotificationService(NotificationClient notificationClient) {
        this.notificationClient = notificationClient;
    }

    @Bulkhead(name = "notification", type = Bulkhead.Type.SEMAPHORE)
    public void envoyerNotification(NotificationDto notification) {
        notificationClient.envoyer(notification);
    }
}
```

Dans cet exemple, le Bulkhead limite à cinq le nombre d'exécutions simultanées protégées par cette configuration.

Si la limite est atteinte, une nouvelle invocation peut être rejetée au lieu d'attendre indéfiniment.

L'annotation nécessite l'intégration Spring AOP appropriée et un appel passant par le proxy Spring. La méthode doit également être conçue pour traiter correctement les erreurs de saturation.

### 5.2. Ajouter un Circuit Breaker

Configuration illustrative :

```yaml
resilience4j:
  circuitbreaker:
    instances:
      notification:
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
```

Cette configuration définit notamment :
- Une fenêtre d'observation de dix appels.
- Un minimum de cinq appels avant d'évaluer le taux d'échec.
- Un seuil de 50 % d'échecs pour ouvrir le circuit.
- Une attente de dix secondes avant la transition permettant de tester le rétablissement.

Les seuils doivent être adaptés au trafic et au profil du service. Le Circuit Breaker et le Bulkhead sont complémentaires : le premier limite les appels vers une dépendance défaillante, tandis que le second limite la consommation de ressources.

### 5.3. Isoler les notifications du parcours principal

Lorsque les notifications ne sont pas nécessaires à la réponse immédiate, une architecture asynchrone peut être préférable :

```text
Requête de simulation
        |
        v
Calcul financier
        |
        +------> Retour du résultat
        |
        v
Enregistrement de la notification
        |
        v
File de traitement
        |
        v
Worker de notification
        |
        v
Service SMS
```

Cette architecture évite que la latence du fournisseur SMS bloque directement le calcul.

Pour garantir la fiabilité, il faut prévoir une file durable, des reprises, une gestion des doublons et une supervision des messages en échec. Un mécanisme d'outbox transactionnelle peut être utile lorsque l'enregistrement métier et la publication de l'événement doivent rester cohérents.

## 6. Fault Isolation, Bulkhead et Fault Tolerance

Ces notions sont liées, mais ne désignent pas la même chose.

| Concept | Objectif |
|---|---|
| Fault Isolation | Limiter la propagation d'une panne. |
| Bulkhead | Isoler et limiter les ressources consommées par une dépendance. |
| Fault Tolerance | Permettre au système de continuer à fonctionner malgré certaines défaillances. |
| Circuit Breaker | Empêcher temporairement les appels vers une dépendance qui échoue trop souvent. |
| Graceful Degradation | Préserver les fonctions essentielles en réduisant certaines fonctionnalités. |

La Fault Isolation contribue à la Fault Tolerance, mais ne garantit pas à elle seule la continuité du service.

Par exemple, isoler un service de tarification empêche sa saturation de consommer les ressources des autres services. Cela ne permet pas automatiquement de calculer une tarification si ce service est indispensable et indisponible.

## 7. Bonnes pratiques

### 7.1. Identifier les points de défaillance communs

Les microservices peuvent partager :
- Une base de données.
- Un serveur d'authentification.
- Une plateforme de messages.
- Un DNS ou un réseau.
- Un système de fichiers.
- Un fournisseur externe.

Ces composants peuvent devenir des points de défaillance communs, même lorsque les services sont déployés séparément.

### 7.2. Limiter les ressources consommées

Définir des limites adaptées pour :
- Les threads.
- Les connexions HTTP.
- Les connexions aux bases de données.
- Les tâches simultanées.
- Les files d'attente.
- La mémoire et le CPU.

Les limites doivent être cohérentes entre les niveaux applicatif et infrastructure.

### 7.3. Éviter les dépendances synchrones inutiles

Si une opération secondaire n'est pas nécessaire à la réponse principale, envisager un traitement asynchrone.

Cela réduit le nombre de dépendances dont la disponibilité conditionne directement le parcours utilisateur.

### 7.4. Tester la propagation des pannes

Les tests de résilience doivent vérifier qu'une dépendance lente, saturée ou indisponible ne dégrade pas excessivement les autres fonctionnalités.

Il est possible de simuler :
- Des erreurs HTTP.
- Des timeouts.
- Une saturation de pool.
- Une indisponibilité de base de données.
- Une interruption du système de messagerie.

Ces tests permettent de mesurer le rayon d'impact réel et de découvrir les ressources partagées qui ne sont pas correctement isolées.

### 7.5. Superviser les mécanismes d'isolation

Surveiller :
- Les erreurs par dépendance.
- Les appels rejetés par les Bulkheads.
- Les timeouts.
- L'état des Circuit Breakers.
- La saturation des pools.
- La taille des files d'attente.
- Les ressources CPU et mémoire.

L'objectif est de détecter une dégradation avant qu'elle ne se propage à l'ensemble du système.

## 8. Limites

La Fault Isolation n'empêche pas toutes les pannes.

Ses limites incluent :
- Les dépendances partagées entre plusieurs composants.
- Les ressources communes saturées.
- Les erreurs de conception qui propagent des états incohérents.
- Les limites de capacité mal configurées.
- Le coût de maintenance de mécanismes d'isolation supplémentaires.

Elle doit être pensée à plusieurs niveaux : application, données, réseau et infrastructure.

## 9. À retenir

La Fault Isolation vise à contenir les pannes et à réduire leur impact sur le reste du système.

Les points essentiels sont :

1. Isoler les ressources et les dépendances critiques.
2. Combiner Bulkhead, Timeout et Circuit Breaker selon les besoins.
3. Utiliser l'asynchronisme pour les traitements secondaires lorsque cela est pertinent.
4. Identifier les ressources partagées susceptibles de propager une panne.
5. Tester les scénarios de défaillance et mesurer leur rayon d'impact.

**Règle d'or :** la panne d'un composant ne doit pas devenir la panne de toute l'application.