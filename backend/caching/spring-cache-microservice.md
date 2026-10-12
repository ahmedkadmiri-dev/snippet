# Spring Boot Microservice — Cache

## 🎯 Objectif

Ce document présente progressivement :

1.  **Le cache** : définition, fonctionnement et cas d'utilisation.
2.  **Le cache local** avec Caffeine.
3.  **Le cache distribué Redis** pour une architecture composée de
    plusieurs instances.
4.  **Redis avec fallback local Caffeine** si Redis devient
    indisponible.
5.  **La différence entre cache et idempotence**, notamment pour les API
    POST/PATCH.

Exemples basés sur Spring Boot 3 et Java 21. Les versions des
dépendances sont à laisser gérer par le parent/BOM Spring Boot.

------------------------------------------------------------------------

# 1. Qu'est-ce qu'un cache ?

Un cache est un mécanisme qui conserve temporairement le résultat d'une
opération afin d'éviter de refaire plusieurs fois le même travail.

Exemple : une API consulte une base de données pour récupérer un
référentiel. Si la même donnée est demandée plusieurs fois, le cache
peut retourner le résultat sans exécuter à nouveau la requête SQL.

``` text
Client
  |
  | GET /api/referentiels/segments
  v
Microservice
  |
  v
Cache
  |
  +-- Cache HIT  --------------------> Retourner la valeur en cache
  |
  +-- Cache MISS
          |
          v
      Base de données
          |
          v
      Stocker le résultat en cache
          |
          v
      Retourner le résultat au client
```

## 1.1 Cache HIT et Cache MISS

-   **Cache HIT** : la donnée demandée existe dans le cache et peut être
    réutilisée.
-   **Cache MISS** : la donnée n'est pas présente, a expiré ou a été
    évincée. L'application doit alors la charger ou la calculer à
    nouveau.

## 1.2 Pourquoi utiliser un cache ?

Le cache peut permettre de :

-   réduire le nombre de requêtes vers la base de données ;
-   diminuer la latence des réponses ;
-   réduire les appels vers des API externes ;
-   éviter de recalculer plusieurs fois un résultat coûteux ;
-   absorber une partie des lectures répétitives.

Le cache est une **optimisation de performance**, et non la source de
vérité des données.

------------------------------------------------------------------------

# 2. Quand utiliser un cache ?

Un cache est pertinent lorsque la donnée :

-   est fréquemment consultée ;
-   change moins souvent qu'elle n'est lue ;
-   coûte cher à charger ou à calculer ;
-   peut être réutilisée pendant une durée déterminée ;
-   tolère éventuellement un léger décalage par rapport à la base de
    données.

Exemples :

``` text
GET /api/referentiels/{code}
GET /api/configurations/{code}
GET /api/parametres/{key}
GET /api/produits/{id}
```

Les référentiels, paramètres techniques et certaines données de
consultation sont de bons candidats.

## 2.1 Quand éviter le cache ?

Éviter ou analyser soigneusement le cache pour :

-   les données qui changent constamment ;
-   les données nécessitant une cohérence immédiate ;
-   les informations personnelles ou sensibles ;
-   les résultats personnalisés si la clé ne distingue pas les
    utilisateurs ou les contextes ;
-   les opérations dont le coût est négligeable.

Exemples qui exigent une attention particulière : solde bancaire, statut
d'une transaction, droits d'accès, données d'un client et résultats
métier dépendant de règles qui évoluent.

------------------------------------------------------------------------

# 3. Les stratégies de cache

## 3.1 Cache-Aside

C'est un modèle courant avec Spring Cache :

1.  l'application consulte le cache ;
2.  si la donnée est présente, elle est retournée ;
3.  sinon, l'application charge la donnée depuis la source ;
4.  le résultat est placé dans le cache ;
5.  les prochains appels peuvent réutiliser le résultat.

``` text
Application
    |
    v
  Cache
    |
    +-- HIT  ----------------------> Résultat
    |
    +-- MISS
          |
          v
     Base de données
          |
          v
     Écriture en cache
          |
          v
       Résultat
```

Avec `@Cacheable`, Spring implémente ce comportement autour de l'appel
de méthode.

## 3.2 TTL --- Time To Live

Le TTL définit combien de temps une entrée peut rester en cache avant
d'expirer.

