# Redundancy — Redondance des composants

## 1. Définition

La **Redundancy**, ou redondance, consiste à disposer de plusieurs ressources capables d'assurer une même fonction, afin qu'une défaillance isolée ne provoque pas nécessairement l'interruption du service.

Ces ressources peuvent être des serveurs applicatifs, des bases de données, des équipements réseau, des instances de services ou des zones d'infrastructure.

La redondance est un mécanisme fondamental de la haute disponibilité. Elle permet de maintenir le service lorsqu'une ressource devient indisponible, à condition que les autres ressources soient opérationnelles et correctement configurées.

## 2. Pourquoi utiliser la redondance ?

Considérons une API Spring Boot déployée sur un seul serveur.

```text id="p9v8m3"
Client
  |
  v
Serveur unique
  |
  v
API Spring Boot
```

Si le serveur tombe en panne, l'API devient indisponible.

Avec deux instances :

```text id="2fd6zj"
               Clients
                  |
                  v
           Load Balancer
              /      \
             v        v
        Instance A  Instance B
```

Si l'instance A tombe en panne, le répartiteur peut continuer à envoyer les requêtes vers l'instance B.

La redondance permet donc de réduire la dépendance à une ressource unique.

## 3. Les principaux types de redondance

### 3.1. Redondance applicative

Plusieurs instances de la même application sont déployées.

Exemple : deux instances d'une API de simulation financière exécutent la même version de l'application.

Avantages :
- continuité du service en cas de panne d'une instance ;
- possibilité de répartir la charge ;
- maintenance facilitée lorsque les déploiements sont progressifs.

Points d'attention :
- gestion des sessions ;
- cohérence des données ;
- configuration identique des instances ;
- capacité restante après une panne.

### 3.2. Redondance des données

Les données sont répliquées sur plusieurs ressources de stockage ou instances de base de données.

Exemple :

```text id="e5wjdc"
          Application
               |
               v
        Base principale
               |
          Réplication
               |
               v
        Base secondaire
```

La réplication peut améliorer la disponibilité et faciliter la reprise après incident.

Cependant, une réplication ne constitue pas automatiquement une sauvegarde. Une suppression accidentelle ou une corruption peut également être répliquée.

Il faut donc distinguer réplication, sauvegarde et restauration.

### 3.3. Redondance réseau

Plusieurs liens réseau ou équipements peuvent être prévus pour éviter qu'une panne de connexion bloque les communications.

Cette redondance doit être suffisamment indépendante pour qu'un incident commun ne coupe pas simultanément toutes les voies de communication.

### 3.4. Redondance géographique

Les composants sont répartis entre plusieurs zones de disponibilité ou sites distincts.

Cette approche permet de mieux résister à une panne affectant une zone entière.

Elle nécessite toutefois de gérer les latences réseau, la réplication des données, le routage et les éventuels conflits entre instances.

## 4. Les modes de redondance

### 4.1. Active-passive

Une ressource principale traite les requêtes. Une ressource secondaire reste disponible pour prendre le relais.

```text id="0f2x0w"
             Trafic
               |
               v
        Instance principale
               |
        Panne détectée
               |
               v
        Instance de secours
```

Avantages :
- fonctionnement relativement simple ;
- séparation claire entre ressource principale et ressource de secours.

Inconvénients :
- capacité de secours potentiellement inutilisée ;
- délai de basculement ;
- nécessité de synchroniser l'état lorsque cela est requis.

### 4.2. Active-active

Plusieurs ressources traitent simultanément les requêtes.

```text id="2f2x0w"
             Trafic
                |
                v
          Load Balancer
             /      \
            v        v
        Instance A  Instance B
```

Avantages :
- utilisation simultanée des ressources ;
- répartition de charge ;
- possibilité de poursuivre le service après la perte d'une instance.

Inconvénients :
- complexité de gestion de l'état ;
- nécessité d'éviter les traitements concurrents incohérents ;
- dimensionnement nécessaire pour absorber une panne.

## 5. Redondance et point unique de défaillance

Un **Single Point of Failure (SPOF)** est un composant dont la défaillance suffit à interrompre un service.

