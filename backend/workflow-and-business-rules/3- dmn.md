# DMN — Decision Model and Notation

## 1. Objectif

**DMN (Decision Model and Notation)** est une notation standardisée permettant de modéliser, documenter et automatiser les décisions métier à l'aide de règles explicites et de tables de décision.

DMN est particulièrement utile lorsque les décisions dépendent de plusieurs critères métier et doivent être maintenues indépendamment du déroulement du processus.

Exemples :
- Déterminer l'éligibilité d'un client à un financement.
- Calculer un niveau de risque selon le profil du client.
- Déterminer les justificatifs nécessaires selon le type de dossier.
- Appliquer des règles de tarification selon le produit et les caractéristiques du client.
- Déterminer si une demande nécessite une validation supplémentaire.

## 2. Pourquoi utiliser DMN ?

DMN permet de séparer les règles métier du code applicatif et de l'orchestration du workflow.

Ses principaux avantages sont :

- **Lisibilité :** les règles sont représentées dans des tables compréhensibles par les équipes métier et techniques.
- **Maintenance :** une règle peut évoluer sans modifier l'ensemble du processus.
- **Testabilité :** les décisions peuvent être testées avec différents jeux de données.
- **Réutilisation :** une même décision peut être appelée depuis plusieurs processus ou services.
- **Traçabilité :** les règles appliquées peuvent être documentées et leurs résultats analysés.
- **Cohérence :** une décision centralisée limite la duplication de règles dans plusieurs microservices.

Attention : DMN ne remplace pas les validations techniques, les contrôles de sécurité ni la logique métier qui nécessite des traitements complexes.

## 3. Les principaux concepts DMN

### 3.1. Decision — Décision

Une décision représente le résultat métier à déterminer à partir de données d'entrée.

Exemples :
- `DeterminerEligibilite`
- `DeterminerNiveauRisque`
- `DeterminerTauxApplicable`

Une décision peut dépendre d'autres décisions.

### 3.2. Input Data — Données d'entrée

Les données d'entrée sont les informations nécessaires à l'évaluation des règles.

Exemple pour une décision de financement :

| Donnée | Type indicatif | Exemple |
|---|---|---|
| Âge du client | Nombre | 35 |
| Revenu mensuel | Nombre | 15 000 |
| Montant demandé | Nombre | 500 000 |
| Durée du financement | Nombre | 20 |
| Type de contrat | Texte | CDI |

Les types et formats doivent être définis explicitement dans le modèle de décision.

### 3.3. Decision Table — Table de décision

Une table de décision définit les conditions et les résultats associés à chaque règle.

Exemple simplifié :

| Revenu mensuel | Contrat | Décision |
|---|---|---|
| Supérieur ou égal à 10 000 | CDI | Étude standard |
| Supérieur ou égal à 10 000 | CDD | Étude complémentaire |
| Inférieur à 10 000 | Tout type | Analyse complémentaire |

Cet exemple est illustratif : les seuils et critères réels doivent être validés par le métier.

### 3.4. Hit Policy — Politique de correspondance

La *Hit Policy* détermine comment les résultats sont produits lorsque plusieurs règles correspondent aux données d'entrée.

| Politique | Signification | Cas d'usage |
|---|---|---|
| UNIQUE | Une seule règle doit correspondre | Catégorisation exclusive |
| FIRST | La première règle correspondante est retenue | Règles ordonnées par priorité |
| ANY | Plusieurs règles peuvent correspondre, mais leurs résultats doivent être identiques | Règles redondantes produisant le même résultat |
| COLLECT | Les résultats de toutes les règles correspondantes sont collectés | Liste de contrôles ou de justificatifs |
| RULE ORDER | Les résultats sont collectés en respectant l'ordre des règles | Résultats ordonnés |
| OUTPUT ORDER | Les résultats sont ordonnés selon les valeurs de sortie | Classement de résultats |

Le choix de la politique doit correspondre au besoin métier. Une mauvaise politique peut produire des résultats incohérents ou masquer des conflits entre règles.

## 4. Exemple métier : éligibilité d'un financement

Supposons que le métier souhaite déterminer si un dossier peut poursuivre le processus d'instruction.

Les données d'entrée sont :
- `age`
- `revenuMensuel`
- `typeContrat`

La décision retourne :
- `ELIGIBLE`
- `ETUDE_COMPLEMENTAIRE`
- `NON_ELIGIBLE`

Exemple de règles purement illustratives :

| Âge | Revenu mensuel | Type de contrat | Résultat |
|---|---|---|---|
| Inférieur à 18 | Tout | Tout | NON_ELIGIBLE |
| Supérieur ou égal à 18 | Supérieur ou égal à 10 000 | CDI | ELIGIBLE |
| Supérieur ou égal à 18 | Supérieur ou égal à 10 000 | CDD | ETUDE_COMPLEMENTAIRE |
| Supérieur ou égal à 18 | Inférieur à 10 000 | Tout | ETUDE_COMPLEMENTAIRE |