Exemples indicatifs :

  Type de donnée                                           TTL possible
  --------------------------------- -----------------------------------
  Référentiel peu changeant                                     1 heure
  Paramètre métier                                      15 à 30 minutes
  Résultat de calcul réutilisable                         1 à 5 minutes
  Donnée très volatile                Quelques secondes, ou aucun cache

Le TTL doit être choisi selon la fréquence de modification et le niveau
de fraîcheur attendu. Il ne remplace pas une stratégie d'invalidation.

## 3.3 Invalidation

Après une modification en base, une entrée du cache peut devenir
obsolète. Il faut donc décider si l'on doit :

-   supprimer l'entrée concernée ;
-   vider toutes les entrées d'un cache ;
-   attendre l'expiration du TTL ;
-   rafraîchir explicitement la donnée.

------------------------------------------------------------------------

# 4. Activer le cache dans Spring Boot

## 4.1 Dépendance Maven

``` xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
```

## 4.2 Configuration minimale

``` java
package ma.wafaimmobilier.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {
}
```

`@EnableCaching` active la prise en charge des annotations comme
`@Cacheable`, `@CachePut` et `@CacheEvict`. Cette annotation ne choisit
pas à elle seule le type de stockage : Spring doit aussi disposer d'un
`CacheManager`.

------------------------------------------------------------------------

# 5. Exemple d'utilisation de `@Cacheable`

``` java
package ma.wafaimmobilier.service;

import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class ReferentielService {

    @Cacheable(cacheNames = "referentiels", key = "#type")
    public List<String> getReferentiel(String type) {
        // Remplacer par le chargement réel depuis la base ou une API.
        return chargerDepuisLaBase(type);
    }

    private List<String> chargerDepuisLaBase(String type) {
        return List.of("A", "B", "C");
    }
}
```

Au premier appel pour `type = "segments"`, la méthode est exécutée et
son résultat est mis en cache. Lors des appels suivants avec la même
clé, Spring peut retourner la valeur en cache.

## 5.1 Invalidation avec `@CacheEvict`

``` java
@CacheEvict(cacheNames = "referentiels", key = "#type")
public void modifierReferentiel(String type) {
    // Effectuer ici la modification réelle en base.
}
```

Pour invalider toutes les entrées du cache :

``` java
@CacheEvict(cacheNames = "referentiels", allEntries = true)
public void invaliderTousLesReferentiels() {
    // Méthode dédiée à l'invalidation.
}
```

Attention : si la méthode modifie la base, l'opération métier doit
réellement être implémentée. L'annotation d'invalidation ne modifie pas
la base à elle seule.

------------------------------------------------------------------------

# 6. Choisir entre cache local et cache distribué

## 6.1 Cache local

Un cache local conserve les entrées dans la mémoire du processus Java.

Exemple : **Caffeine**.

``` text
                 Microservice
              ┌────────────────┐
Requête ----->│ Cache Caffeine │----> Base si MISS
              └────────────────┘
```

Avantages : - très rapide, car les données sont dans la mémoire du
processus ; - pas de dépendance réseau pour consulter le cache ; -
configuration simple.

Limites : - chaque instance possède son propre cache ; - les entrées
sont perdues au redémarrage ; - une invalidation sur une instance
n'invalide pas automatiquement les autres ; - la mémoire utilisée doit
être limitée.

## 6.2 Cache distribué

Un cache distribué conserve les entrées dans un service externe, partagé
par les instances. **Redis** est un exemple courant.

``` text
       ┌──────────────────────┐
       │         Redis        │
       │   Cache partagé      │
       └──────────┬───────────┘
                  |
          ┌───────┴────────┐
          |                |
   ┌──────▼──────┐  ┌──────▼──────┐
   │ Instance 1  │  │ Instance 2  │
   │ Microservice│  │ Microservice│
   └─────────────┘  └─────────────┘
```

Avantages : - plusieurs instances peuvent partager les mêmes entrées ; -
le cache ne disparaît pas simplement parce qu'une instance applicative
redémarre ; - la mémoire du cache est séparée de celle des JVM.

Limites : - dépendance réseau et service supplémentaire à superviser ; -
latence supérieure à un cache local ; - disponibilité et cohérence à
prendre en compte ; - configuration de sécurité, de capacité et
d'expiration nécessaire.

------------------------------------------------------------------------

# 7. Configurer Redis comme cache principal

## 7.1 Dépendances Maven

