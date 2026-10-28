# Health Check — Vérification de l'état de santé d'un service

## 1. Définition

Un **Health Check** est un mécanisme qui permet de vérifier si une application ou un service est dans un état lui permettant de fonctionner correctement.

Il est utilisé pour surveiller les applications, détecter les défaillances et permettre aux plateformes d'orchestration de prendre des décisions adaptées, comme redémarrer un conteneur ou retirer temporairement une instance du trafic.

Dans une architecture de microservices, chaque service doit pouvoir exposer des informations fiables sur son état de fonctionnement.

Par exemple, une application Spring Boot peut être démarrée, mais incapable de traiter correctement les requêtes parce que sa connexion à la base de données est indisponible.

Le Health Check permet de distinguer ces situations.

## 2. Pourquoi utiliser un Health Check ?

Considérons une architecture composée d'une Gateway et de plusieurs microservices.

```text
                    Clients
                       |
                       v
                 API Gateway
                       |
             +---------+---------+
             |                   |
             v                   v
       Service A             Service B
             |                   |
             v                   v
         Database             Service C
```

Si le service B devient indisponible, la Gateway ou la plateforme d'orchestration doit pouvoir détecter le problème.

Sans Health Check, une instance défaillante peut continuer à recevoir des requêtes, même si elle n'est plus capable de les traiter correctement.

Le Health Check permet notamment de :

- Détecter les applications bloquées.
- Vérifier la disponibilité des ressources essentielles.
- Identifier les problèmes de connexion à une base de données.
- Retirer du trafic les instances temporairement incapables de servir les requêtes.
- Déclencher un redémarrage lorsqu'une application ne répond plus.
- Améliorer la supervision de l'infrastructure.

Il ne remplace toutefois ni les métriques ni les tests fonctionnels complets.

## 3. Les trois principaux types de Health Check

### 3.1. Liveness : l'application est-elle toujours vivante ?

Le contrôle de **liveness** indique si le processus de l'application fonctionne suffisamment correctement pour continuer à s'exécuter.

Si le contrôle échoue durablement, une plateforme comme Kubernetes peut décider de redémarrer le conteneur.

Exemple :

```text
Application démarrée
        |
        v
Liveness Check
        |
        +---- OK ------> Continuer l'exécution
        |
        +---- KO ------> Redémarrage éventuel
```

Un contrôle de liveness doit généralement rester indépendant des dépendances externes.

Par exemple, une indisponibilité temporaire de la base de données ne doit pas nécessairement provoquer le redémarrage de toutes les instances de l'application. Cela pourrait aggraver la panne.

### 3.2. Readiness : l'application peut-elle recevoir du trafic ?

Le contrôle de **readiness** indique si une instance est prête à traiter des requêtes.

Une application peut être vivante sans être encore prête, par exemple pendant son initialisation ou lorsqu'une ressource indispensable à son fonctionnement n'est pas disponible.

```text
Application démarrée
        |
        v
Readiness Check
        |
        +---- OK ------> Instance disponible pour le trafic
        |
        +---- KO ------> Instance retirée du trafic
```

Une instance non prête n'a pas nécessairement besoin d'être redémarrée.

C'est la distinction essentielle entre liveness et readiness.

### 3.3. Startup : l'application a-t-elle terminé son initialisation ?

Le contrôle de **startup** permet de gérer les applications dont le démarrage peut prendre beaucoup de temps.

Il est particulièrement utile pour les applications qui doivent :

- Charger des référentiels volumineux.
- Initialiser des composants.
- Effectuer des migrations ou des vérifications de démarrage.
- Préparer des caches nécessaires au traitement des requêtes.

Tant que le contrôle de startup n'a pas réussi, Kubernetes peut différer l'application des contrôles de liveness et de readiness associés à cette configuration.

## 4. Liveness, Readiness et Startup : comparaison

| Contrôle | Question | Action typique en cas d'échec |
|---|---|---|
| Liveness | L'application peut-elle continuer à fonctionner ? | Redémarrer l'instance selon la politique de la plateforme |
| Readiness | L'instance peut-elle recevoir du trafic ? | Ne plus lui envoyer de nouvelles requêtes |
| Startup | L'initialisation est-elle terminée ? | Continuer à attendre ou appliquer la politique de redémarrage |

