# Resilience — Résilience des systèmes distribués

## 1. Définition

La **résilience** est la capacité d'un système informatique à faire face aux incidents, à limiter leur impact sur les utilisateurs et à retrouver un fonctionnement normal après une perturbation.

Dans une architecture distribuée, une application dépend de plusieurs composants qui peuvent échouer indépendamment les uns des autres : API partenaires, bases de données, services internes, brokers de messages ou infrastructures réseau.

Une architecture résiliente ne suppose pas que les pannes peuvent être totalement évitées. Elle est conçue pour les anticiper, les contenir et les gérer de manière contrôlée.

## 2. Pourquoi la résilience est-elle importante ?

Prenons une application de simulation financière qui appelle plusieurs services :

```text
                     Client
                       |
                       v
                API Simulation
                       |
          +------------+------------+
          |            |            |
          v            v            v
     Service Client  Tarification  Assurance
```

Si le service de tarification devient lent ou indisponible, l'application peut subir plusieurs conséquences :

- les requêtes restent bloquées ;
- les threads sont progressivement occupés ;
- les délais de réponse augmentent ;
- les nouvelles requêtes s'accumulent ;
- les autres fonctionnalités deviennent indisponibles à leur tour.

Ce phénomène est appelé **propagation de défaillance**.

La résilience vise à empêcher qu'un incident local entraîne une indisponibilité généralisée.

## 3. Les objectifs de la résilience

Une architecture résiliente cherche à atteindre plusieurs objectifs.

### 3.1. Détecter les défaillances

Identifier rapidement les erreurs, les dépassements de délais, les indisponibilités et les comportements anormaux.

Exemples : timeouts, erreurs HTTP `5xx`, hausse de latence ou échecs de connexion à la base de données.

### 3.2. Contenir les incidents

Limiter la propagation d'une défaillance aux composants qui en dépendent.

Exemples : circuit breaker, bulkhead et limitation des appels concurrents.

### 3.3. Maintenir un service acceptable

Continuer à fournir les fonctionnalités essentielles lorsque certaines fonctionnalités secondaires sont indisponibles.

Exemples : fallback, réponse dégradée ou désactivation temporaire d'une fonctionnalité non critique.

Cette dégradation doit respecter les règles métier. Une application financière ne doit pas inventer un taux ou un montant pour remplacer une donnée indisponible.

### 3.4. Récupérer le fonctionnement normal

Détecter le rétablissement d'un service, reprendre les appels de manière contrôlée et restaurer les fonctionnalités concernées.

Exemples : circuit breaker en mode semi-ouvert, reprise progressive du trafic et mécanismes de récupération.

## 4. Les principaux mécanismes de résilience

| Mécanisme | Rôle |
|---|---|
| Timeout | Limite le temps d'attente d'une opération. |
| Retry | Retente une opération après un échec temporaire. |
| Backoff | Espace les tentatives successives. |
| Circuit Breaker | Interrompt temporairement les appels vers un service défaillant. |
| Bulkhead | Isole les ressources pour limiter les effets d'une panne. |
| Fallback | Fournit une réponse alternative autorisée. |
| Rate Limiter | Limite le nombre d'appels sur une période donnée. |
| Health Check | Indique l'état de santé d'un composant. |
| Monitoring | Permet de suivre le comportement et les incidents. |

Ces mécanismes sont complémentaires. Aucun ne garantit à lui seul la résilience de l'ensemble du système.

## 5. Résilience et disponibilité

La résilience et la disponibilité sont liées, mais représentent deux notions différentes.

- **Disponibilité** : capacité d'un service à être accessible et opérationnel.
- **Résilience** : capacité à absorber les perturbations, à limiter leurs effets et à récupérer.

Par exemple, plusieurs instances d'une API derrière un répartiteur de charge peuvent améliorer sa disponibilité. Toutefois, si toutes les instances dépendent d'un même service partenaire indisponible, l'application peut rester incapable de traiter certaines opérations.