Avec Spring Boot, laisser le parent/BOM gérer les versions.

``` xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-cache</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>

    <dependency>
        <groupId>com.github.ben-manes.caffeine</groupId>
        <artifactId>caffeine</artifactId>
    </dependency>
</dependencies>
```

Caffeine est ajouté ici parce qu'il sera utilisé dans la stratégie de
fallback présentée plus loin.

## 7.2 Configuration `application.yml`

``` yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      username: ${REDIS_USERNAME:}
      password: ${REDIS_PASSWORD:}
      connect-timeout: 2s
      timeout: 2s
```

Les valeurs par défaut conviennent à un environnement local de
développement. En recette et en production, configurer les secrets et
les paramètres réseau selon l'infrastructure. Ne pas committer les mots
de passe.

## 7.3 `CacheConfig` avec Redis et des TTL par cache

La configuration ci-dessous crée un cache Redis pour les référentiels,
les paramètres et les résultats de simulation.

``` java
package ma.wafaimmobilier.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration
                .defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        cacheConfigurations.put(
                "referentiels",
                defaultConfig.entryTtl(Duration.ofHours(1)));

        cacheConfigurations.put(
                "parametres",
                defaultConfig.entryTtl(Duration.ofMinutes(30)));

        cacheConfigurations.put(
                "simulations",
                defaultConfig.entryTtl(Duration.ofMinutes(5)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}
```

> Vérifier le sérialiseur JSON avec la version Spring Data Redis gérée
> par votre version de Spring Boot. Tester la sérialisation et la
> désérialisation des types réellement placés en cache avant le
> déploiement.

## 7.4 Lancer Redis localement avec Docker

``` bash
docker run --name redis-cache -p 6379:6379 -d redis:7-alpine
```

Tester la connexion :

``` bash
docker exec -it redis-cache redis-cli ping
```

Réponse attendue :

``` text
PONG
```

Cette commande démarre Redis sans authentification : usage réservé à un
environnement local isolé, jamais à une exposition directe en recette ou
en production.

------------------------------------------------------------------------

# 8. Redis avec fallback local Caffeine

## 8.1 Pourquoi un fallback ?

Redis est le cache principal partagé. Mais si Redis devient
indisponible, les opérations de cache peuvent échouer et perturber les
appels applicatifs.

Une stratégie de fallback consiste à utiliser Caffeine localement quand
une opération Redis échoue.

``` text
                 Requête applicative
                         |
                         v
                  Cache Redis
                    /      \
               Succès      Erreur
                 |            |
                 v            v
            Valeur Redis   Cache Caffeine
                              local
```

Comportement attendu :

1.  l'application essaie Redis ;
2.  si l'opération Redis réussit, son résultat est utilisé ;
3.  si l'opération Redis échoue, l'application consulte ou met à jour
    Caffeine ;
4.  les requêtes suivantes réessaient Redis : le fallback ne désactive
    pas définitivement Redis.

## 8.2 Limite fondamentale du fallback

Le fallback améliore la disponibilité, mais **il ne garantit pas une
cohérence parfaite** :

-   chaque instance possède son propre cache Caffeine ;
-   les écritures locales réalisées pendant une panne ne sont pas
    automatiquement recopiées dans Redis ;
-   les invalidations locales ne sont pas automatiquement rejouées dans
    Redis ;
-   au rétablissement de Redis, une ancienne entrée Redis peut être
    préférée à une valeur locale plus récente.

Il faut donc réserver cette approche aux données dont le niveau de
cohérence tolère ces limites, utiliser des TTL locaux courts et
surveiller les erreurs Redis.

## 8.3 `CacheConfig` Redis + Caffeine

Le code suivant configure les deux `CacheManager`, puis expose un
gestionnaire de cache avec fallback.

``` java
package ma.wafaimmobilier.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableCaching
public class CacheConfig {

    private static final List<String> CACHE_NAMES =
            List.of("referentiels", "parametres", "simulations");

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultRedisConfig = RedisCacheConfiguration
                .defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        Map<String, RedisCacheConfiguration> redisConfigs = new HashMap<>();
        redisConfigs.put("referentiels",
                defaultRedisConfig.entryTtl(Duration.ofHours(1)));
        redisConfigs.put("parametres",
                defaultRedisConfig.entryTtl(Duration.ofMinutes(30)));
        redisConfigs.put("simulations",
                defaultRedisConfig.entryTtl(Duration.ofMinutes(5)));

        CacheManager redisManager = RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultRedisConfig)
                .withInitialCacheConfigurations(redisConfigs)
                .build();

        CaffeineCacheManager caffeineManager = new CaffeineCacheManager();
        caffeineManager.setCacheNames(CACHE_NAMES);
        caffeineManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(Duration.ofMinutes(2))
                .recordStats());

        return new FallbackCacheManager(redisManager, caffeineManager, CACHE_NAMES);
    }
}
```

