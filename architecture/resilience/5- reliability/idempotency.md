# Idempotency — Garantir l'exécution sans doublons

## 1. Définition

L'**idempotence** est une propriété qui garantit que l'exécution répétée d'une même opération, avec les mêmes paramètres et dans le même contexte, produit le même effet métier qu'une seule exécution.

Elle est particulièrement importante dans les architectures distribuées, où les timeouts, les erreurs réseau et les tentatives de reprise peuvent provoquer plusieurs appels pour une même action métier.

Par exemple, un client envoie une demande de création de simulation. Le serveur traite la demande, mais la réponse ne parvient jamais au client. Celui-ci ne sait pas si l'opération a réussi et décide de la renvoyer.

Sans mécanisme d'idempotence, deux simulations peuvent être créées.

Avec une gestion correcte de l'idempotence, la deuxième requête retrouve le résultat de la première au lieu de créer une nouvelle simulation.

## 2. Pourquoi l'idempotence est-elle importante ?

Dans un système distribué, il est difficile de distinguer avec certitude une opération non exécutée d'une opération exécutée dont la réponse a été perdue.

Exemple :

```text
Client
   |
   | POST /simulations
   v
API Spring Boot
   |
   | Création de la simulation
   v
Base de données
   |
   | Réponse perdue
   X
Client
   |
   | Nouvelle tentative
   v
API Spring Boot
```

Le client peut envoyer plusieurs fois la même demande.

Les causes les plus fréquentes sont :

- Un timeout HTTP.
- Une coupure réseau.
- Un retry automatique dans un client HTTP.
- Un double clic sur un bouton.
- Une reprise après une erreur temporaire.
- La livraison répétée d'un message par un broker.
- Une relance manuelle d'un traitement.

L'idempotence permet d'éviter les effets métier indésirables liés à ces répétitions.

## 3. Comprendre le principe

Prenons une API qui crée une simulation financière.

Première requête :

```http
POST /api/moteur-financier/simulations
Idempotency-Key: simulation-2026-001
Content-Type: application/json
```

```json
{
  "montantProjet": 500000,
  "duree": 240,
  "typeOffre": "CLASSIQUE"
}
```

Le serveur crée la simulation et enregistre le résultat associé à la clé `simulation-2026-001`.

Si le client renvoie exactement la même requête avec cette clé, le serveur retourne le résultat déjà enregistré, sans créer une deuxième simulation.

```text
Première requête
       |
       v
Clé inconnue
       |
       v
Exécution métier
       |
       v
Enregistrement du résultat
       |
       v
Retour de la réponse


Deuxième requête avec la même clé
       |
       v
Clé déjà connue
       |
       v
Retour du résultat enregistré
```

Le mécanisme doit être conçu de manière à éviter que deux requêtes concurrentes portant la même clé déclenchent simultanément la création de deux ressources.

## 4. Idempotence HTTP et idempotence métier

### 4.1. Idempotence HTTP

Selon la sémantique HTTP, les méthodes suivantes sont idempotentes :

- `GET` : lecture d'une ressource.
- `PUT` : remplacement de l'état d'une ressource par l'état demandé.
- `DELETE` : suppression d'une ressource.
- `HEAD` et `OPTIONS`.

Les méthodes `POST` et `PATCH` ne sont pas idempotentes par défaut.

Cela ne signifie pas qu'un `POST` ne peut jamais être rendu idempotent. Une API peut ajouter un mécanisme d'idempotence applicatif, par exemple avec un en-tête `Idempotency-Key`.

Il faut également distinguer la sémantique HTTP de l'implémentation réelle : une opération `PUT` n'est idempotente que si ses effets observables respectent effectivement cette propriété.

### 4.2. Idempotence métier

L'idempotence métier concerne les effets produits par l'opération.

Exemple : transformer une simulation en dossier de financement.

Si la même demande de transformation est reçue deux fois, le système doit éviter :

- La création de deux dossiers.
- L'envoi de plusieurs notifications métier.
- La génération de plusieurs événements équivalents.
- La répétition d'une opération financière non souhaitée.

Une opération peut respecter la sémantique HTTP tout en nécessitant des garanties métier supplémentaires, notamment lorsqu'elle interagit avec plusieurs services.

## 5. Le mécanisme Idempotency-Key

Une approche courante consiste à demander au client d'envoyer une clé unique pour chaque opération logique.

Exemple :

```http
Idempotency-Key: 7f84a6c0-9e4f-4f28-a342-1a8a7c09b125
```

Le serveur associe cette clé à :

- L'identité du client ou le périmètre de sécurité.
- L'opération appelée.
- Une empreinte de la requête.
- L'état du traitement.
- Le résultat à restituer.
- Une date d'expiration éventuelle.

