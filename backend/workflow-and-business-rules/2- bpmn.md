# BPMN — Business Process Model and Notation

## 1. Objectif

**BPMN (Business Process Model and Notation)** est une notation graphique standardisée permettant de modéliser les processus métier sous forme de diagrammes.

Elle permet de représenter visuellement :

- Les étapes d'un processus métier.
- Les décisions et les conditions de passage.
- Les événements déclencheurs et les événements de fin.
- Les interactions entre les acteurs et les systèmes.
- Les traitements automatiques et les tâches humaines.
- Les chemins alternatifs et la gestion des erreurs.

BPMN facilite la communication entre les équipes métier, les architectes, les développeurs et les responsables de processus.

## 2. Pourquoi utiliser BPMN ?

BPMN permet de transformer un processus métier complexe en un modèle compréhensible et exploitable.

Ses principaux avantages sont :

- **Lisibilité métier :** représenter un processus sans se limiter au code.
- **Standardisation :** utiliser une notation commune entre les équipes.
- **Automatisation :** exécuter certains modèles avec un moteur de workflow compatible, comme Camunda.
- **Traçabilité :** visualiser les étapes franchies et les décisions prises.
- **Maintenance :** faire évoluer le processus sans devoir réécrire toute sa logique métier.

Attention : un diagramme BPMN peut être utilisé uniquement comme documentation. Pour qu'il soit exécutable, il doit respecter les capacités et les contraintes du moteur de workflow choisi.

## 3. Les principaux éléments BPMN

### 3.1. Événements — Events

Les événements représentent quelque chose qui se produit pendant le processus.

| Élément | Signification | Exemple |
|---|---|---|
| Start Event | Début du processus | Demande de financement reçue |
| Intermediate Event | Événement intermédiaire | Réception d'une réponse externe |
| End Event | Fin du processus | Demande traitée |
| Timer Event | Déclenchement lié au temps | Expiration du délai de validation |
| Message Event | Réception ou émission d'un message | Réception d'une décision de scoring |
| Error Event | Survenue ou interception d'une erreur | Échec d'un traitement |

### 3.2. Activités — Activities

Les activités représentent les opérations exécutées pendant le processus.

| Élément | Signification | Exemple |
|---|---|---|
| Task | Tâche élémentaire | Vérifier les données du client |
| User Task | Tâche réalisée par un utilisateur | Valider un dossier |
| Service Task | Tâche exécutée par un service | Appeler une API de scoring |
| Business Rule Task | Évaluer des règles métier | Déterminer l'éligibilité |
| Sub-Process | Sous-processus composé de plusieurs étapes | Étudier un dossier de financement |

### 3.3. Passerelles — Gateways

Les passerelles contrôlent les chemins suivis par le processus.

| Passerelle | Utilisation | Exemple |
|---|---|---|
| Exclusive Gateway (XOR) | Choisir un seul chemin | Dossier accepté ou refusé |
| Parallel Gateway (AND) | Exécuter plusieurs chemins en parallèle | Contrôle des revenus et contrôle des pièces |
| Inclusive Gateway (OR) | Activer un ou plusieurs chemins selon les conditions | Contrôles complémentaires selon le profil |
| Event-Based Gateway | Choisir le chemin selon le premier événement reçu | Réponse reçue ou délai expiré |

**Point important :** une passerelle XOR ne signifie pas que les deux branches peuvent être exécutées. Elle sélectionne un seul chemin selon les conditions définies.

### 3.4. Flux — Flows

| Élément | Rôle |
|---|---|
| Sequence Flow | Définit l'ordre d'exécution des étapes |
| Message Flow | Représente un échange de messages entre participants distincts |
| Association | Relie une annotation ou un artefact à un élément du processus |

Le `Sequence Flow` représente le déroulement du processus. Le `Message Flow` représente un échange entre participants distincts, par exemple entre deux pools.

### 3.5. Pools et lanes

- **Pool :** représente un participant au processus, par exemple la banque ou un partenaire externe.
- **Lane :** subdivise un participant pour distinguer les responsabilités, par exemple le conseiller, le système de scoring et le service de financement.

Les pools et les lanes permettent de clarifier qui réalise chaque action.

## 4. Exemple de processus métier

Prenons le cas d'une demande de financement immobilier.

Le processus peut suivre les étapes suivantes :

