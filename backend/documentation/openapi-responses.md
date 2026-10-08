# OpenAPI — Documentation des réponses API

## 1. Objectif

OpenAPI permet de documenter le contrat des API REST directement dans le code Spring Boot.

La documentation des réponses permet de préciser :

* le code HTTP retourné ;
* la description de la réponse ;
* le format du body ;
* le modèle de données retourné ;
* les erreurs fonctionnelles et techniques possibles.

L'objectif est d'obtenir une documentation homogène et maintenable dans Swagger UI.

---

## 2. Dépendance

Pour Spring Boot avec Spring MVC, utiliser `springdoc-openapi`.

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>${springdoc.version}</version>
</dependency>
```

---

## 3. Documentation d'un endpoint

Utiliser :

* `@Operation` pour documenter l'opération ;
* `@ApiResponses` pour définir plusieurs réponses ;
* `@ApiResponse` pour définir une réponse ;
* `@Content` pour documenter le contenu du body ;
* `@Schema` pour définir le modèle de données.

Exemple :

```java
@Operation(
        summary = "Créer une simulation",
        description = "Crée une nouvelle simulation financière"
)
@ApiResponses(value = {
        @ApiResponse(
                responseCode = "201",
                description = "Simulation créée avec succès",
                content = @Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = @Schema(
                                implementation = SimulationResponseDTO.class
                        )
                )
        ),
        @ApiResponse(
                responseCode = "400",
                description = "Données de demande invalides"
        ),
        @ApiResponse(
                responseCode = "500",
                description = "Erreur interne du serveur"
        )
})
@PostMapping
public ResponseEntity<SimulationResponseDTO> create(
        @Valid @RequestBody SimulationInputDTO request) {

    return ResponseEntity.status(HttpStatus.CREATED)
            .body(simulationService.create(request));
}
```

---

# 4. Réponse 200 — OK

Une réponse `200 OK` est utilisée lorsqu'une opération est exécutée avec succès et retourne généralement un body.

```java
@ApiResponse(
        responseCode = "200",
        description = "Simulation récupérée avec succès",
        content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(
                        implementation = SimulationResponseDTO.class
                )
        )
)
```

Exemple de réponse :

```json
{
    "id": 123,
    "statut": "SAUVEGARDEE",
    "duree": 240
}
```

---

# 5. Réponse 201 — Created

Le code `201 Created` est utilisé lorsqu'une nouvelle ressource est créée.

```java
@ApiResponse(
        responseCode = "201",
        description = "Simulation créée avec succès",
        content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(
                        implementation = SimulationResponseDTO.class
                )
        )
)
```

Si la réponse contient uniquement un identifiant :

```java
@ApiResponse(
        responseCode = "201",
        description = "Resource created successfully",
        content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(
                        type = "object",
                        example = "{\"id\": 123}"
                )
        )
)
```

---

# 6. Réponse 204 — No Content

Une réponse `204 No Content` indique que l'opération a réussi mais qu'aucun body n'est retourné.

Il ne faut donc pas définir de `content`.

```java
@ApiResponse(
        responseCode = "204",
        description = "Simulation abandonnée avec succès"
)
```

Exemple :

```java
@Operation(
        summary = "Abandonner une simulation"
)
@ApiResponses(value = {
        @ApiResponse(
                responseCode = "204",
                description = "Simulation abandonnée avec succès"
        ),
        @ApiResponse(
                responseCode = "400",
                description = "Demande invalide"
        ),
        @ApiResponse(
                responseCode = "404",
                description = "Simulation introuvable"
        )
})
@PatchMapping("/{id}/abandon")
public ResponseEntity<Void> abandon(
        @PathVariable Long id) {

    simulationService.abandon(id);

    return ResponseEntity.noContent().build();
}
```

---

# 7. Réponse 400 — Bad Request

Le code `400 Bad Request` est utilisé notamment pour :

* les erreurs de validation ;
* les données de requête invalides ;
* les erreurs métier considérées comme invalides ;
* les paramètres incorrects.

Lorsque l'API utilise un modèle d'erreur standardisé :

```java
@ApiResponse(
        responseCode = "400",
        description = "Données de demande invalides",
        content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(
                        implementation = ApiErrorResponse.class
                )
        )
)
```

Exemple de réponse :

```json
{
    "errorCode": "error.simulation.csp.required",
    "message": "Le CSP est obligatoire"
}
```

---

# 8. Réponse 401 — Unauthorized

Le code `401 Unauthorized` est utilisé lorsqu'une authentification est nécessaire ou invalide.

```java
@ApiResponse(
        responseCode = "401",
        description = "Authentification requise"
)
```

Si l'API retourne un body d'erreur :

```java
@ApiResponse(
        responseCode = "401",
        description = "Authentification requise",
        content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(
                        implementation = ApiErrorResponse.class
                )
        )
)
```

---

# 9. Réponse 403 — Forbidden

Le code `403 Forbidden` indique que l'utilisateur est authentifié mais ne possède pas les droits nécessaires.

```java
@ApiResponse(
        responseCode = "403",
        description = "Accès refusé"
)
```

---

# 10. Réponse 404 — Not Found

Le code `404 Not Found` est utilisé lorsqu'une ressource demandée n'existe pas.

```java
@ApiResponse(
        responseCode = "404",
        description = "Simulation introuvable",
        content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(
                        implementation = ApiErrorResponse.class
                )
        )
)
```

---

# 11. Réponse 409 — Conflict

Le code `409 Conflict` peut être utilisé lorsqu'une requête entre en conflit avec l'état actuel de la ressource.

Exemple :

```java
@ApiResponse(
        responseCode = "409",
        description = "Conflit avec l'état actuel de la ressource",
        content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(
                        implementation = ApiErrorResponse.class
                )
        )
)
```

Exemples d'utilisation :

* conflit d'état ;
* ressource déjà existante ;
* conflit lié à l'idempotence ;
* opération impossible dans l'état actuel de la ressource.

---

# 12. Réponse 500 — Internal Server Error

Le code `500 Internal Server Error` correspond à une erreur technique non prévue.

```java
@ApiResponse(
        responseCode = "500",
        description = "Erreur interne du serveur",
        content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(
                        implementation = ApiErrorResponse.class
                )
        )
)
```

---

# 13. Modèle de réponse d'erreur

Le modèle d'erreur standard doit être documenté avec `@Schema`.

Exemple :

```java
@Schema(description = "Réponse d'erreur de l'API")
public class ApiErrorResponse {