Par exemple, deux instances applicatives ne garantissent pas la haute disponibilité si elles dépendent toutes les deux d'un répartiteur de charge unique non redondé ou d'une base de données unique sans solution de secours.

```text id="9h0q2c"
             Clients
                |
                v
        Load Balancer unique
                |
          +-----+-----+
          |           |
          v           v
      Instance A  Instance B
          \           /
           \         /
            v       v
          Base unique
```

Dans ce scénario, la base de données demeure un point unique de défaillance.

L'analyse de redondance doit donc couvrir l'ensemble des composants et de leurs dépendances.

## 6. Exemple avec Spring Boot

La redondance applicative n'est généralement pas obtenue en ajoutant une annotation Java. Elle dépend principalement du déploiement, du routage du trafic et de la gestion de l'état.

Une application Spring Boot peut être déployée sur plusieurs instances identiques :

```text id="1n8f6p"
              Clients
                 |
                 v
          Load Balancer
            /       \
           v         v
      Spring Boot  Spring Boot
      Instance A   Instance B
            \       /
             v     v
          Base de données
```

Chaque instance expose les mêmes endpoints REST.

Par exemple :

```java id="8v2m4k"
@RestController
@RequestMapping("/api/simulations")
public class SimulationController {

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("UP");
    }
}
```

Cet endpoint est uniquement illustratif. En production, il est préférable d'utiliser Spring Boot Actuator pour les contrôles de santé, avec une configuration appropriée.

Pour que les deux instances fonctionnent correctement :

- elles doivent utiliser une configuration cohérente ;
- leurs dépendances doivent être accessibles ;
- les sessions ne doivent pas dépendre exclusivement de la mémoire locale d'une instance si les requêtes peuvent changer d'instance ;
- les opérations métier doivent être protégées contre les doublons lorsque cela est nécessaire ;
- le répartiteur doit retirer du trafic les instances qui ne sont plus prêtes.

## 7. Redondance et résilience

La redondance et la résilience sont complémentaires, mais différentes.

| Redondance | Résilience |
|---|---|
| Fournit plusieurs ressources pour une même fonction. | Définit comment le système réagit aux incidents. |
| Réduit la dépendance à une ressource unique. | Limite la propagation des défaillances et organise la récupération. |
| Facilite le maintien du service grâce à des ressources de remplacement. | S'appuie notamment sur les timeouts, circuit breakers, fallbacks et mécanismes de reprise. |

Disposer de plusieurs instances ne suffit pas si chaque instance reste bloquée par le même service partenaire indisponible.

Inversement, un circuit breaker peut protéger une application contre un partenaire défaillant, mais il ne remplace pas une seconde instance applicative lorsque le serveur principal tombe en panne.

## 8. Bonnes pratiques

- Identifier les points uniques de défaillance.
- Choisir entre active-active et active-passive selon les besoins.
- Répartir les ressources critiques lorsque cela est nécessaire.
- Vérifier que la capacité restante peut absorber la charge.
- Tester réellement la perte d'une instance.
- Automatiser la détection des ressources indisponibles.
- Gérer correctement les sessions et les données partagées.
- Distinguer réplication et sauvegarde.
- Vérifier que les mécanismes de secours sont régulièrement testés.
- Surveiller les ressources restantes après un basculement.

## 9. Limites et points d'attention

La redondance augmente la complexité de l'exploitation et peut engendrer des coûts supplémentaires.

Elle ne garantit pas une continuité absolue : plusieurs ressources peuvent être touchées simultanément par une panne commune, une mauvaise configuration ou une défaillance de leur dépendance partagée.

Une architecture redondante doit donc être accompagnée de mécanismes de détection, de basculement, de supervision et de récupération.

## 10. À retenir

La **Redundancy** consiste à multiplier les ressources critiques pour réduire les conséquences d'une défaillance isolée.

Elle peut être applicative, liée aux données, au réseau ou géographique, avec des modes active-active ou active-passive.

Pour être efficace, elle doit éliminer les points uniques de défaillance pertinents et être complétée par des mécanismes de résilience et des tests de basculement.
