# Backpressure — Régulation de la charge

## 1. Définition

Le **Backpressure** est un mécanisme qui permet à un système de signaler qu'il ne peut plus traiter les requêtes ou les événements au rythme auquel ils arrivent.

Son objectif est d'éviter qu'un composant soit submergé par un volume de travail supérieur à sa capacité de traitement.

Le principe est simple :

- Le producteur génère des données ou des requêtes.
- Le consommateur traite ces données ou ces requêtes.
- Lorsque le consommateur est saturé, le système régule le flux entrant au lieu de continuer à accumuler du travail.

Le Backpressure est particulièrement utile dans les architectures asynchrones, les traitements de flux, les systèmes événementiels et les applications réactives.

## 2. Pourquoi utiliser le Backpressure ?

Considérons une application Spring Boot qui reçoit des commandes et les transmet à un service externe.

```text
Clients
   |
   v
API Spring Boot
   |
   v
File d'attente
   |
   v
Service externe
```

Supposons que l'API reçoive 2 000 requêtes par seconde, alors que le service externe ne peut en traiter que 500.

Sans mécanisme de régulation, les requêtes s'accumulent. Cette accumulation peut provoquer :

- Une augmentation de la consommation mémoire.
- Une augmentation du temps de réponse.
- Une saturation des threads.
- Des délais d'attente dépassés.
- Des erreurs en cascade.
- Une indisponibilité de l'application.

Le Backpressure permet de contrôler le volume de travail accepté ou transmis afin de maintenir le système dans une zone de fonctionnement maîtrisée.

## 3. Fonctionnement du Backpressure

### 3.1. Le producteur et le consommateur

Le producteur est le composant qui génère les données. Le consommateur est celui qui les traite.

Exemple :

- Producteur : un contrôleur REST qui reçoit des commandes.
- Consommateur : un service qui appelle une API partenaire.
- Ressource limitée : le nombre de requêtes que le partenaire peut traiter simultanément.

Lorsque le producteur est plus rapide que le consommateur, un déséquilibre apparaît.

Le Backpressure peut agir à différents niveaux :

1. **Ralentir le producteur** lorsque le protocole le permet.
2. **Limiter les éléments transmis** au consommateur.
3. **Mettre en attente un volume limité de travail**.
4. **Rejeter les nouvelles demandes** lorsque la capacité maximale est atteinte.
5. **Réduire le rythme de consommation** selon les capacités disponibles.

Le choix dépend du type de système et des garanties attendues.

### 3.2. Flux avec et sans Backpressure

Sans Backpressure :

```text
Producteur :  1000 événements/s
                    |
                    v
              File qui grandit
                    |
                    v
Consommateur : 200 événements/s
```

Avec une régulation :

```text
Producteur
    |
    v
Mécanisme de régulation
    |
    | Flux adapté à la capacité disponible
    v
Consommateur
```

Le but n'est pas nécessairement de traiter toutes les demandes immédiatement. Il s'agit d'éviter qu'un débit excessif dégrade l'ensemble du système.

## 4. Les principales stratégies

### 4.1. Contrôle de la demande

Le consommateur indique combien d'éléments il est prêt à recevoir.

Cette stratégie est courante dans les systèmes réactifs qui prennent en charge la notion de demande.

Par exemple, un consommateur peut demander 50 éléments, les traiter, puis demander les 50 suivants.

Le producteur ne transmet pas davantage d'éléments que le consommateur n'en a demandé.

### 4.2. File d'attente bornée

Une file d'attente bornée possède une capacité maximale.

Exemple :

```text
Capacité maximale : 100 tâches

File actuelle : 80 tâches
Nouvelle tâche : acceptée

File actuelle : 100 tâches
Nouvelle tâche : rejetée ou différée
```

Une file bornée empêche l'accumulation illimitée de tâches en mémoire.

Il faut définir explicitement le comportement lorsque la file est pleine : rejet, attente, mise en attente durable ou traitement alternatif.

### 4.3. Limitation de la concurrence

Le système limite le nombre de traitements exécutés simultanément.

Par exemple, une application peut autoriser au maximum 20 appels concurrents vers un service partenaire.

Cette stratégie protège le service distant et les ressources locales, mais elle ne suffit pas toujours à maîtriser le volume total de demandes en attente. Elle doit être combinée, si nécessaire, avec une file bornée ou une politique de rejet.

