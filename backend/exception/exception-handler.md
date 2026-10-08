# Exception Handling

## 1. Présentation

L'API utilise un **gestionnaire global d'exceptions** basé sur `@RestControllerAdvice` afin de centraliser la gestion des erreurs et de retourner une réponse d'erreur standardisée.

La gestion couvre notamment :

* les exceptions métier de type `ApiException` ;
* les erreurs de validation Jakarta Validation ;
* la résolution des messages à partir des codes d'erreur ;
* la standardisation de la réponse HTTP.

---

## 2. Exception métier

Les erreurs métier sont représentées par `ApiException`.

```java
@Getter
@EqualsAndHashCode(callSuper = true)
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    private final String errorCode;

    private final Object[] errorArgs;

    private final Map<String, Object> details;

    public ApiException(
            HttpStatus status,
            String errorCode,
            Object[] errorArgs) {

        this.status = status;
        this.errorCode = errorCode;
        this.errorArgs = errorArgs;
        this.details = new HashMap<>();
    }
}
```

### Propriétés

| Propriété   | Description                                       |
| ----------- | ------------------------------------------------- |
| `status`    | Statut HTTP retourné par l'API                    |
| `errorCode` | Code d'erreur métier                              |
| `errorArgs` | Arguments utilisés pour construire le message     |
| `details`   | Informations complémentaires associées à l'erreur |

---

## 3. Utilisation de `ApiException`

Une `ApiException` est levée lorsqu'une règle métier n'est pas respectée.

```java
if (request.getDuree() <= 0) {
    throw new ApiException(
            HttpStatus.BAD_REQUEST,
            "error.simulation.duree.invalid",
            null
    );
}
```

Il est recommandé d'utiliser un **code d'erreur stable** plutôt qu'un message directement dans le code Java.

---

## 4. Gestionnaire global

Le gestionnaire global est implémenté avec `@RestControllerAdvice`.

```java
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private final MessageSource messageSource;

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(
            ApiException exception) {

        return ResponseEntity
                .status(exception.getStatus())
                .body(
                        ErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .errorCode(exception.getErrorCode())
                                .detail(
                                        messageSource.getMessage(
                                                exception.getErrorCode(),
                                                exception.getErrorArgs(),
                                                exception.getErrorCode(),
                                                LocaleContextHolder.getLocale()
                                        )
                                )
                                .build()
                );
    }
}
```

Le handler :

1. récupère le `status` de l'exception ;
2. récupère le `errorCode` ;
3. résout le message correspondant via `MessageSource` ;
4. construit la réponse standardisée ;
5. retourne le statut HTTP approprié.

---

## 5. Gestion des erreurs de validation

Les erreurs générées par `@Valid` sont traitées via `handleMethodArgumentNotValid`.

```java
@Override
protected ResponseEntity<Object> handleMethodArgumentNotValid(
        MethodArgumentNotValidException exception,
        HttpHeaders headers,
        HttpStatusCode status,
        WebRequest request) {

    FieldError fieldError = exception.getBindingResult()
            .getFieldErrors()
            .stream()
            .findFirst()
            .orElse(null);

    String errorCode = fieldError != null
            ? fieldError.getDefaultMessage()
            : "error.validation";

    String detail = messageSource.getMessage(
            errorCode,
            null,
            errorCode,
            LocaleContextHolder.getLocale()
    );

    ErrorResponse response = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .errorCode(errorCode)
            .detail(detail)
            .build();

    return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(response);
}
```

Le `defaultMessage` de la contrainte est utilisé comme **code d'erreur**.

Exemple :

```java
@NotBlank(message = "error.simulation.csp.required")
private String csp;
```

Le code :

```text
error.simulation.csp.required
```

est récupéré par le handler puis utilisé pour rechercher le message correspondant.

---

## 6. Gestion des messages

Les messages sont externalisés dans les fichiers de messages Spring.

