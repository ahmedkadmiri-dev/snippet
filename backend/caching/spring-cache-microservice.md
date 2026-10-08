# Spring Boot Microservice — Cache

## 🎯 Objectif

Le cache permet à un microservice de conserver temporairement le résultat d'une ressource afin d'éviter de refaire plusieurs fois la même opération coûteuse.

Cas typique :

```text
Client
  │
  │ GET /api/products/123
  ▼
┌──────────────────────┐
│ Product Microservice │
└──────────┬───────────┘
           │
           ▼
       ┌────────┐
       │ Cache  │
       └───┬────┘
           │
     Cache HIT ──────► Response
           │
     Cache MISS
           │
           ▼
      Database
           │
           ▼
        Cache
           │
           ▼
       Response
```

---

# 1. Quand utiliser un cache ?

Le cache est particulièrement adapté lorsqu'une ressource :

* est appelée très fréquemment ;
* retourne souvent les mêmes données ;
* nécessite un accès coûteux ;
* dépend d'une base de données ;
* dépend d'une API externe ;
* évolue relativement peu ;
* peut accepter une certaine durée de validité.

Exemple :

```text
GET /api/configurations/{code}
GET /api/referentials/{code}
GET /api/products/{id}
GET /api/parameters/{key}
```

---

# 2. Dépendance Spring Boot

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
```

Pour Redis :

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

---

# 3. Activer le cache

```java
@Configuration
@EnableCaching
public class CacheConfig {
}
```

---

# 4. Cache local

Pour un microservice avec une seule instance, un cache local en mémoire peut être suffisant.

Exemple avec Caffeine :

```xml
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```

Configuration :

```yaml
spring:
  cache:
    type: caffeine
    cache-names:
      - products
    caffeine:
      spec: maximumSize=1000,expireAfterWrite=10m
```

---

# 5. Utilisation avec @Cacheable

```java
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Cacheable(
        value = "products",
        key = "#id"
    )
    public Product getProduct(Long id) {

        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }
}
```

Premier appel :

```text
GET /products/10

Cache MISS
    ↓
Database
    ↓
Cache
    ↓
Response
```

Deuxième appel :

```text
GET /products/10

Cache HIT
    ↓
Response
```

La requête SQL n'est donc pas exécutée une deuxième fois.

---

# 6. Cache avec plusieurs paramètres

Pour une ressource dépendant de plusieurs paramètres :

```java
@Cacheable(
    value = "simulationResults",
    key = "#input.csp + ':' + #input.amount + ':' + #input.duration"
)
public SimulationResult calculate(SimulationInput input) {
    return calculateSimulation(input);
}
```

Exemple :

```text
SALA:200000:240
```

est utilisé comme clé du cache.

---

# 7. Attention à la clé

La clé doit représenter **tous les paramètres qui influencent le résultat**.

Mauvais exemple :

```java
@Cacheable(value = "simulations", key = "#input.csp")
```

Si le résultat dépend également de :

```text
csp
amount
duration
rateType
insurance
```

la clé doit prendre en compte ces paramètres.

Exemple :

```java
key = "#input.csp + ':' + #input.amount + ':' + #input.duration + ':' + #input.rateType"
```

Sinon deux requêtes différentes peuvent recevoir le même résultat.

---

# 8. Cache Redis dans un microservice

Dans une architecture avec plusieurs instances :

```text
                 ┌───────────────┐
                 │     Redis     │
                 └───────┬───────┘
                         │
              ┌──────────┴──────────┐
              │                     │
        ┌─────▼─────┐         ┌─────▼─────┐
        │ Instance 1│         │ Instance 2│
        │ Microservice        │ Microservice
        └───────────┘         └───────────┘
```

Le cache local n'est plus idéal car chaque instance possède son propre cache.

Redis permet de partager le cache.

Configuration :

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379

  cache:
    type: redis
```

---

# 9. TTL

Un cache doit généralement avoir une durée de vie.

Exemple :

```text
TTL = 10 minutes
```

Après 10 minutes :

```text
Cache entry
     ↓
   Expired
     ↓
Database
     ↓
New cache entry
```

Le TTL dépend de la nature de la donnée.

Exemple :

| Ressource               |      TTL possible |
| ----------------------- | ----------------: |
| Configuration technique |                1h |
| Référentiel             |                1h |
| Produit                 |            10 min |
| Résultat calculé        |             5 min |
| Donnée temps réel       | quelques secondes |

Le TTL doit être choisi selon la fréquence de modification et le niveau de fraîcheur attendu.

---

# 10. Invalidation du cache

Lorsqu'une donnée est modifiée, le cache doit être invalidé.

```java
@CacheEvict(
    value = "products",
    key = "#id"
)
public void updateProduct(Long id, ProductRequest request) {

    // update database
}
```

Pour vider tout le cache :