## 8.4 Classe `FallbackCacheManager`

Créer le fichier `FallbackCacheManager.java` dans le même package. Ce
wrapper essaie d'abord Redis et bascule sur Caffeine si une opération du
cache Redis lève une `RuntimeException`.

``` java
package ma.wafaimmobilier.config;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.lang.Nullable;

public class FallbackCacheManager implements CacheManager {

    private static final Logger log =
            LoggerFactory.getLogger(FallbackCacheManager.class);

    private final Map<String, Cache> caches = new ConcurrentHashMap<>();

    public FallbackCacheManager(
            CacheManager primary,
            CacheManager fallback,
            Collection<String> cacheNames) {

        for (String name : cacheNames) {
            Cache primaryCache = primary.getCache(name);
            Cache fallbackCache = fallback.getCache(name);

            if (primaryCache == null || fallbackCache == null) {
                throw new IllegalStateException("Cache non configuré : " + name);
            }

            caches.put(name, new FallbackCache(primaryCache, fallbackCache));
        }
    }

    @Override
    @Nullable
    public Cache getCache(String name) {
        return caches.get(name);
    }

    @Override
    public Collection<String> getCacheNames() {
        return caches.keySet();
    }

    private static final class FallbackCache implements Cache {

        private final Cache primary;
        private final Cache fallback;

        private FallbackCache(Cache primary, Cache fallback) {
            this.primary = primary;
            this.fallback = fallback;
        }

        @Override
        public String getName() {
            return primary.getName();
        }

        @Override
        public Object getNativeCache() {
            return primary.getNativeCache();
        }

        @Override
        @Nullable
        public ValueWrapper get(Object key) {
            try {
                return primary.get(key);
            } catch (RuntimeException ex) {
                log.warn("Erreur Redis sur get, cache '{}'. Fallback local activé.",
                        getName(), ex);
                return fallback.get(key);
            }
        }

        @Override
        @Nullable
        public <T> T get(Object key, @Nullable Class<T> type) {
            try {
                return primary.get(key, type);
            } catch (RuntimeException ex) {
                log.warn("Erreur Redis sur get(type), cache '{}'. Fallback local activé.",
                        getName(), ex);
                return fallback.get(key, type);
            }
        }

        @Override
        @Nullable
        public <T> T get(Object key, Callable<T> valueLoader) {
            try {
                return primary.get(key, valueLoader);
            } catch (RuntimeException ex) {
                log.warn("Erreur Redis sur get(valueLoader), cache '{}'. Fallback local activé.",
                        getName(), ex);
                return fallback.get(key, valueLoader);
            }
        }

        @Override
        public void put(Object key, @Nullable Object value) {
            try {
                primary.put(key, value);
            } catch (RuntimeException ex) {
                log.warn("Erreur Redis sur put, cache '{}'. Écriture locale utilisée.",
                        getName(), ex);
                fallback.put(key, value);
            }
        }

        @Override
        public void evict(Object key) {
            try {
                primary.evict(key);
            } catch (RuntimeException ex) {
                log.warn("Erreur Redis sur evict, cache '{}'. Éviction locale utilisée.",
                        getName(), ex);
                fallback.evict(key);
            }
        }

        @Override
        public void clear() {
            try {
                primary.clear();
            } catch (RuntimeException ex) {
                log.warn("Erreur Redis sur clear, cache '{}'. Nettoyage local utilisé.",
                        getName(), ex);
                fallback.clear();
            }
        }

        @Override
        @Nullable
        public ValueWrapper putIfAbsent(Object key, @Nullable Object value) {
            try {
                return primary.putIfAbsent(key, value);
            } catch (RuntimeException ex) {
                log.warn("Erreur Redis sur putIfAbsent, cache '{}'. Fallback local utilisé.",
                        getName(), ex);
                return fallback.putIfAbsent(key, value);
            }
        }

        @Override
        public boolean evictIfPresent(Object key) {
            try {
                return primary.evictIfPresent(key);
            } catch (RuntimeException ex) {
                log.warn("Erreur Redis sur evictIfPresent, cache '{}'. Fallback local utilisé.",
                        getName(), ex);
                return fallback.evictIfPresent(key);
            }
        }

        @Override
        public boolean invalidate() {
            try {
                return primary.invalidate();
            } catch (RuntimeException ex) {
                log.warn("Erreur Redis sur invalidate, cache '{}'. Fallback local utilisé.",
                        getName(), ex);
                return fallback.invalidate();
            }
        }
    }
}
```

