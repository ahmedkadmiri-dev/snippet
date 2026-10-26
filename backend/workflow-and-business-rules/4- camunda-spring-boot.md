# Camunda avec Spring Boot — Intégration et orchestration des workflows

## 1. Objectif

Camunda est une plateforme d'automatisation des processus permettant de modéliser et d'exécuter des workflows métier à partir de modèles BPMN et, selon la version utilisée, de décisions DMN.

Dans une architecture backend Java/Spring Boot, Camunda peut permettre de :

- Orchestrer plusieurs étapes métier.
- Coordonner les appels à plusieurs microservices.
- Gérer les tâches automatiques et humaines.
- Attendre des événements ou des réponses externes.
- Gérer les délais et les chemins alternatifs.
- Suivre l'état d'un processus métier.
- Exécuter des décisions métier modélisées en DMN, lorsque le moteur choisi le prend en charge.

Exemple : orchestrer une demande de financement depuis sa réception jusqu'à la décision finale, en coordonnant la validation, le scoring, l'instruction et la notification.

## 2. Architecture générale

Une intégration typique comprend les éléments suivants :

- **Spring Boot :** héberge les API métier et les composants d'intégration.
- **Camunda :** exécute les workflows et gère leur état.
- **Modèle BPMN :** décrit l'enchaînement du processus.
- **Modèle DMN :** formalise certaines décisions métier.
- **Microservices métier :** exécutent les traitements spécifiques à leur domaine.
- **Base de données :** stocke les données métier ; le moteur dispose également de son propre mécanisme de persistance selon son mode de déploiement.

Exemple conceptuel :

```text
Client / Frontend
       |
       v
API Spring Boot / Gateway
       |
       v
Moteur de workflow Camunda
       |
       +----> Service de validation
       |
       +----> Service de scoring
       |
       +----> Décision DMN
       |
       +----> Service de notification
```

Cette représentation est conceptuelle. Selon la version et l'architecture choisies, le moteur Camunda peut être intégré à l'application ou fonctionner comme une plateforme distante.

## 3. Camunda 7 et Camunda 8

Il est important de distinguer les deux générations avant de choisir les dépendances et les API.

| Critère | Camunda 7 | Camunda 8 |
|---|---|---|
| Moteur | Process Engine BPMN | Moteur Zeebe |
| Intégration Java | API Process Engine et intégration Spring | Client/API Camunda 8 et mécanismes d'intégration associés |
| Définition des processus | BPMN | BPMN |
| Décisions DMN | Prise en charge native dans le Process Engine | Capacités dépendantes de la version et des composants utilisés |
| Exécution des tâches | Jobs et delegates, notamment | Job workers |
| Déploiement | Moteur embarqué ou distant selon l'architecture | Généralement plateforme et moteur Zeebe |
| Configuration | Dépendances et propriétés propres à Camunda 7 | Configuration propre à Camunda 8 |

Ne mélange pas les dépendances, les annotations ou les API des deux versions. Les exemples ci-dessous illustrent Camunda 7 ; ils ne sont pas directement transposables à Camunda 8.

## 4. Organisation du projet Spring Boot

Une organisation possible pour Camunda 7 :

```text
src/main/
├── java/com/example/financing/
│   ├── controller/
│   │   └── WorkflowController.java
│   ├── service/
│   │   └── WorkflowService.java
│   ├── workflow/
│   │   ├── delegate/
│   │   │   ├── ValidateApplicationDelegate.java
│   │   │   └── NotifyCustomerDelegate.java
│   │   └── config/
│   └── domain/
│
└── resources/
    └── processes/
        ├── financing-process.bpmn
        └── financing-eligibility.dmn
```

Cette organisation est indicative. Le chemin de déploiement des modèles dépend de la configuration Spring Boot et de la version de Camunda.

## 5. Définir un workflow BPMN

Un fichier BPMN décrit le processus que le moteur doit exécuter.

Exemple simplifié :

```text
(Start)
   |
   v
[Validate Application]
   |
   v
[Evaluate Eligibility]
   |
   v
< Eligible? >
   /       \
 Yes        No
  |          |
  v          v
[Process]  [Notify Rejection]
  |          |
  v          v
(End)      (End)
```

Dans un modèle BPMN réel, les tâches automatiques peuvent être configurées comme des tâches de service, et les transitions conditionnelles utilisent des expressions ou les résultats d'une décision.

Le fichier BPMN décrit l'orchestration ; il ne remplace pas les traitements métier Java ni les API des microservices.

## 6. Exécuter une tâche avec Java — Camunda 7

Dans Camunda 7, une tâche de service peut déléguer son traitement à une classe Java.

Exemple :

```java
@Component
public class ValidateApplicationDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        String applicationId =
                (String) execution.getVariable("applicationId");

        // Appeler le service métier responsable de la validation.
        // Exemple conceptuel : applicationService.validate(applicationId).

        execution.setVariable("applicationValidated", true);
    }
}
```

Dans le modèle BPMN, la tâche de service doit référencer cette classe via la configuration prévue par Camunda 7, par exemple avec l'attribut `camunda:class`.

**Bonnes pratiques :**

- Garder les traitements métier dans les services métier.
- Éviter de mettre des règles complexes dans les delegates.
- Utiliser des variables de processus pour transmettre uniquement les données nécessaires.
- Gérer les exceptions techniques et métier selon leur nature.
- Ne pas supposer que le traitement sera exécuté une seule fois : les mécanismes de reprise peuvent entraîner une nouvelle exécution.

Le code est illustratif : il faut adapter les dépendances, le modèle BPMN et la configuration à la version réellement utilisée.

## 7. Démarrer un workflow depuis une API REST

Dans une application utilisant Camunda 7 et son API Java, un service peut démarrer une instance de processus.