### 4.4. Rejet contrôlé

Lorsque le système ne peut plus accepter de travail, il refuse rapidement les nouvelles demandes.

Pour une API REST, cela peut se traduire par :

- `429 Too Many Requests` lorsque la limite d'admission est dépassée.
- `503 Service Unavailable` lorsque le service ne peut temporairement pas traiter la demande.

Le code HTTP doit refléter la cause réelle du refus.

## 5. Backpressure et Spring Boot

Dans une application Spring Boot, plusieurs mécanismes peuvent contribuer à réguler la charge. Ils ne sont pas tous du Backpressure au sens strict.

### 5.1. Limiter les appels concurrents avec Resilience4j

Le module `resilience4j-bulkhead` permet de limiter le nombre d'exécutions simultanées d'une opération.

Exemple de configuration :

```yaml
resilience4j:
  bulkhead:
    instances:
      partenaire:
        maxConcurrentCalls: 20
        maxWaitDuration: 0
```

Dans cet exemple :

- `maxConcurrentCalls: 20` autorise au maximum 20 appels simultanés.
- `maxWaitDuration: 0` signifie qu'une demande qui ne peut pas obtenir immédiatement une place est rejetée plutôt que mise en attente par ce Bulkhead.

Exemple d'utilisation :

```java
@Service
public class PartenaireService {

    @Bulkhead(name = "partenaire", type = Bulkhead.Type.SEMAPHORE)
    public Resultat appelerPartenaire(Requete requete) {
        return clientPartenaire.appeler(requete);
    }
}
```

Cette configuration protège l'application contre une concurrence excessive. Elle constitue un mécanisme de contrôle de charge complémentaire au Backpressure.

**Attention :** l'annotation nécessite que Resilience4j soit correctement intégré à Spring. Avec le mode Spring AOP, les appels internes directs à une autre méthode du même bean peuvent contourner l'intercepteur.

### 5.2. Contrôler la capacité d'un pool de threads

Un exécuteur asynchrone peut être configuré avec un nombre limité de threads et une file d'attente bornée.

```java
@Configuration
@EnableAsync
public class AsyncConfiguration {

    @Bean("taskExecutor")
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("traitement-");

        executor.initialize();
        return executor;
    }
}
```

Cette configuration limite :

- Le nombre de threads principaux à 5.
- Le nombre maximal de threads à 10.
- La file d'attente à 100 tâches.

Lorsque les threads et la file sont saturés, le comportement dépend de la politique de rejet de l'exécuteur.

Cette configuration est une forme de limitation de capacité, mais pas un protocole de Backpressure complet : elle ne ralentit pas automatiquement le producteur distant.

### 5.3. Avec Spring WebFlux et Reactor

Dans une application réactive, Reactor permet de contrôler la demande entre les éléments d'un flux.

Exemple :

```java
Flux<Integer> flux = Flux.range(1, 1000)
    .limitRate(50)
    .doOnNext(this::traiter);
```

`limitRate(50)` permet de réguler la demande en amont par lots plutôt que de demander tous les éléments en une seule fois.

Le comportement précis dépend de la source et des opérateurs utilisés. `limitRate` ne constitue pas une limite universelle sur la mémoire ou sur le nombre de requêtes HTTP en cours.

Dans une application WebFlux, il faut également veiller à ne pas bloquer les threads d'exécution réactifs avec des opérations bloquantes.

## 6. Backpressure, Rate Limiting et Bulkhead

Ces mécanismes sont complémentaires, mais répondent à des problèmes différents.

| Mécanisme | Objectif principal | Exemple |
|---|---|---|
| Backpressure | Adapter le flux à la capacité du consommateur | Demander les événements par lots |
| Rate Limiting | Limiter le débit accepté sur une période | 100 requêtes par seconde |
| Bulkhead | Limiter les ressources consommées simultanément | 20 appels concurrents |
| Timeout | Limiter la durée d'attente d'une opération | Abandonner après 2 secondes |
| Circuit Breaker | Éviter les appels répétés vers un service défaillant | Bloquer temporairement les appels |
| File bornée | Limiter le travail en attente | Maximum 100 tâches |

Un système résilient combine souvent plusieurs de ces mécanismes.

Par exemple, un Rate Limiter contrôle le débit entrant, un Bulkhead limite les appels concurrents et une file bornée empêche une accumulation excessive des traitements.