Ces trois contrôles sont complémentaires.

Il est important de ne pas utiliser un seul endpoint pour répondre à toutes les questions : une instance peut être vivante, mais non prête à recevoir du trafic.

## 5. Implémentation avec Spring Boot Actuator

Spring Boot fournit le module Actuator, qui expose des endpoints de supervision.

### 5.1. Ajouter la dépendance Maven

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

Actuator permet notamment d'exposer les informations de santé de l'application.

### 5.2. Configurer les endpoints

Dans `application.yml` :

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      probes:
        enabled: true
      show-details: never
```

Cette configuration :

- Expose les endpoints `health` et `info`.
- Active les groupes de santé utilisés pour les probes.
- Évite d'afficher les détails de santé dans la réponse publique.

En environnement de production, il faut également contrôler l'accès aux endpoints de supervision et vérifier qu'ils ne divulguent aucune information sensible.

### 5.3. Les endpoints de santé

Avec cette configuration, les endpoints suivants sont disponibles par défaut sur le port de gestion :

```text
/actuator/health
/actuator/health/liveness
/actuator/health/readiness
```

Les URL exactes dépendent de la configuration des endpoints et du contexte HTTP de l'application.

Exemple de réponse :

```json
{
  "status": "UP"
}
```

Les états courants incluent :

- `UP` : le contrôle est considéré comme satisfaisant.
- `DOWN` : le contrôle a détecté un problème.
- `OUT_OF_SERVICE` : le composant ne doit pas être utilisé.
- `UNKNOWN` : l'état n'a pas pu être déterminé.

Le résultat global dépend des indicateurs de santé inclus dans le contrôle concerné.

## 6. Vérifier les dépendances externes

Une application dépend souvent de plusieurs composants :

- Une base de données Oracle ou PostgreSQL.
- Un service REST distant.
- Un broker de messages.
- Un cache distribué.
- Un fournisseur d'identité.

Actuator peut intégrer des contrôles de santé pour certaines de ces dépendances lorsque les intégrations correspondantes sont présentes.

Par exemple, un contrôle de santé de base de données peut détecter une perte de connexion.

Cependant, il faut distinguer deux objectifs :

- **Santé de l'application** : le processus peut-il fonctionner ?
- **Disponibilité des dépendances** : les services dont il dépend sont-ils accessibles ?

Une indisponibilité externe ne doit pas systématiquement déclencher un redémarrage de l'application.

Pour une dépendance indispensable au traitement des requêtes, la readiness peut tenir compte de son état. Pour une dépendance facultative, il peut être préférable de laisser l'instance disponible et d'utiliser un mécanisme de fallback.

### Exemple de configuration des groupes

```yaml
management:
  endpoint:
    health:
      probes:
        enabled: true
      group:
        readiness:
          include: readinessState,db
```

Cette configuration illustre l'inclusion de l'état de readiness et du contrôle de santé de la base de données dans le groupe readiness.

Elle suppose que l'indicateur `db` est disponible. Il faut vérifier les indicateurs effectivement exposés par la version de Spring Boot et les dépendances du projet.

**Attention :** intégrer une dépendance dans la readiness signifie qu'une défaillance de cette dépendance peut retirer l'instance du trafic. Si toutes les instances prennent la même décision au même moment, une panne externe peut provoquer une indisponibilité généralisée. Ce choix doit donc être fait selon les besoins métier.

## 7. Créer un indicateur de santé personnalisé

Spring Boot permet d'ajouter des contrôles de santé spécifiques au métier.

Par exemple, une application peut vérifier si un composant interne indispensable est initialisé.

```java
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class ReferentielHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        boolean disponible = verifierDisponibilite();

        if (disponible) {
            return Health.up().build();
        }

        return Health.down()
                .withDetail("referentiel", "indisponible")
                .build();
    }

    private boolean verifierDisponibilite() {
        // Remplacer par un véritable contrôle métier.
        return true;
    }
}
```

Cet exemple illustre le mécanisme ; la méthode `verifierDisponibilite()` doit être remplacée par une vérification réelle.

Les détails retournés doivent rester non sensibles, car ils peuvent être exposés à des systèmes de supervision ou à des utilisateurs autorisés à consulter Actuator.

Il faut également éviter de réaliser des opérations coûteuses ou bloquantes à chaque appel de l'endpoint de santé.

## 8. Health Check et Kubernetes

Kubernetes utilise les probes pour surveiller les conteneurs et prendre des décisions opérationnelles.

Exemple de configuration :

```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 10
  periodSeconds: 10
  timeoutSeconds: 2
  failureThreshold: 3

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 5
  periodSeconds: 5
  timeoutSeconds: 2
  failureThreshold: 3