```java
@Service
public class WorkflowService {

    private final RuntimeService runtimeService;

    public WorkflowService(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    public String startFinancingWorkflow(
            String applicationId,
            String customerId) {

        Map<String, Object> variables = new HashMap<>();
        variables.put("applicationId", applicationId);
        variables.put("customerId", customerId);

        ProcessInstance instance =
                runtimeService.startProcessInstanceByKey(
                        "financingProcess",
                        applicationId,
                        variables
                );

        return instance.getProcessInstanceId();
    }
}
```

Le `financingProcess` doit correspondre à la clé du processus définie dans le fichier BPMN.

L'utilisation de `applicationId` comme identifiant métier de corrélation peut aider à retrouver le workflow correspondant à une demande. Cela ne garantit toutefois pas à elle seule l'idempotence du démarrage.

L'API REST peut ensuite exposer cette fonctionnalité à un frontend ou à une gateway, avec les contrôles d'autorisation et de validation nécessaires.

## 8. Appeler un microservice depuis le workflow

Un workflow peut coordonner plusieurs microservices. Le choix du mécanisme d'appel dépend des exigences fonctionnelles et techniques.

| Mécanisme | Utilisation |
|---|---|
| Appel HTTP synchrone | Le résultat est nécessaire immédiatement |
| Message asynchrone | Le traitement peut se poursuivre après réception d'un événement |
| Job worker | Approche notamment utilisée avec Camunda 8 |
| Delegate Java | Approche disponible avec Camunda 7 |

Exemple d'orchestration :

1. Le workflow reçoit une demande.
2. Une tâche appelle le service de validation.
3. Une autre tâche récupère le résultat de scoring.
4. Le workflow évalue les conditions de poursuite.
5. Une tâche de notification est déclenchée.

Pour les appels HTTP, prévoir des délais d'attente, une gestion explicite des erreurs et des mécanismes de reprise adaptés. Ne pas maintenir une transaction de base de données ouverte pendant un long appel réseau.

## 9. Gestion des erreurs, retries et timeouts

Les erreurs peuvent être fonctionnelles ou techniques.

### Erreurs métier

Exemples :
- Dossier non éligible.
- Pièces justificatives manquantes.
- Demande refusée par une règle métier.

Ces résultats peuvent être représentés par des variables, des chemins BPMN alternatifs ou des événements d'erreur BPMN, selon le modèle.

### Erreurs techniques

Exemples :
- Service de scoring indisponible.
- Timeout HTTP.
- Échec temporaire de connexion à une dépendance.

Selon le moteur et la configuration, on peut prévoir :
- Des tentatives de reprise.
- Des délais entre les tentatives.
- Un traitement alternatif.
- Une alerte ou une intervention manuelle.
- Une compensation métier si une étape précédente a déjà produit un effet.

La stratégie doit éviter les boucles infinies et les répétitions d'effets métier non idempotents.

## 10. BPMN et DMN dans un même workflow

BPMN et DMN sont complémentaires :

- BPMN orchestre le parcours.
- DMN évalue les règles de décision.
- Les services Spring Boot réalisent les traitements métier.

Exemple : le workflow reçoit une demande, appelle une décision d'éligibilité, puis poursuit l'instruction ou déclenche une notification de refus selon le résultat.

Le support exact de DMN dépend de la version de Camunda et des composants déployés. Il faut vérifier cette compatibilité avant de définir les dépendances et les mécanismes d'exécution.

## 11. Sécurité et architecture de production

Pour une architecture bancaire ou d'entreprise, prévoir notamment :

- Une authentification et une autorisation adaptées pour les API et les opérations de workflow.
- La protection des variables contenant des données sensibles.
- La séparation entre les droits de consultation, de démarrage et d'administration des processus.
- La supervision des instances bloquées et des jobs en échec.
- La gestion des versions des modèles BPMN et DMN.
- Des politiques de rétention des historiques et des données techniques.
- Des contrôles d'idempotence pour les appels externes susceptibles d'être répétés.
- Une stratégie de sauvegarde et de restauration du stockage du moteur.
- Des tests de reprise après panne et de comportement en cas d'indisponibilité d'un microservice.

Ne stocke pas systématiquement des données personnelles ou des objets métier complets dans les variables du workflow. Privilégie des identifiants et les informations strictement nécessaires au processus.

## 12. Bonnes pratiques

- Définir les frontières métier avant de modéliser le workflow.
- Utiliser BPMN pour l'orchestration, DMN pour les décisions adaptées à cette notation et Java pour les traitements métier.
- Maintenir des modèles lisibles et limiter les processus trop volumineux.
- Définir des conventions de nommage pour les processus, tâches et variables.
- Versionner les modèles avec le code et maîtriser leur déploiement.
- Tester les chemins nominaux, les erreurs et les reprises.
- Éviter de faire du moteur de workflow un substitut aux microservices métier.
- Concevoir les opérations externes pour supporter les répétitions lorsque cela est possible.
- Documenter les règles de corrélation, les délais et les stratégies de reprise.

## 13. Résumé

Camunda peut fournir le moteur d'exécution nécessaire pour orchestrer un processus métier décrit en BPMN et, selon la version choisie, utiliser des décisions DMN.

Dans une architecture Spring Boot et microservices :

- BPMN définit le parcours.
- DMN formalise certaines décisions.
- Les microservices portent les traitements métier.
- Camunda gère l'exécution et l'état du workflow.
- Les mécanismes de sécurité, de reprise et d'observabilité assurent son exploitation.

Avant l'implémentation, choisir explicitement Camunda 7 ou Camunda 8, puis aligner les dépendances, les exemples de code, la configuration et la stratégie de déploiement sur cette version.