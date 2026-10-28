# High Availability — Haute disponibilité

## 1. Définition

La **High Availability (HA)**, ou haute disponibilité, désigne la capacité d'un système informatique à rester accessible et opérationnel avec un minimum d'interruptions.

L'objectif est de réduire la durée et la fréquence des indisponibilités, qu'elles soient provoquées par une panne matérielle, une erreur logicielle, une opération de maintenance ou la défaillance d'une dépendance.

La haute disponibilité repose généralement sur la redondance des composants, la détection des pannes, le basculement vers des ressources de secours et la supervision.

Elle ne signifie pas qu'un système ne tombera jamais en panne, mais que les interruptions doivent rester limitées et maîtrisées.

## 2. Pourquoi la haute disponibilité est-elle importante ?

Considérons une application bancaire exposant une API de simulation financière.

```text
                   Clients
                      |
                      v
              Load Balancer
                 /       \
                v         v
          Instance A   Instance B
                \         /
                 v       v
                Base de données
```

Si l'instance A tombe en panne, le répartiteur de charge peut continuer à envoyer les requêtes vers l'instance B, à condition que celle-ci soit opérationnelle et dispose des ressources nécessaires.

L'application peut donc rester accessible sans attendre le redémarrage de l'instance A.

Cependant, si les deux instances dépendent d'une base de données unique qui devient indisponible, le service peut tout de même être interrompu.

La haute disponibilité doit donc être conçue en tenant compte de l'ensemble des dépendances critiques.

## 3. Les objectifs de la haute disponibilité

### 3.1. Réduire les interruptions

Limiter les interruptions causées par les pannes, les déploiements et les opérations de maintenance.

### 3.2. Éliminer les points uniques de défaillance

Un *Single Point of Failure* (SPOF) est un composant dont la panne suffit à rendre indisponible un service critique.

Exemples :

- un serveur applicatif unique ;
- un répartiteur de charge sans solution de secours ;
- une base de données sans mécanisme de basculement ;
- un composant réseau partagé indispensable.

### 3.3. Basculer rapidement vers une ressource disponible

Le système doit détecter les défaillances et orienter les requêtes vers des ressources capables de poursuivre le traitement.

### 3.4. Maintenir une capacité suffisante

La présence de plusieurs instances ne suffit pas si les instances restantes ne peuvent pas absorber la charge après une panne.

La capacité de secours doit être dimensionnée en fonction du trafic et des objectifs de disponibilité.

## 4. Les principaux mécanismes

### 4.1. Redondance

La redondance consiste à disposer de plusieurs ressources assurant une fonction identique ou équivalente.

Elle peut concerner :

- les serveurs applicatifs ;
- les bases de données ;
- les répartiteurs de charge ;
- les liens réseau ;
- les zones de disponibilité.

La redondance permet d'éviter qu'une panne isolée entraîne automatiquement l'indisponibilité du service.

### 4.2. Load Balancing

Le *load balancing* distribue les requêtes entre plusieurs instances.

Il peut prendre en compte la disponibilité des instances, leur capacité et des règles de distribution.

Le répartiteur doit également être protégé contre sa propre défaillance lorsqu'il constitue un composant critique.

### 4.3. Health Checks

Les contrôles de santé permettent de déterminer si une instance est apte à recevoir des requêtes.

Il faut distinguer :

- **Liveness** : indique si l'application fonctionne encore ou doit être redémarrée.
- **Readiness** : indique si l'application est prête à recevoir du trafic.

Une application peut être vivante tout en étant incapable de traiter correctement les requêtes, par exemple si une dépendance obligatoire est indisponible.

### 4.4. Failover

Le failover est le basculement vers une ressource de secours lorsqu'une ressource principale ne peut plus assurer son rôle.

Il peut être automatique ou manuel, selon l'architecture et les exigences opérationnelles.

### 4.5. Déploiement sans interruption

Des stratégies comme le rolling deployment ou le blue-green deployment permettent de limiter les interruptions lors des mises à jour applicatives.

Elles nécessitent notamment de gérer correctement les contrôles de santé, le retrait des instances du trafic et la compatibilité des changements de base de données.

## 5. Mesurer la disponibilité

La disponibilité peut être exprimée comme le rapport entre le temps pendant lequel le service est opérationnel et le temps total observé.

