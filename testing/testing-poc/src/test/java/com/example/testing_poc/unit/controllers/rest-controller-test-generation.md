Prompt — Génération de tests REST Controller Spring Boot

Tu es un expert Java, Spring Boot, JUnit 5, Spring MVC MockMvc et Mockito.

Objectif

À partir du code source d’un contrôleur REST Spring Boot fourni en entrée, génère une classe de tests basiques permettant de vérifier exclusivement le comportement du contrôleur et son contrat HTTP.

Entrée

Classe Controller :
{{CONTROLLER_SOURCE}}

Analyse également les DTO, services et éventuels @ControllerAdvice associés lorsqu’ils sont disponibles.

Règles générales

* Utiliser JUnit 5, Mockito et Spring MVC MockMvc.
* Privilégier @WebMvcTest pour tester uniquement la couche MVC.
* Mocker les dépendances du contrôleur.
* Ne jamais appeler une véritable base de données ou un service externe.
* Ne pas tester la logique métier des services.
* Respecter les versions de Java et Spring Boot du projet.
* Utiliser @MockBean ou @MockitoBean selon la version disponible.
* Ne pas inventer de champs, méthodes ou constructeurs absents du projet.
* Générer des tests simples, lisibles, indépendants et maintenables.

Tests à générer automatiquement

Pour chaque endpoint identifié dans le contrôleur :

1. Mapping HTTP

* Vérifier l’URL et la méthode HTTP (GET, POST, PUT, PATCH, DELETE).
* Vérifier le code HTTP attendu.

2. Paramètres d’entrée

Selon les annotations présentes :

* @PathVariable : vérifier la transmission correcte au service.
* @RequestParam : vérifier les paramètres obligatoires, facultatifs et leurs valeurs par défaut.
* @RequestBody : vérifier la désérialisation JSON vers le DTO et la transmission des valeurs.
* @Valid / @Validated : vérifier qu’une entrée invalide génère une réponse HTTP 400 lorsque les contraintes l’exigent.
* @RequestHeader : vérifier les en-têtes requis.

Ne générer que les tests applicables à l’endpoint.

3. Types de retour

Adapter les assertions au type de retour réel :

* ResponseEntity<T> : vérifier le statut HTTP et le contenu.
* DTO ou objet : vérifier les propriétés JSON principales.
* List<T> : vérifier la structure JSON et quelques éléments.
* Page<T> : vérifier la pagination et le contenu.
* String : vérifier le contenu textuel.
* void / ResponseEntity<Void> : vérifier le statut et l’absence de contenu lorsque prévue.

4. Interactions avec le service

* Utiliser Mockito (when, verify, any, eq, argThat) lorsque nécessaire.
* Vérifier que la bonne méthode est appelée avec les bons paramètres.
* Vérifier qu’aucun appel au service n’est effectué en cas de requête rejetée par la validation MVC.

5. Gestion des erreurs

* Tester uniquement les erreurs réellement gérées par le contrôleur ou son @ControllerAdvice.
* Ne pas inventer de comportements métier ni de codes HTTP.

Stratégie de génération

Pour chaque endpoint, générer :

1. Un test nominal (Happy Path).
2. Un test de validation négatif lorsque pertinent.
3. Un test d’erreur uniquement si un comportement d’erreur est explicitement défini.

Éviter les tests redondants ou trop complexes.

Format de sortie

1. Générer une classe Java NomControllerTest.
2. Utiliser la convention shouldExpectedBehavior_whenCondition.
3. Produire du code directement compilable avec les dépendances du projet.
4. Ajouter les imports nécessaires.
5. Fournir une brève description des scénarios couverts.
6. Signaler les informations manquantes empêchant de produire un test fiable.

Important : privilégier des tests minimaux qui vérifient le contrat HTTP du contrôleur, sans tester les détails d’implémentation du service.