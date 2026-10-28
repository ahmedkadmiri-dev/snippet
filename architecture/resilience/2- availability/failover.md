# Failover — Basculement vers une ressource de secours

## 1. Définition

Le **Failover** désigne le mécanisme permettant de basculer d'une ressource principale vers une ressource de secours lorsqu'une défaillance empêche la première d'assurer correctement son rôle.

L'objectif est de maintenir la continuité du service en réduisant la durée d'indisponibilité.

Le failover peut concerner des serveurs applicatifs, des bases de données, des services réseau ou des infrastructures réparties sur plusieurs zones.

Il constitue l'un des mécanismes utilisés pour améliorer la haute disponibilité.

## 2. Pourquoi le failover est-il nécessaire ?

Considérons une application Spring Boot déployée sur deux instances.

```text
                Clients
                   |
                   v
            Load Balancer
               /       \
              v         v
         Instance A  Instance B
            ACTIVE     STANDBY
```

L'instance A traite normalement les requêtes. L'instance B est disponible pour prendre le relais.

Si l'instance A tombe en panne, le système détecte son indisponibilité et redirige les requêtes vers l'instance B.

```text
                Clients
                   |
                   v
            Load Balancer
               /       \
              X         v
         Instance A  Instance B
          INACTIVE     ACTIVE
```

Le service peut continuer à fonctionner sans attendre le rétablissement de l'instance A, à condition que l'instance B soit opérationnelle et suffisamment dimensionnée.

## 3. Les principales étapes du failover

### 3.1. Détection de la panne

Le système identifie une ressource qui ne peut plus assurer son rôle.

La détection peut reposer sur :

- des health checks ;
- des sondes de disponibilité ;
- des erreurs de connexion ;
- des timeouts ;
- des signaux fournis par l'infrastructure.

Il faut distinguer une panne réelle d'une réponse momentanément lente. Une détection trop sensible peut déclencher des basculements inutiles.

### 3.2. Décision de basculement

Le système détermine si le basculement est nécessaire.

Cette décision peut être automatique, par exemple à travers un répartiteur de charge ou un gestionnaire de cluster, ou manuelle dans le cadre d'une procédure d'exploitation.

### 3.3. Activation de la ressource de secours

Selon l'architecture, le système peut :

- rediriger le trafic vers une autre instance ;
- promouvoir une réplique de base de données ;
- activer un serveur de secours ;
- basculer vers une autre zone d'infrastructure.

### 3.4. Vérification du service

Après le basculement, il faut vérifier que la ressource de secours traite correctement les requêtes et que les dépendances nécessaires sont disponibles.

### 3.5. Retour à la situation nominale

Lorsque la ressource principale est rétablie, il faut décider si elle doit reprendre son rôle.

Ce retour, parfois appelé *failback*, doit être contrôlé afin d'éviter les basculements répétés ou les incohérences d'état.

## 4. Les principaux types de failover

### 4.1. Failover applicatif

Le trafic est redirigé vers une autre instance de l'application.

Exemple : une instance Spring Boot devient indisponible et le répartiteur continue d'envoyer les requêtes vers les autres instances disponibles.

### 4.2. Failover de base de données

Une base secondaire prend le rôle de base principale lorsqu'une défaillance affecte la base active.

Ce mécanisme nécessite de gérer la réplication, les connexions, la cohérence des données et les risques de divergence entre les nœuds.

Un basculement réussi ne garantit pas nécessairement l'absence de perte de données : cela dépend notamment du mode de réplication et de la politique de validation des transactions.

### 4.3. Failover réseau

Le trafic est redirigé vers un lien, un équipement ou un chemin réseau alternatif.

### 4.4. Failover géographique

Le service est repris dans un autre site ou une autre zone lorsque l'infrastructure principale est affectée.

Cette approche peut nécessiter des mécanismes spécifiques pour la réplication des données, le routage, le DNS et la reprise des traitements.

## 5. Failover automatique et manuel

| Critère | Failover automatique | Failover manuel |
|---|---|---|
| Déclenchement | Par un mécanisme automatisé. | Par une action d'exploitation. |
| Rapidité | Généralement plus rapide. | Dépend du délai de détection et d'intervention. |
| Complexité | Nécessite une détection fiable et des règles de basculement. | Nécessite des procédures documentées et des opérateurs disponibles. |
| Risque principal | Faux positifs ou basculements répétés. | Retard d'intervention ou erreur humaine. |
| Cas d'usage | Pannes courantes avec critères de basculement bien définis. | Incidents complexes ou basculements nécessitant une validation. |

Le choix dépend des exigences de disponibilité, de la nature des composants et du niveau de risque acceptable.

## 6. Failover et split-brain

Le **split-brain** est une situation dans laquelle plusieurs nœuds se considèrent simultanément comme le nœud principal.

