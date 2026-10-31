## 5. LogRequestResponseFilter

`LogRequestResponseFilter` permet de tracer automatiquement les requêtes et réponses HTTP de l'application.

Il permet notamment de journaliser :

* la méthode HTTP ;
* l'URL et les paramètres de requête ;
* le payload de la requête ;
* le statut HTTP de la réponse ;
* le payload de la réponse ;
* le temps d'exécution ;
* le masquage des données sensibles ;
* la limitation de la taille des payloads journalisés.

### 5.1. Implémentation

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class LogRequestResponseFilter extends OncePerRequestFilter {

    @Value("${logging.max.payload.length:100000}")
    private int maxPayloadLength;

    private final ObjectMapper objectMapper;

    private final SensitiveDataMasker sensitiveDataMasker;

    @SneakyThrows
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) {

        long startTime = System.currentTimeMillis();

        ContentCachingRequestWrapper wrappedRequest =
                new ContentCachingRequestWrapper(request);

        ContentCachingResponseWrapper wrappedResponse =
                new ContentCachingResponseWrapper(response);

        String url = request.getRequestURI();

        String queryString = request.getQueryString();

        if (queryString != null) {
            url += "?%s".formatted(
                    maskingRequired()
                            ? sensitiveDataMasker.maskQueryString(queryString)
                            : queryString
            );
        }

        logPayload(
                ">>> before request [{} {}]",
                request.getMethod(),
                url
        );

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {

            String requestBody = describeBody(
                    wrappedRequest.getContentAsByteArray(),
                    request.getCharacterEncoding(),
                    wrappedRequest.getContentType()
            );

            if (!requestBody.isEmpty()) {
                requestBody = ", payload %s".formatted(requestBody);
            }

            logPayload(
                    ">>> after request [{} {}{}]",
                    request.getMethod(),
                    url,
                    requestBody
            );

            long duration =
                    System.currentTimeMillis() - startTime;

            String responseBody = describeBody(
                    wrappedResponse.getContentAsByteArray(),
                    response.getCharacterEncoding(),
                    wrappedResponse.getContentType()
            );

            if (!responseBody.isEmpty()) {
                responseBody =
                        ", responseBody %s".formatted(responseBody);
            }

            logPayload(
                    "<<< response [{} {}, HttpStatus {}, executionTime {}ms{}]",
                    request.getMethod(),
                    url,
                    response.getStatus(),
                    duration,
                    responseBody
            );

            wrappedResponse.copyBodyToResponse();
        }
    }
}
```

### 5.2. ContentCachingRequestWrapper

`ContentCachingRequestWrapper` permet de récupérer le contenu de la requête après son passage dans la chaîne de filtres.

De même, `ContentCachingResponseWrapper` permet de récupérer le contenu de la réponse avant de la transmettre au client.

```java
ContentCachingRequestWrapper wrappedRequest =
        new ContentCachingRequestWrapper(request);

ContentCachingResponseWrapper wrappedResponse =
        new ContentCachingResponseWrapper(response);
```

Il est important d'appeler :

```java
wrappedResponse.copyBodyToResponse();
```

afin de recopier le contenu de la réponse vers la réponse HTTP réelle.

### 5.3. Exemple de logs

Pour une requête :

```http
POST /api/simulations
```

le filtre peut produire :

```text
>>> before request [POST /api/simulations]

>>> after request [POST /api/simulations,
payload {"csp":"SALA","montantProjet":500000,"duree":240}]

<<< response [POST /api/simulations,
HttpStatus 200,
executionTime 125ms,
responseBody {"id":12345,"statut":"SAUVEGARDEE"}]
```

### 5.4. Masquage des données sensibles

Les données sensibles ne doivent pas être écrites en clair dans les logs.

Le filtre utilise `SensitiveDataMasker` pour masquer notamment les données présentes dans les paramètres de requête.

```java
String queryString = request.getQueryString();

if (queryString != null) {
    url += "?%s".formatted(
            maskingRequired()
                    ? sensitiveDataMasker.maskQueryString(queryString)
                    : queryString
    );
}
```

Exemple :

```text
Avant :
?token=abc123&email=test@example.com

Après :
?token=***&email=***
```

Le même principe doit être appliqué aux payloads lorsque ceux-ci contiennent des données sensibles.

### 5.5. Limitation de la taille des payloads

Les payloads volumineux ne doivent pas être entièrement écrits dans les logs.

La taille maximale peut être configurée :

```properties
logging.max.payload.length=100000
```

Une valeur par défaut peut être définie directement dans le code :

```java
@Value("${logging.max.payload.length:100000}")
private int maxPayloadLength;
```

Cette limitation permet notamment d'éviter :

* des logs excessivement volumineux ;
* une consommation inutile de stockage ;
* une dégradation des performances ;
* des problèmes lors du traitement de fichiers ou de gros payloads.

### 5.6. Mesure du temps d'exécution

Le filtre mesure automatiquement le temps nécessaire au traitement de la requête :

```java
long startTime = System.currentTimeMillis();

filterChain.doFilter(
        wrappedRequest,
        wrappedResponse
);

long duration =
        System.currentTimeMillis() - startTime;
```

Le temps est ensuite ajouté au log de réponse :

```text
executionTime 125ms
```

Cela permet d'identifier rapidement les endpoints présentant des temps de réponse élevés.

### 5.7. Bonnes pratiques

Le filtre doit respecter les règles suivantes :

* ne jamais journaliser de mot de passe ;
* masquer les tokens et informations d'authentification ;
* éviter les données personnelles lorsque cela n'est pas nécessaire ;
* limiter la taille des payloads ;
* ne pas journaliser systématiquement les fichiers binaires ;
* utiliser des logs structurés et facilement exploitables ;
* éviter de journaliser deux fois la même information ;
* conserver les logs suffisamment longtemps pour le diagnostic, conformément aux règles de sécurité et de rétention.

### 5.8. Configuration

Exemple :

```properties
logging.max.payload.length=100000
```

Le niveau de log peut être configuré avec Spring Boot :

```properties
logging.level.root=INFO
logging.level.ma.wafaimmobilier=DEBUG
```

Le filtre peut ainsi être activé en `INFO` en production et passer en `DEBUG` lorsque des investigations techniques nécessitent davantage de détails.