    @Schema(
            description = "Code fonctionnel de l'erreur",
            example = "error.simulation.csp.required"
    )
    private String errorCode;

    @Schema(
            description = "Message associé à l'erreur",
            example = "Le CSP est obligatoire"
    )
    private String message;

    @Schema(
            description = "Détails complémentaires de l'erreur"
    )
    private Map<String, Object> details;
}
```

Le champ `errorCode` permet au consommateur de l'API d'identifier l'erreur sans dépendre du texte du message.

---

# 14. Documentation des validations

Les contraintes Bean Validation peuvent être documentées directement sur les DTO.

Exemple :

```java
public class SimulationInputDTO {

    @NotBlank(message = "error.simulation.csp.required")
    @Schema(
            description = "Code CSP du client",
            example = "SALA",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String csp;
}
```

La validation est exécutée par Bean Validation.

OpenAPI sert ici à documenter le contrat attendu par l'API.

---

# 15. Éviter la répétition des réponses HTTP

Lorsque plusieurs endpoints retournent les mêmes réponses HTTP, il est préférable de ne pas répéter les mêmes `@ApiResponse` dans chaque méthode.

Exemple à éviter :

```java
@ApiResponses(value = {
        @ApiResponse(responseCode = "400", description = "Bad Request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "500", description = "Internal Server Error")
})
```

répété sur chaque endpoint.

Pour éviter cette duplication, utiliser des annotations OpenAPI personnalisées.

---

# 16. Annotation `@CommonApiResponses`

Créer une annotation regroupant les réponses communes.

Exemple :

```java
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ApiResponses(value = {

        @ApiResponse(
                responseCode = "400",
                description = "Bad Request",
                content = @Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = @Schema(
                                implementation = ApiErrorResponse.class
                        )
                )
        ),

        @ApiResponse(
                responseCode = "401",
                description = "Unauthorized",
                content = @Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = @Schema(
                                implementation = ApiErrorResponse.class
                        )
                )
        ),

        @ApiResponse(
                responseCode = "403",
                description = "Forbidden",
                content = @Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = @Schema(
                                implementation = ApiErrorResponse.class
                        )
                )
        ),