La clé doit être réutilisée lors des nouvelles tentatives de la même opération logique. Une nouvelle opération indépendante doit utiliser une nouvelle clé.

### 5.1. Cycle de vie d'une clé

Un cycle de vie possible est le suivant :

```text
ABSENTE
   |
   v
EN COURS
   |
   v
TERMINEE
   |
   v
EXPIREE
```

- **ABSENTE** : aucune opération n'est enregistrée pour cette clé.
- **EN COURS** : une requête a commencé le traitement.
- **TERMINEE** : le résultat est disponible et peut être restitué.
- **EXPIREE** : la période de conservation est terminée ; le comportement dépend de la politique définie.

L'expiration doit être choisie en fonction de la durée pendant laquelle les clients peuvent légitimement réessayer une opération.

Une clé expirée ne doit pas entraîner accidentellement la répétition d'une opération métier sensible.

## 6. Idempotence dans une application Spring Boot

### 6.1. Exemple de contrôleur REST

```java
@RestController
@RequestMapping("/api/moteur-financier/simulations")
public class SimulationController {

    private final SimulationService simulationService;

    public SimulationController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    @PostMapping
    public ResponseEntity<SimulationResponse> creerSimulation(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody SimulationRequest request) {

        SimulationResponse response =
                simulationService.creerSimulation(idempotencyKey, request);

        return ResponseEntity.ok(response);
    }
}
```

Cet exemple illustre la transmission de la clé au service métier. À lui seul, le contrôleur ne garantit pas l'idempotence.

En production, il faut notamment valider la clé, gérer les requêtes concurrentes et définir le comportement lorsque la même clé est réutilisée avec un contenu différent.

### 6.2. Exemple de service

Une conception possible consiste à déléguer la gestion des clés à un composant spécialisé :

```java
@Service
public class SimulationService {

    private final IdempotencyService idempotencyService;
    private final SimulationRepository simulationRepository;

    public SimulationService(
            IdempotencyService idempotencyService,
            SimulationRepository simulationRepository) {
        this.idempotencyService = idempotencyService;
        this.simulationRepository = simulationRepository;
    }

    public SimulationResponse creerSimulation(
            String key,
            SimulationRequest request) {

        return idempotencyService.execute(
                key,
                request,
                () -> {
                    Simulation simulation = new Simulation();
                    simulation.setMontantProjet(request.getMontantProjet());
                    simulation.setDuree(request.getDuree());

                    Simulation saved = simulationRepository.save(simulation);

                    return SimulationResponse.from(saved);
                }
        );
    }
}
```

La méthode `execute` représente ici une abstraction à implémenter. Elle doit assurer la coordination entre la réservation de la clé, l'exécution métier et l'enregistrement du résultat.

Le code ne doit pas être considéré comme une implémentation complète tant que ces garanties ne sont pas définies.

## 7. Où stocker les clés d'idempotence ?

Le stockage dépend des exigences de cohérence, de disponibilité et de durée de conservation.

### 7.1. En base de données

Une table dédiée peut contenir :

```text
IDEMPOTENCY_RECORD
------------------
ID
SCOPE
IDEMPOTENCY_KEY
REQUEST_HASH
STATUS
RESPONSE_STATUS
RESPONSE_BODY
CREATED_AT
EXPIRES_AT
```

Une contrainte d'unicité sur le couple `SCOPE + IDEMPOTENCY_KEY` empêche plusieurs enregistrements concurrents pour la même clé dans le même périmètre.

**Avantages :**

- Persistance durable.
- Possibilité d'utiliser les transactions.
- Bonne maîtrise des contraintes d'unicité.
- Adaptée aux opérations métier critiques.

**Inconvénients :**

- Charge supplémentaire sur la base de données.
- Gestion du nettoyage des enregistrements expirés.
- Besoin de maîtriser la taille des réponses conservées.

Pour une création métier critique, le stockage durable et la transaction peuvent être préférables à un simple cache.

### 7.2. Dans un cache distribué

Un cache distribué tel que Memcached ou Redis peut conserver temporairement les clés et les réponses.

**Avantages :**

- Accès rapide.
- Expiration automatique possible.
- Réduction des lectures en base métier.

**Inconvénients :**

- Les données peuvent expirer ou être évincées.
- La disponibilité et la persistance dépendent de la technologie et de sa configuration.
- Une réservation atomique de la clé est nécessaire.
- Le cache ne garantit pas, à lui seul, l'atomicité entre l'enregistrement de la clé et la transaction métier.

Si une perte de clé peut entraîner une seconde création métier, un cache seul ne suffit pas toujours. Il est souvent utile de compléter le mécanisme par une contrainte d'unicité métier ou une garantie transactionnelle durable.