### `messages.properties`

```properties
error.simulation.csp.required=Le CSP est obligatoire.
error.simulation.duree.invalid=La durée de la simulation est invalide.
error.simulation.montant.projet.invalid=Le montant du projet est invalide.
error.validation=Les données fournies sont invalides.
```

### Configuration

```properties
spring.messages.basename=messages
spring.messages.encoding=UTF-8
```

Le message est résolu avec le `Locale` courant :

```java
messageSource.getMessage(
        errorCode,
        errorArgs,
        errorCode,
        LocaleContextHolder.getLocale()
);
```

Le troisième paramètre permet de retourner le `errorCode` si aucun message correspondant n'est trouvé.

---

## 7. Messages avec paramètres

`errorArgs` permet de transmettre des paramètres au message.

### Exception

```java
throw new ApiException(
        HttpStatus.BAD_REQUEST,
        "error.simulation.duree.max",
        new Object[]{300}
);
```

### `messages.properties`

```properties
error.simulation.duree.max=La durée ne peut pas dépasser {0} mois.
```

Le message retourné sera :

```text
La durée ne peut pas dépasser 300 mois.
```

---

## 8. Réponse d'erreur standard

Toutes les erreurs sont retournées avec le même modèle `ErrorResponse`.

```java
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private LocalDateTime timestamp;

    private String errorCode;

    private String detail;
}
```

Exemple de réponse :

```json
{
    "timestamp": "2026-10-07T15:42:10",
    "errorCode": "error.simulation.duree.invalid",
    "detail": "La durée de la simulation est invalide."
}
```

---

## 9. Validation standard et validation custom

Les validations Jakarta Validation utilisent également le même mécanisme de gestion des erreurs.

### Validation standard

```java
@NotBlank(message = "error.simulation.csp.required")
private String csp;
```

### Validation custom

```java
context.buildConstraintViolationWithTemplate(
        "error.simulation.date-naissance.age.required"
)
.addPropertyNode("dateNaissance")
.addConstraintViolation();
```

Dans les deux cas, le handler récupère le code d'erreur et utilise `MessageSource` pour obtenir le message final.

Cela permet d'avoir un mécanisme homogène pour les erreurs de validation et les erreurs métier.

---

## 10. Flux de gestion d'une erreur

```text
Request
   │
   ▼
Controller
   │
   ├── @Valid
   │      │
   │      └── Validation invalide
   │
   └── Service
          │
          └── ApiException
                  │
                  ▼
        GlobalExceptionHandler
                  │
                  ▼
            errorCode
                  │
                  ▼
            MessageSource
                  │
                  ▼
            ErrorResponse
                  │
                  ▼
             HTTP Response
```

---

## 11. Bonnes pratiques

* Utiliser des **codes d'erreur stables** plutôt que des messages en dur.
* Centraliser la gestion des exceptions dans `GlobalExceptionHandler`.
* Utiliser `MessageSource` pour externaliser les messages.
* Utiliser `errorArgs` pour les messages paramétrables.
* Retourner un modèle `ErrorResponse` homogène pour les erreurs API.
* Ne pas exposer la stack trace ou les détails techniques internes au consommateur de l'API.
* Utiliser le statut HTTP correspondant au type d'erreur.
* Conserver une séparation entre le **code d'erreur** (`errorCode`) et le **message affiché** (`detail`).

---

## 12. Exemple complet

### Service

```java
if (request.getDuree() <= 0) {
    throw new ApiException(
            HttpStatus.BAD_REQUEST,
            "error.simulation.duree.invalid",
            null
    );
}
```

### `messages.properties`

```properties
error.simulation.duree.invalid=La durée de la simulation est invalide.
```

### Réponse API

```json
{
    "timestamp": "2026-10-07T15:42:10",
    "errorCode": "error.simulation.duree.invalid",
    "detail": "La durée de la simulation est invalide."
}
```