Ce problème est particulièrement important pour les bases de données et les systèmes distribués qui autorisent des écritures.

Il peut entraîner :

- des écritures concurrentes ;
- des données divergentes ;
- des conflits de synchronisation ;
- une perte de cohérence métier.

Les solutions possibles comprennent un mécanisme de quorum, un système de consensus ou un mécanisme de fencing empêchant un ancien nœud principal d'effectuer des écritures après la promotion d'un nouveau nœud.

Le choix dépend de la technologie utilisée et des garanties de cohérence attendues.

## 7. Failover et Failback

Ces deux mécanismes correspondent à des étapes différentes.

```text
Fonctionnement normal
        |
        v
Défaillance détectée
        |
        v
     Failover
        |
        v
Service assuré par la ressource de secours
        |
        v
Ressource principale rétablie
        |
        v
     Failback
        |
        v
Retour contrôlé à la situation nominale
```

- **Failover** : passage vers une ressource de secours.
- **Failback** : retour éventuel vers la ressource principale.

Le failback n'est pas toujours immédiat ni obligatoire. Il doit tenir compte de la stabilité de la ressource rétablie et de la cohérence des données.

## 8. Exemple avec Spring Boot et un répartiteur de charge

Spring Boot ne fournit pas à lui seul un failover complet entre plusieurs instances. Le basculement est généralement assuré par l'infrastructure de déploiement ou de routage.

Par exemple, plusieurs instances d'une API peuvent être placées derrière un répartiteur de charge.

```text
                    Clients
                       |
                       v
                Load Balancer
                 /          \
                v            v
         Spring Boot A   Spring Boot B
                 \          /
                  v        v
                 Base de données
```

Les instances peuvent exposer les endpoints de santé Spring Boot Actuator.

### 8.1. Dépendance Maven

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

### 8.2. Configuration des probes

```yaml
management:
  endpoint:
    health:
      probes:
        enabled: true
  endpoints:
    web:
      exposure:
        include: health
```

Les endpoints de readiness et de liveness peuvent ensuite être utilisés par l'infrastructure pour déterminer si une instance est prête à recevoir du trafic ou si elle doit être redémarrée.

Le répartiteur de charge doit être configuré pour retirer du trafic les instances non disponibles. La configuration exacte dépend de la plateforme utilisée.

**Point important :** un health check ne réalise pas à lui seul le failover. Il fournit un signal à un composant chargé de prendre la décision et d'effectuer le basculement.

## 9. Failover et résilience applicative

Le failover et les patterns de résilience répondent à des problèmes complémentaires.

| Failover | Résilience applicative |
|---|---|
| Remplace une ressource indisponible par une autre. | Protège les traitements contre les défaillances de leurs dépendances. |
| Est souvent géré par l'infrastructure ou le cluster. | Est souvent implémentée dans l'application ou dans ses bibliothèques clientes. |
| Repose sur la redondance et les mécanismes de basculement. | Repose notamment sur les timeouts, retries, circuit breakers et fallbacks. |

Par exemple, si une API partenaire est indisponible, un circuit breaker peut interrompre les appels inutiles. Si une instance applicative tombe en panne, le failover peut rediriger les requêtes vers une autre instance.

## 10. Bonnes pratiques

- Définir des critères de détection fiables.
- Tester les scénarios de panne et les basculements réels.
- Vérifier que la ressource de secours dispose d'une capacité suffisante.
- Mesurer le délai de détection et le délai de basculement.
- Contrôler la cohérence des données après un failover de base de données.
- Prévenir les situations de split-brain lorsque plusieurs nœuds peuvent écrire.
- Documenter les procédures de failover et de failback.
- Prévoir des alertes lors d'un basculement.
- Éviter les basculements automatiques répétés provoqués par des erreurs de détection.
- Vérifier les dépendances partagées qui pourraient rester des points uniques de défaillance.

## 11. Indicateurs utiles

Deux indicateurs sont particulièrement importants :

- **RTO (Recovery Time Objective)** : durée cible maximale pour rétablir le service après une interruption.
- **RPO (Recovery Point Objective)** : perte de données maximale acceptable, exprimée comme une période de données.

Ces objectifs sont complémentaires. Le RTO concerne le temps de reprise, tandis que le RPO concerne les données que l'organisation accepte éventuellement de perdre.

Leur définition dépend des exigences métier et des garanties techniques de l'infrastructure.

## 12. À retenir

Le **Failover** permet de basculer vers une ressource de secours lorsqu'une ressource principale devient indisponible.

Il peut concerner les applications, les bases de données, le réseau ou des sites géographiques.

Pour être efficace, il nécessite une détection fiable, des ressources de secours opérationnelles, une gestion cohérente des données et des tests réguliers de basculement.