startupProbe:
  httpGet:
    path: /actuator/health
    port: 8080
  periodSeconds: 5
  failureThreshold: 30
```

Cet exemple illustre les principaux paramètres :

- `initialDelaySeconds` : délai avant le premier contrôle.
- `periodSeconds` : intervalle entre les contrôles.
- `timeoutSeconds` : durée maximale d'un contrôle.
- `failureThreshold` : nombre d'échecs consécutifs avant de considérer la probe en échec.

Les valeurs doivent être adaptées au temps de démarrage et au comportement de l'application.

Lorsque l'application utilise un port de gestion distinct, il faut configurer le port approprié. Il faut aussi vérifier que les endpoints de santé sont accessibles depuis l'environnement d'exécution.

## 9. Health Check et architecture de microservices

Dans une architecture distribuée, chaque service peut exposer ses propres informations de santé.

Exemple :

```text
                 Supervision
                      |
          +-----------+-----------+
          |           |           |
          v           v           v
      Service A   Service B   Service C
          |           |           |
          v           v           v
        Health      Health      Health
        Check       Check       Check
```

Une plateforme de supervision peut collecter ces informations pour détecter les services défaillants.

Il faut toutefois distinguer :

- La santé individuelle d'un service.
- La disponibilité d'une dépendance.
- La disponibilité fonctionnelle de toute une chaîne métier.

Un service peut être `UP` alors qu'une opération métier échoue parce qu'un autre service est indisponible. Pour surveiller ces scénarios, il faut compléter les Health Checks par des métriques, des traces distribuées et des alertes.

## 10. Bonnes pratiques

### 10.1. Séparer liveness et readiness

Ne pas utiliser systématiquement la même logique pour les deux contrôles.

Un service qui ne peut temporairement pas accéder à une dépendance doit souvent être retiré du trafic plutôt que redémarré automatiquement.

### 10.2. Éviter les contrôles trop coûteux

Un Health Check doit être rapide et fiable.

Éviter notamment :

- Des requêtes SQL complexes.
- Des appels en cascade vers tous les microservices.
- Des opérations de calcul volumineuses.
- Des appels externes sans timeout.

Un contrôle trop coûteux peut lui-même contribuer à la surcharge.

### 10.3. Protéger les endpoints Actuator

En production :

- N'exposer que les endpoints nécessaires.
- Limiter l'accès aux informations détaillées.
- Utiliser les mécanismes de sécurité adaptés.
- Éviter de divulguer des URL internes, des noms de serveurs ou des informations de connexion.

### 10.4. Configurer des seuils adaptés

Des contrôles trop fréquents peuvent créer une charge inutile. Des seuils trop permissifs peuvent retarder la détection d'une panne.

Il faut adapter les paramètres à la latence et au comportement réel de l'application.

### 10.5. Tester les situations de panne

Vérifier le comportement lorsque :

- La base de données est indisponible.
- Une dépendance répond trop lentement.
- L'application démarre lentement.
- Une instance est bloquée.
- Une dépendance facultative est indisponible.

L'objectif est de s'assurer que la plateforme prend la bonne décision : attendre, retirer l'instance du trafic ou la redémarrer.

## 11. À retenir

Le Health Check permet de détecter les problèmes de fonctionnement et d'aider les plateformes de déploiement à gérer les instances.

Avec Spring Boot Actuator, il est possible d'exposer des endpoints de santé et de les intégrer à des outils de supervision ou à Kubernetes.

**Principe clé :** être démarré ne signifie pas nécessairement être prêt à traiter des requêtes ; il faut mesurer séparément la capacité à fonctionner et la capacité à servir le trafic.