> Note de compatibilité : l'interface `Cache` évolue selon les versions
> de Spring Framework. Compiler ce code avec la version de Spring Boot
> du projet et adapter les méthodes si l'interface utilisée diffère.
> Supprimer les imports inutilisés si l'IDE le signale.

### Attention à `@Cacheable` et au fallback

Ce wrapper intercepte les erreurs levées par les opérations du cache.
Selon le chemin d'exécution interne de Spring Cache, le chargement de la
valeur après un `MISS` et l'écriture du résultat peuvent emprunter des
méthodes différentes. Il faut tester explicitement le comportement avec
la version Spring Boot utilisée, Redis arrêté, et une méthode annotée
`@Cacheable`.

Cette stratégie est un exemple d'architecture à valider par des tests de
panne, pas une garantie de cohérence distribuée.

------------------------------------------------------------------------

# 9. Utiliser les caches dans les services

L'utilisation côté service reste identique, que le cache principal soit
Redis seul ou Redis avec fallback.

``` java
@Service
public class ReferentielService {

    @Cacheable(cacheNames = "referentiels", key = "#type")
    public List<String> getReferentiel(String type) {
        return chargerDepuisLaBase(type);
    }

    @CacheEvict(cacheNames = "referentiels", key = "#type")
    public void modifierReferentiel(String type) {
        // Effectuer la mise à jour réelle en base.
    }

    private List<String> chargerDepuisLaBase(String type) {
        return List.of("A", "B", "C");
    }
}
```

La clé doit représenter tous les paramètres qui influencent le résultat.
Par exemple, pour un résultat de simulation, elle pourrait devoir
inclure le profil, le montant, la durée, le type de taux et la version
des paramètres métier. Une clé incomplète peut faire retourner le
résultat d'une requête différente.

------------------------------------------------------------------------

# 10. Cache et idempotence : quelle différence ?

Le cache et l'idempotence répondent à deux problèmes différents.

## 10.1 Cache : éviter de refaire un travail

Objectif : **améliorer les performances** en réutilisant une valeur.

``` text
GET /referentiels/segments
        |
        v
     Cache HIT
        |
        v
Retourner les données
```

Le cache peut expirer, être vidé ou perdre ses entrées. Il n'est donc
pas une preuve qu'une opération métier a déjà été exécutée.

## 10.2 Idempotence : empêcher les doublons lors des retries

Objectif : **ne pas exécuter plusieurs fois la même opération métier**
lorsqu'un client réessaie une requête après un timeout ou une réponse
perdue.

Exemple :

``` http
POST /api/simulations
Idempotency-Key: 6a73c9b4-...
Content-Type: application/json
```

Premier appel :

``` text
Idempotency-Key
       |
       v
Vérifier la clé
       |
       v
Créer la simulation
       |
       v
Enregistrer la réponse associée à la clé
```

Nouvel appel avec la même clé et la même opération :

``` text
Même Idempotency-Key
       |
       v
Clé déjà traitée
       |
       v
Retourner la réponse mémorisée
Sans recréer la simulation
```

Le comportement précis doit être défini : même clé avec payload
différent, requête encore en cours, expiration de la clé, erreur métier
et erreur technique.