        @ApiResponse(
                responseCode = "500",
                description = "Internal Server Error",
                content = @Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = @Schema(
                                implementation = ApiErrorResponse.class
                        )
                )
        )
})
public @interface CommonApiResponses {
}
```

---

# 17. Utilisation de `@CommonApiResponses`

L'annotation peut être utilisée directement sur une méthode.

```java
@Operation(
        summary = "Récupérer une simulation"
)
@CommonApiResponses
@ApiResponse(
        responseCode = "200",
        description = "Simulation récupérée avec succès",
        content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(
                        implementation = SimulationResponseDTO.class
                )
        )
)
@GetMapping("/{id}")
public ResponseEntity<SimulationResponseDTO> getById(
        @PathVariable Long id) {

    return ResponseEntity.ok(
            simulationService.getById(id)
    );
}
```

Les réponses communes sont ainsi séparées de la réponse spécifique de l'endpoint.

---

# 18. Utilisation au niveau du contrôleur

L'annotation peut également être placée sur la classe du contrôleur.

```java
@RestController
@RequestMapping("/simulations")
@CommonApiResponses
public class SimulationController {

    // endpoints
}
```

Les réponses communes sont alors appliquées aux opérations du contrôleur.

Cette approche est particulièrement utile lorsque tous les endpoints du contrôleur partagent les mêmes réponses HTTP.

---

# 19. Réponses communes et réponses spécifiques

Le principe recommandé est :

```text
@CommonApiResponses
        +
@ApiResponse spécifique à l'endpoint
```

Exemple pour une création :

```java
@Operation(
        summary = "Créer une simulation"
)
@CommonApiResponses
@ApiResponse(
        responseCode = "201",
        description = "Simulation créée avec succès",
        content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(
                        implementation = SimulationResponseDTO.class
                )
        )
)
@PostMapping
public ResponseEntity<SimulationResponseDTO> create(
        @Valid @RequestBody SimulationInputDTO request) {

    return ResponseEntity.status(HttpStatus.CREATED)
            .body(simulationService.create(request));
}
```

---

# 20. Annotation dédiée aux réponses de ressources

Lorsque plusieurs endpoints manipulent des ressources pouvant ne pas exister, une annotation dédiée peut être créée.

Exemple :

```java
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ApiResponse(
        responseCode = "404",
        description = "Resource not found",
        content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(
                        implementation = ApiErrorResponse.class
                )
        )
)
public @interface ResourceNotFoundResponse {
}
```

Utilisation :

```java
@CommonApiResponses
@ResourceNotFoundResponse
@GetMapping("/{id}")
public ResponseEntity<SimulationResponseDTO> getById(
        @PathVariable Long id) {
    
    return ResponseEntity.ok(
            simulationService.getById(id)
    );
}
```

---

# 21. Organisation des annotations

Les annotations OpenAPI personnalisées peuvent être regroupées dans un package dédié :

```text
com.company.project
└── api
    └── documentation
        ├── CommonApiResponses.java
        ├── ResourceNotFoundResponse.java
        └── ...