1. Recevoir la demande du client.
2. Vérifier les informations obligatoires.
3. Calculer la capacité de financement.
4. Décider si le dossier est éligible.
5. Si le dossier est éligible, poursuivre son instruction.
6. Sinon, enregistrer le refus et notifier le client.
7. Terminer le processus.

Représentation simplifiée :

```text
(Start)
   |
   v
[Recevoir la demande]
   |
   v
[Vérifier les données]
   |
   v
[Calculer la capacité]
   |
   v
< Dossier éligible ? >
      /          \
    Oui           Non
     |             |
     v             v
[Instruire]    [Enregistrer le refus]
     |             |
     v             v
   (Fin)         [Notifier]
                    |
                    v
                  (Fin)
```

Ce schéma est une représentation textuelle simplifiée. Un véritable diagramme BPMN utilise les symboles graphiques normalisés.

## 5. BPMN et moteur de workflow

BPMN décrit le processus ; le moteur de workflow pilote son exécution lorsque le modèle est compatible avec celui-ci.

Exemple avec Camunda :

1. Le développeur modélise le processus dans un fichier `.bpmn`.
2. Le fichier est déployé sur le moteur Camunda.
3. L'application démarre une instance du processus.
4. Le moteur exécute les étapes automatiques et gère les transitions.
5. Les tâches humaines peuvent être prises en charge par une application dédiée.
6. L'application et le moteur suivent l'état de l'instance.

Le moteur peut conserver l'état du workflow entre plusieurs étapes, y compris lorsqu'une intervention humaine ou une attente externe est nécessaire.

## 6. BPMN et UML Activity Diagram

Le diagramme d'activité UML est l'un des diagrammes UML les plus proches de BPMN.

| Critère | BPMN | UML Activity Diagram |
|---|---|---|
| Objectif principal | Modéliser les processus métier | Modéliser les activités et les flux de contrôle |
| Public cible | Métier, analystes, développeurs | Développeurs, concepteurs et architectes |
| Échanges de messages | Notation dédiée | Représentation possible avec d'autres éléments UML |
| Tâches humaines et automatiques | Notation orientée processus | Modélisation possible des actions |
| Exécution par un moteur | Possible avec un moteur compatible | Ne constitue pas, à lui seul, un format d'exécution standard équivalent |
| Cas d'usage typique | Workflow métier automatisé | Comportement d'un système ou d'un composant |

**En résumé :** UML Activity Diagram convient pour modéliser un comportement ou un algorithme. BPMN est généralement plus adapté lorsqu'il faut représenter un processus métier avec ses acteurs, ses échanges, ses événements et son automatisation.

## 7. Bonnes pratiques

- Utiliser des noms d'activités explicites, par exemple `Vérifier les données du client`.
- Garder les diagrammes lisibles et éviter les processus trop complexes sur une seule page.
- Nommer clairement les conditions des passerelles, par exemple `Éligible` et `Non éligible`.
- Séparer les responsabilités avec des pools et des lanes lorsque cela améliore la compréhension.
- Utiliser des sous-processus pour isoler les parties complexes.
- Distinguer la logique d'orchestration de la logique métier.
- Éviter de placer des règles métier complexes directement dans le diagramme lorsqu'un mécanisme dédié, comme DMN, est plus approprié.
- Vérifier la compatibilité du modèle avec le moteur BPMN utilisé avant de le déployer.

## 8. BPMN, DMN et Camunda

Ces trois notions sont complémentaires :

- **BPMN :** décrit les étapes et l'enchaînement du processus.
- **DMN :** décrit les règles et les décisions métier.
- **Camunda :** fournit un moteur permettant d'exécuter des processus BPMN et, selon la version et la configuration utilisées, d'intégrer des décisions DMN.

Exemple :

Dans un processus de financement, BPMN orchestre la réception du dossier, le calcul de capacité et la notification. DMN peut déterminer l'éligibilité du client à partir de critères métier. Camunda pilote l'enchaînement des étapes.

## 9. Résumé

BPMN est une notation standard pour représenter les processus métier. Elle aide à comprendre le fonctionnement d'un workflow, à répartir les responsabilités et à préparer l'automatisation d'un processus.

Elle s'intègre naturellement dans une architecture backend utilisant un moteur de workflow tel que Camunda, avec DMN pour externaliser les décisions métier lorsque cela est pertinent.