## 10.3 Tableau comparatif

  -----------------------------------------------------------------------
  Critère                 Cache                   Idempotence
  ----------------------- ----------------------- -----------------------
  Objectif                Réduire le temps de     Empêcher l'exécution
                          réponse et le coût      répétée d'une opération

  Cas typique             GET de référentiel      POST de création ou
                                                  opération avec effet
                                                  métier

  Clé                     Paramètres de la donnée `Idempotency-Key`
                          ou du calcul            fournie pour
                                                  l'opération

  Valeur stockée          Résultat réutilisable   État de traitement et
                                                  réponse associée

  Expiration              Selon la fraîcheur      Selon la politique de
                          acceptable              retry et la garantie
                                                  attendue

  Perte des données       Rechargement/recalcul   Peut permettre un
                          généralement possible   doublon si aucune autre
                                                  protection durable
                                                  n'existe

  Garantie métier         Non                     Doit être conçue
                                                  explicitement,
                                                  notamment en
                                                  concurrence
  -----------------------------------------------------------------------

## 10.4 Peut-on utiliser Redis pour l'idempotence ?

Oui, Redis peut stocker les clés d'idempotence et les réponses. Mais
**le simple fait de stocker une clé en cache ne suffit pas**.

Une implémentation correcte doit notamment : - réserver atomiquement une
clé pour éviter que deux requêtes concurrentes ne créent deux
simulations ; - distinguer un traitement en cours d'un traitement
terminé ; - vérifier le payload associé à la clé ; - mémoriser le statut
HTTP et le corps de la réponse à restituer ; - définir la durée de
conservation ; - gérer les erreurs et les reprises ; - évaluer le risque
de perte des clés si Redis redémarre ou devient indisponible.

Pour une création métier importante, une contrainte d'unicité ou une
persistance durable en base peut être nécessaire en complément. Le
fallback Caffeine n'est pas adapté à une garantie d'idempotence partagée
entre plusieurs instances : chaque JVM possède son propre état local.

------------------------------------------------------------------------

# 11. Tester la configuration Redis + fallback

Checklist de test :

-   [ ] Démarrer Redis et l'application.
-   [ ] Appeler deux fois une méthode `@Cacheable` avec la même clé.
-   [ ] Vérifier que le second appel réutilise la valeur.
-   [ ] Arrêter Redis et vérifier que l'application bascule vers
    Caffeine.
-   [ ] Tester une écriture de cache pendant la panne.
-   [ ] Tester `@CacheEvict` pendant la panne.
-   [ ] Redémarrer Redis et vérifier que les appels retentent Redis.
-   [ ] Vérifier les éventuelles valeurs obsolètes après le
    rétablissement.
-   [ ] Tester la sérialisation des DTO réels.
-   [ ] Mesurer les timeouts et vérifier que la panne Redis ne bloque
    pas trop longtemps les requêtes.
-   [ ] Tester les retries concurrents d'une API utilisant
    `Idempotency-Key`.

Le fallback ne rend pas la panne détectable instantanément : le délai
avant l'exception dépend des timeouts et de la configuration réseau.

------------------------------------------------------------------------

# 12. Checklist avant mise en production

-   [ ] Choisir un TTL cohérent pour chaque cache.
-   [ ] Définir une stratégie d'invalidation après les mises à jour.
-   [ ] Limiter la taille du cache local Caffeine.
-   [ ] Configurer Redis avec authentification, contrôle réseau et TLS
    si nécessaire.
-   [ ] Stocker les secrets hors du code et des fichiers versionnés.
-   [ ] Ajouter des métriques pour les hits/misses, les erreurs Redis et
    les bascules locales.
-   [ ] Ne pas mettre en cache de données sensibles sans analyse
    préalable.
-   [ ] Vérifier la compatibilité du sérialiseur et des DTO.
-   [ ] Tester les indisponibilités Redis et les redémarrages.
-   [ ] Documenter les incohérences acceptables pendant le fallback.
-   [ ] Ne pas utiliser le fallback local comme mécanisme d'idempotence
    distribuée.

------------------------------------------------------------------------

# Conclusion

``` text
Besoin de réutiliser une donnée pour accélérer les lectures
        |
        v
       CACHE

Plusieurs instances doivent partager les entrées
        |
        v
       REDIS

Redis peut être indisponible et la donnée tolère un cache local temporaire
        |
        v
REDIS + FALLBACK CAFFEINE
        |
        v
Accepter les limites de cohérence entre instances

Éviter de créer deux fois la même ressource après un retry
        |
        v
    IDEMPOTENCE
        |
        v
Traitement dédié, concurrence maîtrisée et persistance adaptée
```

**À retenir :** le cache est un mécanisme d'optimisation ; l'idempotence
est un mécanisme de fiabilité métier. Redis peut servir aux deux, mais
avec des stratégies distinctes.