La disponibilité dépend donc aussi de la conception des dépendances et de la gestion des défaillances.

## 6. Résilience dans une architecture Spring Boot

Dans une application Spring Boot, les mécanismes de résilience peuvent être appliqués aux appels vers des services distants.

Une architecture illustrative peut être organisée ainsi :

```text
Client
  |
  v
SimulationController
  |
  v
SimulationService
  |
  v
PartnerResilienceService
  |
  +-- Timeout
  +-- Retry
  +-- Circuit Breaker
  +-- Fallback
  |
  v
PartnerFeignClient
  |
  v
API partenaire
```

Les responsabilités sont séparées :

- `SimulationController` expose les endpoints REST.
- `SimulationService` orchestre le traitement métier.
- `PartnerResilienceService` applique les protections nécessaires.
- `PartnerFeignClient` réalise l'appel HTTP.
- L'API partenaire fournit la réponse ou rencontre une défaillance.

Spring Cloud OpenFeign facilite les appels HTTP déclaratifs. Resilience4j permet d'appliquer différents mécanismes de résilience autour de ces appels.

## 7. Exemple avec Resilience4j

L'exemple suivant protège un appel partenaire avec un circuit breaker et un fallback.

```java
@Service
@RequiredArgsConstructor
public class PartnerResilienceService {

    private final PartnerFeignClient partnerFeignClient;

    @CircuitBreaker(
        name = "partnerApi",
        fallbackMethod = "partnerFallback"
    )
    public PartnerResponse getPartnerData() {
        return partnerFeignClient.getData();
    }

    private PartnerResponse partnerFallback(Throwable exception) {
        throw new PartnerUnavailableException(
            "Le service partenaire est temporairement indisponible",
            exception
        );
    }
}
```

Dans cet exemple, le fallback ne fournit pas de fausse réponse métier : il transforme l'échec en une exception applicative explicite.

La classe `PartnerUnavailableException` doit être définie dans le projet. Un gestionnaire d'exceptions peut ensuite convertir cette exception en une réponse HTTP adaptée, par exemple `503 Service Unavailable`.

Si le métier autorise une réponse dégradée, le fallback peut retourner un résultat alternatif valide plutôt que de relancer une exception.

## 8. Bonnes pratiques

- Définir les objectifs de disponibilité et les délais de réponse attendus.
- Identifier les dépendances critiques et les points uniques de défaillance.
- Configurer des timeouts explicites.
- Limiter les retries et utiliser un backoff adapté.
- Éviter les retries automatiques sur les opérations non idempotentes sans protection.
- Utiliser des circuit breakers et des mécanismes d'isolation lorsque cela est pertinent.
- Définir des fallbacks conformes aux règles métier.
- Mesurer les erreurs, la latence et la saturation des ressources.
- Tester les scénarios de panne, de surcharge et de récupération.
- Prévoir une reprise progressive du trafic après un incident.

## 9. Limites et points d'attention

La résilience ne se résume pas à ajouter des annotations sur les méthodes Java.

Elle doit être pensée à plusieurs niveaux :

- **Application** : gestion des erreurs et protection des appels.
- **Architecture** : séparation des responsabilités et isolation des dépendances.
- **Infrastructure** : redondance, réplication et basculement.
- **Exploitation** : supervision, alertes et procédures de reprise.
- **Métier** : définition des fonctionnalités essentielles et des dégradations acceptables.

Une configuration adaptée à un service à faible trafic peut être inadaptée à une API très sollicitée. Les paramètres doivent donc être définis et validés en fonction du contexte réel.

## 10. À retenir

La résilience est une propriété globale d'une application, et non un mécanisme isolé.

Elle repose sur la détection des incidents, leur confinement, le maintien d'un service acceptable et la récupération contrôlée.

Les patterns comme Timeout, Retry, Circuit Breaker, Bulkhead et Fallback sont des outils permettant de construire cette résilience, à condition de les configurer et de les tester selon les exigences métier.