```java
@CacheEvict(
    value = "products",
    allEntries = true
)
public void clearProductCache() {
}
```

---

# 11. Cache PUT

Pour mettre explicitement une valeur dans le cache :

```java
@CachePut(
    value = "products",
    key = "#result.id"
)
public Product updateProduct(Long id, ProductRequest request) {

    return productRepository.update(id, request);
}
```

---

# 12. Cache conditionnel

On peut conditionner l'utilisation du cache :

```java
@Cacheable(
    value = "products",
    key = "#id",
    condition = "#id != null"
)
public Product getProduct(Long id) {
    ...
}
```

---

# 13. Ne pas mettre n'importe quelle donnée en cache

Éviter de mettre en cache :

* données personnelles sensibles ;
* données très volatiles ;
* informations nécessitant une cohérence immédiate ;
* données dépendant de l'utilisateur sans clé appropriée ;
* résultats dont le coût de recalcul est faible.

Attention notamment aux données :

```text
User
Authorization
Permissions
Account balance
Transaction status
```

---

# 14. Cache et cohérence

Le cache introduit potentiellement une différence entre :

```text
Database
   ≠
Cache
```

Exemple :

```text
DB : amount = 1000

Cache : amount = 900
```

Si la donnée est modifiée en base sans invalider le cache, l'application peut continuer à retourner l'ancienne valeur.

Il faut donc définir une stratégie :

* TTL ;
* invalidation ;
* refresh ;
* write-through ;
* cache-aside.

---

# 15. Pattern Cache-Aside

Le pattern le plus courant avec Spring :

```text
Application
    │
    ▼
Cache
 ┌──┴──┐
 │     │
 HIT   MISS
 │     │
 │     ▼
 │   Database
 │     │
 │     ▼
 │   Cache
 │
 ▼
Response
```

C'est généralement le modèle le plus simple pour un microservice.

---

# 16. Cache et concurrence

Attention aux appels simultanés :

```text
Request A ──► Cache MISS ──► DB
Request B ──► Cache MISS ──► DB
Request C ──► Cache MISS ──► DB
```

Les trois requêtes peuvent appeler la base en même temps.

Pour une ressource très sollicitée, on peut mettre en place une stratégie de **cache stampede protection**, par exemple :

* verrouillage ;
* single-flight ;
* refresh anticipé ;
* mécanisme de synchronisation ;
* configuration adaptée de Redis/Caffeine.

---

# 17. Cache vs Idempotency

Ces deux concepts sont différents.

### Cache

Objectif :

> Éviter de recalculer ou recharger une donnée.

```text
GET
  ↓
Cache HIT
  ↓
Retourner le résultat
```

### Idempotency

Objectif :

> Empêcher qu'une même opération métier soit exécutée plusieurs fois.

Exemple :

```text
POST /simulations

Idempotency-Key: ABC-123
```

Premier appel :

```text
ABC-123
   ↓
Créer simulation
   ↓
simulationId = 100
```

Retry :

```text
ABC-123
   ↓
Simulation déjà créée
   ↓
Retourner simulationId = 100
```

L'idempotence est particulièrement importante pour les opérations :

```text
POST
Payment
Order creation
Simulation creation
Reservation
Transfer
```

---

# 18. Règles à retenir

### Cache

```text
Même input
    +
Même résultat attendu
    +
Résultat réutilisable
    ↓
CACHE
```

### Idempotency

```text
Même opération
    +
Retry possible
    +
Ne pas exécuter deux fois
    ↓
IDEMPOTENCY
```

Un cache **ne doit pas être utilisé comme mécanisme d'idempotence métier**.

Pour l'idempotence, privilégier une persistance durable et une contrainte d'unicité en base.

---

# 19. Checklist production

Avant d'ajouter un cache :

* [ ] Identifier le coût réel de l'opération
* [ ] Vérifier la fréquence des appels
* [ ] Définir la clé de cache
* [ ] Définir le TTL
* [ ] Définir la stratégie d'invalidation
* [ ] Vérifier la taille maximale du cache
* [ ] Vérifier la mémoire disponible
* [ ] Déterminer cache local ou Redis
* [ ] Évaluer les problèmes de cohérence
* [ ] Prévoir le comportement en cas d'indisponibilité du cache
* [ ] Ajouter des métriques HIT / MISS
* [ ] Ne pas mettre de données sensibles sans analyse
* [ ] Tester les accès concurrents
* [ ] Tester le cache après déploiement

## Conclusion

Pour un microservice Spring Boot :

```text
Simple / une instance
        ↓
Caffeine / cache local

Plusieurs instances
        ↓
Redis / cache distribué

Opération métier avec retry
        ↓
Idempotency + persistance
```

Le cache est un mécanisme **d'optimisation**.

L'idempotence est un mécanisme **de fiabilité métier**.