```

Cette organisation permet de séparer :

* la logique métier ;
* les contrôleurs ;
* les DTO ;
* la documentation OpenAPI.

---

# 22. Exemple complet d'un contrôleur

```java
@RestController
@RequestMapping("/simulations")
@CommonApiResponses
@RequiredArgsConstructor
public class SimulationController {

    private final SimulationService simulationService;

    @Operation(
            summary = "Créer une simulation"
    )
    @ApiResponse(
            responseCode = "201",
            description = "Simulation créée avec succès",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(
                            implementation = SimulationResponseDTO.class
                    )
            )
    )
    @PostMapping
    public ResponseEntity<SimulationResponseDTO> create(
            @Valid @RequestBody SimulationInputDTO request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(simulationService.create(request));
    }

    @Operation(
            summary = "Récupérer une simulation"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Simulation récupérée avec succès",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(
                            implementation = SimulationResponseDTO.class
                    )
            )
    )
    @ResourceNotFoundResponse
    @GetMapping("/{id}")
    public ResponseEntity<SimulationResponseDTO> getById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                simulationService.getById(id)
        );
    }

    @Operation(
            summary = "Abandonner une simulation"
    )
    @ApiResponse(
            responseCode = "204",
            description = "Simulation abandonnée avec succès"
    )
    @ResourceNotFoundResponse
    @PatchMapping("/{id}/abandon")
    public ResponseEntity<Void> abandon(
            @PathVariable Long id) {

        simulationService.abandon(id);

        return ResponseEntity.noContent().build();
    }
}
```

---

# 23. Règles de documentation

Chaque endpoint doit documenter au minimum les réponses réellement possibles.

| Code  | Utilisation                                   |
| ----- | --------------------------------------------- |
| `200` | Opération réussie avec body                   |
| `201` | Ressource créée                               |
| `204` | Opération réussie sans body                   |
| `400` | Requête invalide / validation / erreur métier |
| `401` | Authentification requise                      |
| `403` | Accès refusé                                  |
| `404` | Ressource inexistante                         |
| `409` | Conflit                                       |
| `500` | Erreur technique                              |

Pour une réponse contenant un body :

```java
content = @Content(
        mediaType = MediaType.APPLICATION_JSON_VALUE,
        schema = @Schema(
                implementation = XxxResponseDTO.class
        )
)
```

Pour une réponse `204` :

```java
@ApiResponse(
        responseCode = "204",
        description = "Opération effectuée avec succès"
)
```

Ne pas définir de `content` pour une réponse `204`.

---

# 24. Bonnes pratiques

### Documenter uniquement les réponses réellement possibles

Ne pas ajouter systématiquement tous les codes HTTP possibles.

### Centraliser les réponses communes

Utiliser des annotations personnalisées pour éviter les duplications.

### Garder les réponses spécifiques au niveau de l'endpoint

Exemple :

```text
@CommonApiResponses
        +
201 Created
```

### Utiliser un modèle d'erreur standard

Toutes les erreurs API doivent utiliser autant que possible le même modèle :

```text
ApiErrorResponse
```

### Utiliser `errorCode`

Le consommateur doit pouvoir identifier une erreur grâce à `errorCode` sans dépendre du texte du message.

### Ne pas documenter un body pour 204

Une réponse `204 No Content` ne contient aucun body.

### Maintenir la documentation synchronisée avec l'API

La documentation OpenAPI doit refléter le comportement réel de l'API.

---

# 25. Principe général

La documentation OpenAPI doit suivre le principe suivant :

> **Chaque endpoint documente ses réponses spécifiques et réutilise les annotations personnalisées pour les réponses communes.**

Exemple :

```text
                    Controller
                        |
              @CommonApiResponses
                        |
          +-------------+-------------+
          |             |             |
        POST           GET          PATCH
          |             |             |
         201           200           204
          |             |             |
      spécifique     spécifique    spécifique
```

Cette approche permet d'obtenir une documentation OpenAPI :

* homogène ;
* lisible ;
* maintenable ;
* sans duplication ;
* alignée avec le contrat réel de l'API.