### 7.3. En mémoire locale

Un `Map` ou un cache local peut suffire pour une démonstration ou un test.

Cependant, cette approche ne convient généralement pas à un déploiement distribué :

- Chaque instance possède son propre état.
- Un redémarrage fait perdre les données.
- Deux instances peuvent exécuter la même opération.
- La cohérence entre instances n'est pas garantie.

## 8. Gérer les cas particuliers

### 8.1. Même clé, même requête

Le serveur doit retrouver l'opération existante et restituer son résultat selon le contrat de l'API.

Il ne doit pas rejouer les effets métier.

### 8.2. Même clé, requête différente

Le serveur doit comparer l'empreinte de la requête reçue avec celle qui a été enregistrée.

Si les contenus diffèrent, il est préférable de rejeter la demande, par exemple avec un statut `409 Conflict`, plutôt que d'associer silencieusement deux opérations différentes à la même clé.

### 8.3. Deux requêtes simultanées

Deux requêtes peuvent arriver au même instant avec la même clé.

Le serveur doit réserver la clé de façon atomique. Une seule requête doit devenir propriétaire de l'exécution métier.

La seconde peut :

- Attendre que le traitement se termine.
- Recevoir une réponse indiquant que le traitement est en cours.
- Recevoir une erreur temporaire documentée, avec la possibilité de réessayer.

Le comportement doit être défini dans le contrat de l'API.

### 8.4. Échec pendant le traitement

Il faut distinguer deux situations :

1. Le traitement a échoué avant de produire un effet métier.
2. Le traitement a produit un effet métier, mais l'enregistrement du résultat ou l'envoi de la réponse a échoué.

La seconde situation est plus délicate : une nouvelle tentative ne doit pas répéter l'effet métier.

Lorsque l'opération et l'enregistrement de l'état d'idempotence sont dans la même base, une transaction peut aider à les rendre atomiques. Si l'opération implique plusieurs systèmes, des mécanismes supplémentaires sont nécessaires, par exemple une boîte d'envoi transactionnelle (*Transactional Outbox*) ou une clé métier unique.

## 9. Idempotence et retry

Le Retry et l'idempotence sont complémentaires.

Le Retry permet de relancer une opération après une erreur temporaire.

L'idempotence permet de rendre ces répétitions sûres.

Exemple :

```text
Appel initial
     |
     v
Timeout réseau
     |
     v
Nouvelle tentative
     |
     v
Même Idempotency-Key
     |
     v
Restitution du résultat existant
```

Sans idempotence, une stratégie de Retry appliquée à un `POST` de création peut provoquer des doublons.

Il ne faut donc pas activer des retries automatiques sur les opérations non idempotentes sans définir au préalable comment les répétitions seront traitées.

## 10. Idempotence et microservices

Dans une architecture distribuée, l'idempotence doit être appliquée aux frontières où les doublons peuvent se produire.

Exemple :

```text
Selfcare Gateway
       |
       v
Moteur financier
       |
       v
Service de tarification
       |
       v
Base de données
```

Si la Gateway réessaie un appel vers le moteur financier, la même clé doit pouvoir être transmise et interprétée de manière cohérente.

Il faut définir :

- Qui génère la clé.
- Quel service en est responsable.
- Le périmètre d'unicité de la clé.
- La durée de conservation.
- Le comportement en cas de requête différente.
- La manière de traiter les appels concurrents.
- La façon de gérer les effets secondaires.

Une clé d'idempotence doit être propagée lorsque plusieurs couches participent à la même opération logique, mais chaque service doit également protéger ses propres effets métier.

## 11. Bonnes pratiques

- Générer une clé suffisamment unique, souvent un UUID.
- Limiter la longueur et valider le format de la clé.
- Associer la clé à un périmètre explicite : client, endpoint ou opération.
- Calculer une empreinte stable de la requête normalisée.
- Rejeter la réutilisation d'une clé avec une requête différente.
- Réserver la clé atomiquement avant l'exécution.
- Définir le comportement des requêtes en cours.
- Conserver le résultat pendant une durée adaptée au métier.
- Protéger les données sensibles présentes dans les réponses mises en cache.
- Éviter de stocker inutilement des réponses volumineuses.
- Surveiller les conflits, les doublons évités et les traitements bloqués.
- Tester les appels simultanés, les timeouts et les redémarrages.

## 12. À retenir

L'idempotence est un mécanisme fondamental pour fiabiliser les API et les traitements distribués.

Elle permet de supporter les nouvelles tentatives sans reproduire les effets métier, à condition que la clé, le résultat et les effets métier soient coordonnés correctement.

**Principe clé :** une même opération logique peut être envoyée plusieurs fois, mais elle ne doit pas produire plusieurs fois le même effet métier.