## 7. Backpressure dans une architecture distribuée

Dans une architecture composée de plusieurs microservices, le Backpressure doit être envisagé à chaque frontière de communication.

Exemple :

```text
Client
   |
   v
API Gateway
   |
   v
Service de simulation
   |
   v
Service de tarification
   |
   v
API partenaire
```

Si l'API partenaire ralentit, le service de tarification risque d'accumuler des requêtes. Cette accumulation peut ensuite saturer le service de simulation et remonter jusqu'à la Gateway.

Pour limiter ce risque, on peut combiner :

- Une limitation du débit à l'entrée.
- Des limites de concurrence pour les appels externes.
- Des files d'attente bornées pour les traitements asynchrones.
- Des timeouts cohérents entre les différents services.
- Des rejets explicites lorsque la capacité disponible est dépassée.
- Des métriques pour détecter les files qui grandissent et les consommateurs saturés.

Dans une architecture événementielle utilisant Kafka ou un autre broker, le consommateur peut également ralentir la lecture ou ajuster le nombre de messages traités en parallèle. Le broker apporte une capacité de stockage, mais ne supprime pas le besoin de maîtriser les volumes et les délais de traitement.

## 8. Bonnes pratiques

### 8.1. Définir une capacité maximale

Définir des limites explicites pour :

- Les requêtes en cours.
- Les tâches en attente.
- Les connexions aux services distants.
- Les messages traités simultanément.
- La mémoire consommée par les files.

Une file sans limite peut déplacer le problème sans le résoudre.

### 8.2. Choisir une stratégie de saturation

Lorsqu'une limite est atteinte, décider si le système doit :

- Attendre pendant une durée limitée.
- Rejeter immédiatement.
- Réessayer ultérieurement.
- Persister le travail dans une file durable.
- Utiliser un traitement alternatif.

Le choix dépend de la criticité de l'opération. Une commande financière, par exemple, ne doit pas être abandonnée silencieusement.

### 8.3. Éviter les accumulations invisibles

Surveiller notamment :

- La taille des files d'attente.
- Le nombre de tâches en attente.
- Le nombre de traitements actifs.
- Le temps moyen et maximal passé en file.
- Le nombre de rejets.
- Le taux de traitement des consommateurs.

Une file qui augmente continuellement indique généralement que le système reçoit durablement plus de travail qu'il ne peut en traiter.

### 8.4. Préserver la cohérence métier

Si une requête est rejetée ou différée, l'appelant doit comprendre ce qui s'est passé.

Pour les traitements asynchrones, prévoir si nécessaire :

- Un identifiant de suivi.
- Un mécanisme de consultation du statut.
- Une politique de reprise.
- Une gestion de l'idempotence pour éviter les doublons.

### 8.5. Tester les situations de surcharge

Ne pas se limiter aux tests réalisés à charge normale.

Simuler également :

- Une augmentation soudaine du trafic.
- Un partenaire lent.
- Une file d'attente pleine.
- Une baisse de la capacité des consommateurs.
- Une indisponibilité temporaire d'une dépendance.

L'objectif est de vérifier que le système reste maîtrisé lorsque la demande dépasse sa capacité.

## 9. Limites du Backpressure

Le Backpressure ne crée pas de capacité supplémentaire. Il permet de mieux gérer l'écart entre le volume entrant et la capacité de traitement.

Il ne garantit pas non plus qu'une requête sera exécutée : selon la stratégie choisie, elle peut être différée ou rejetée.

Enfin, il est plus facile à appliquer lorsque les producteurs et les consommateurs utilisent un protocole capable de propager la demande. Entre services HTTP indépendants, il faut généralement construire une politique explicite de limitation, d'attente ou de rejet.

## 10. À retenir

Le Backpressure est un mécanisme essentiel pour éviter qu'un système soit submergé par un flux de travail supérieur à sa capacité.

Dans une application Spring Boot, il peut être mis en œuvre ou complété par :

- Des files d'attente bornées.
- Des limites de concurrence avec Resilience4j Bulkhead.
- Une régulation de la demande avec Reactor.
- Un Rate Limiter pour contrôler le débit.
- Des timeouts et une politique de rejet cohérente.

**Principe clé :** un système résilient ne doit pas seulement savoir traiter les requêtes ; il doit aussi savoir maîtriser la charge qu'il accepte.