Ces règles ne constituent pas une politique de crédit réelle. Les seuils, les priorités et les cas limites doivent être définis et validés par les responsables métier.

## 5. DMN et BPMN : quelle différence ?

BPMN et DMN répondent à deux questions différentes.

| Critère | BPMN | DMN |
|---|---|---|
| Objectif | Décrire l'enchaînement du processus | Décrire les règles de décision |
| Représentation | Événements, tâches, passerelles, flux | Décisions, données, tables de décision |
| Question principale | Quelle est la prochaine étape ? | Quel résultat faut-il retourner ? |
| Exemple | Lancer le calcul de capacité | Déterminer le niveau d'éligibilité |
| Utilisation | Orchestration | Évaluation des règles métier |

Exemple : BPMN orchestre le parcours d'une demande de financement. Lorsqu'il arrive à la tâche de décision, il appelle le modèle DMN qui détermine le résultat. Le processus BPMN utilise ensuite ce résultat pour sélectionner la suite du parcours.

## 6. DMN et microservices

Dans une architecture microservices, plusieurs stratégies sont possibles.

### Option A — Règles intégrées à un microservice

Le microservice possède et exécute ses propres règles.

Avantages :
- Faible dépendance à un moteur externe.
- Déploiement et fonctionnement autonomes.
- Adapté aux règles simples et spécifiques à un service.

Limites :
- Risque de duplication si plusieurs services utilisent les mêmes règles.
- Les évolutions peuvent nécessiter une modification et un redéploiement du service.

### Option B — Moteur de décision dédié

Un moteur DMN est utilisé pour exécuter des modèles de décision indépendamment de la logique métier des microservices.

Avantages :
- Règles centralisées lorsqu'elles sont réellement partagées.
- Modèles plus faciles à examiner et à tester.
- Séparation entre l'orchestration, la décision et les traitements métier.

Limites :
- Dépendance supplémentaire à un composant d'exécution.
- Besoin de gérer la disponibilité, les versions des règles et les temps de réponse.
- Risque de créer un service central trop sollicité ou un point de défaillance.

### Recommandation

Ne centralise pas toutes les règles métier par principe. Garde les règles propres à un domaine dans le microservice responsable. Utilise un moteur DMN lorsque la complexité, la fréquence d'évolution ou les besoins de gouvernance justifient cette séparation.

## 7. Utiliser DMN avec Camunda et Spring Boot

Avec un moteur Camunda compatible avec DMN, une application peut exécuter une décision à partir de données d'entrée et exploiter son résultat dans le processus.

Le déroulement typique est le suivant :

1. Modéliser la décision dans un fichier `.dmn`.
2. Définir les entrées, les règles, les sorties et la politique de correspondance.
3. Déployer le modèle sur le moteur compatible.
4. Fournir les variables nécessaires à l'évaluation.
5. Exécuter la décision.
6. Exploiter le résultat dans l'application ou dans une étape BPMN.
7. Tester les règles avec des cas nominaux, limites et invalides.

La configuration et les API exactes dépendent de la version de Camunda utilisée. Camunda 7 et Camunda 8 ont des architectures et des mécanismes d'intégration différents ; il faut donc préciser la version avant de rédiger la configuration Spring Boot et le code d'exécution.

## 8. Bonnes pratiques

- Donner un nom métier explicite à chaque décision.
- Définir clairement les types des entrées et des sorties.
- Éviter les règles ambiguës et les chevauchements non intentionnels.
- Choisir une `Hit Policy` adaptée au besoin.
- Tester les valeurs limites, les entrées manquantes et les cas contradictoires.
- Versionner les modèles DMN avec le code et les artefacts de déploiement.
- Identifier le responsable métier de chaque règle.
- Conserver une traçabilité suffisante pour comprendre une décision prise.
- Ne pas mettre les accès aux bases de données, les appels réseau ou les traitements complexes dans les règles de décision.
- Ne pas utiliser DMN pour remplacer les contrôles d'autorisation ou les validations de sécurité.
- Définir une stratégie de déploiement et de retour arrière des règles.
- Éviter les règles métier dupliquées dans DMN, Java et les autres microservices.

## 9. Résumé

DMN est une notation permettant de modéliser et d'exécuter des décisions métier de façon structurée.

Dans une architecture backend :
- BPMN orchestre le processus.
- DMN évalue les décisions.
- Les microservices réalisent les traitements métier et techniques.
- Camunda, ou un autre moteur compatible, peut exécuter les modèles correspondants.

Cette séparation améliore la lisibilité et la maintenance, à condition de bien définir les responsabilités de chaque composant.