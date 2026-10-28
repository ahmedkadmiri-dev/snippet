# Saga Pattern — Transactions distribuées en microservices

## 1. Qu'est-ce que c'est ?

Le **Saga Pattern** est un pattern d'architecture qui permet de gérer une transaction métier répartie sur plusieurs microservices, sans utiliser une transaction distribuée globale.

Chaque microservice exécute sa propre transaction locale. Si une étape échoue, le système exécute des **transactions de compensation** pour annuler les effets des étapes précédentes.

### Exemple métier

Dans une plateforme de financement immobilier, une opération pourrait nécessiter :

1. Créer un dossier de financement.
2. Vérifier la capacité financière du client.
3. Enregistrer la demande auprès d'un autre service.
4. Confirmer la création du dossier.

Si la troisième étape échoue après la création du dossier, une action compensatoire peut annuler ou abandonner le dossier créé.

> Une compensation ne supprime pas nécessairement les données : elle rétablit un état métier cohérent.

## 2. Principe de fonctionnement

Une Saga est constituée d'une succession de transactions locales :

- `T1`, `T2`, `T3` : transactions métier.
- `C1`, `C2`, `C3` : transactions compensatoires correspondantes.

Exemple :

```text
T1 : Créer le dossier
        |
        v
T2 : Réserver les ressources
        |
        v
T3 : Confirmer l'opération
        |
        +---- Succès : Saga terminée
        |
        +---- Échec : compensation
                    |
                    v
              C2 : Libérer les ressources
                    |
                    v
              C1 : Annuler le dossier
```

Si `T3` échoue, les compensations sont exécutées dans l'ordre inverse des étapes déjà réussies.

Attention : les compensations ne constituent pas un rollback SQL global. Elles sont de nouvelles opérations métier, susceptibles d'échouer elles aussi.

## 3. Les deux approches

### 3.1. Saga par orchestration

Un orchestrateur central pilote le processus. Il appelle les services, décide de la prochaine étape et déclenche les compensations en cas d'échec.

```text
             Orchestrateur Saga
              /      |      \
             v       v       v
         Service A Service B Service C
```

**Avantages :**
- Flux métier explicite et centralisé.
- Gestion plus lisible des erreurs et compensations.
- Suivi de l'état global de la Saga.

**Inconvénients :**
- L'orchestrateur doit être maintenu et supervisé.
- Il faut éviter qu'il concentre toute la logique métier des services.

### 3.2. Saga par chorégraphie

Les microservices communiquent par événements. Chaque service réagit à un événement et publie le suivant.

```text
Service A
   |
   | DossierCree
   v
RabbitMQ / Kafka
   |
   v
Service B
   |
   | RessourcesReservees
   v
RabbitMQ / Kafka
   |
   v
Service C
```

**Avantages :**
- Faible couplage direct entre les services.
- Adaptée aux architectures événementielles.

**Inconvénients :**
- Le suivi du processus complet devient plus difficile.
- Les dépendances entre événements peuvent devenir complexes.

## 4. Comment l'intégrer avec Spring Boot ?

Pour une application Java/Spring Boot, une Saga orchestrée peut être implémentée avec un service dédié et des clients HTTP (`WebClient`, OpenFeign ou REST).

L'exemple ci-dessous illustre le principe avec deux services : création d'un dossier et réservation d'une ressource.

### 4.1. Définir les états de la Saga

```java
public enum SagaStatus {
    STARTED,
    DOSSIER_CREATED,
    RESOURCE_RESERVED,
    COMPLETED,
    COMPENSATING,
    FAILED
}
```

En production, l'état doit être persisté dans une base de données afin de reprendre le processus après un redémarrage.

### 4.2. Créer l'orchestrateur

```java
@Service
@RequiredArgsConstructor
public class FinancingSagaService {

    private final DossierClient dossierClient;
    private final ResourceClient resourceClient;
    private final SagaRepository sagaRepository;

    public void execute(Long dossierId) {
        SagaExecution saga = sagaRepository.save(
                SagaExecution.started(dossierId)
        );

        boolean dossierCreated = false;

        try {
            dossierClient.createDossier(dossierId);
            dossierCreated = true;

            saga.markDossierCreated();
            sagaRepository.save(saga);

            resourceClient.reserve(dossierId);

            saga.markResourceReserved();
            sagaRepository.save(saga);

            saga.markCompleted();
            sagaRepository.save(saga);

        } catch (Exception exception) {
            saga.markCompensating();
            sagaRepository.save(saga);

            if (dossierCreated) {
                try {
                    dossierClient.cancelDossier(dossierId);
                    saga.markFailed();
                } catch (Exception compensationException) {
                    // Conserver l'état pour une nouvelle tentative.
                    saga.markCompensationPending();
                }
            } else {
                saga.markFailed();
            }

            sagaRepository.save(saga);
            throw new SagaExecutionException(
                    "Échec de la Saga de financement", exception
            );
        }
    }
}
```

**Remarque :** ce code est un squelette pédagogique. Les classes `SagaExecution`, `SagaRepository`, les clients et les méthodes d'état sont à implémenter. En production, il faut aussi traiter les timeouts ambigus, les reprises et les compensations partielles sans supposer qu'une exception signifie toujours que l'opération distante n'a pas été exécutée.

### 4.3. Définir les clients des microservices

Exemple simplifié avec des interfaces :

```java
public interface DossierClient {

    void createDossier(Long dossierId);

    void cancelDossier(Long dossierId);
}
```

```java
public interface ResourceClient {

    void reserve(Long dossierId);

    void release(Long dossierId);
}
```

Les implémentations peuvent utiliser OpenFeign ou `WebClient` pour appeler les API des services concernés.

La compensation de la réservation doit également être prévue : `release(dossierId)`.

## 5. Saga et RabbitMQ / Kafka

Dans une architecture événementielle, les étapes peuvent être déclenchées par des messages.

Exemple de séquence :

```text
Financing Service
       |
       |