\[
Disponibilité = \frac{Temps\ opérationnel}{Temps\ total} \times 100
\]

Les objectifs de disponibilité sont souvent exprimés en pourcentage.

| Disponibilité cible | Indisponibilité théorique maximale sur 30 jours |
|---|---:|
| 99 % | 7 h 12 min |
| 99,9 % | 43 min 12 s |
| 99,95 % | 21 min 36 s |
| 99,99 % | 4 min 19 s |

Ces valeurs sont théoriques et supposent une période de 30 jours. Les résultats réels dépendent de la méthode de mesure, des exclusions prévues et de la définition du service disponible.

## 6. Haute disponibilité et résilience

Les deux notions sont complémentaires, mais différentes.

| Haute disponibilité | Résilience |
|---|---|
| Cherche à réduire les interruptions de service. | Cherche à absorber les incidents et à en limiter les conséquences. |
| S'appuie notamment sur la redondance et le failover. | S'appuie notamment sur les timeouts, circuit breakers, fallbacks et mécanismes de récupération. |
| Traite la continuité d'accès au service. | Traite aussi le comportement du système pendant et après une défaillance. |

Une application peut avoir plusieurs instances et être hautement disponible au niveau applicatif, mais rester peu résiliente si elle ne gère pas correctement les appels vers ses dépendances.

Inversement, une application dotée de circuit breakers peut limiter la propagation des pannes sans garantir que son propre serveur restera disponible.

## 7. Exemple avec Spring Boot

Spring Boot fournit des endpoints de santé via Spring Boot Actuator. Ils peuvent être utilisés par une plateforme d'orchestration ou un répartiteur de charge pour évaluer l'état d'une instance.

### 7.1. Dépendance Maven

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

### 7.2. Configuration

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

Cette configuration active les sondes de santé et expose l'endpoint de santé.

Selon la version et l'environnement, les endpoints de liveness et de readiness peuvent être disponibles sous :

```text
/actuator/health/liveness
/actuator/health/readiness
```

Ils doivent être vérifiés dans la configuration réelle de l'application et protégés conformément aux règles de sécurité de l'environnement.

### 7.3. Utilisation en production

Dans une plateforme Kubernetes, les sondes peuvent être configurées pour contrôler l'état de l'application.

```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
```

Cet extrait illustre le principe ; les délais, seuils et autres paramètres doivent être adaptés au démarrage et au fonctionnement de l'application.

- La liveness probe permet de détecter une application qui ne fonctionne plus correctement.
- La readiness probe permet d'éviter d'envoyer du trafic à une instance qui n'est pas prête.

Les probes ne remplacent ni la redondance ni le failover. Elles fournissent des informations utilisées par la plateforme pour prendre des décisions.

## 8. Bonnes pratiques

- Identifier les points uniques de défaillance.
- Répartir les instances sur des ressources ou zones indépendantes lorsque cela est pertinent.
- Prévoir une capacité suffisante après la perte d'une instance.
- Mettre en place des contrôles de santé adaptés.
- Tester le basculement, pas seulement la configuration nominale.
- Éviter qu'une dépendance critique unique annule tous les bénéfices de la redondance applicative.
- Surveiller la disponibilité réelle du service, pas uniquement l'état des serveurs.
- Définir des objectifs de disponibilité mesurables.
- Tester les procédures de maintenance et de déploiement.
- Documenter les mécanismes de failover et les procédures de retour à la normale.

## 9. Limites et points d'attention

La haute disponibilité augmente la complexité de l'architecture et les besoins en exploitation.

Plusieurs instances peuvent partager la même dépendance défaillante. Une réplication mal configurée peut également entraîner des incohérences, et un basculement automatique peut produire des effets indésirables si l'état réel du composant n'est pas correctement évalué.

La haute disponibilité doit donc être conçue au niveau de l'ensemble du service, avec des scénarios de panne réalistes et des tests réguliers.

## 10. À retenir

La **High Availability** vise à maintenir un service accessible malgré les pannes et les opérations de maintenance.

Elle s'appuie principalement sur la redondance, le load balancing, les health checks et le failover.

Pour obtenir une architecture fiable, ces mécanismes doivent être combinés avec une stratégie de résilience, une supervision adaptée et des tests de défaillance.
