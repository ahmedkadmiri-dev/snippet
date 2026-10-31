# RequestContextInterceptor

## 1. Présentation

`RequestContextInterceptor` permet d'initialiser le contexte d'une requête HTTP avant l'exécution du contrôleur.

Il peut notamment être utilisé pour :

* récupérer les informations présentes dans les headers HTTP ;
* initialiser un `correlationId` ou `requestId` ;
* alimenter le contexte de logging avec `MDC` ;
* transmettre certaines informations nécessaires au traitement de la requête ;
* nettoyer le contexte à la fin de la requête.

L'interceptor implémente l'interface Spring `HandlerInterceptor`.

---

## 2. Implémentation

```java
@Component
public class RequestContextInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler)
            throws ServletException {

        // Initialisation du contexte de la requête

        return true;
    }
}
```

La méthode `preHandle()` est exécutée **avant l'appel du contrôleur**.

Retourner `true` permet de poursuivre le traitement de la requête.

Retourner `false` permet d'interrompre le traitement.

---

## 3. Gestion du correlationId

Le `correlationId` permet d'identifier une requête de manière unique et de retrouver l'ensemble des logs associés.

Exemple :

```java
@Component
public class RequestContextInterceptor implements HandlerInterceptor {

    private static final String CORRELATION_ID = "X-Correlation-Id";

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler)
            throws ServletException {

        String correlationId =
                request.getHeader(CORRELATION_ID);

        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put("correlationId", correlationId);

        response.setHeader(
                CORRELATION_ID,
                correlationId
        );

        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception exception) {

        MDC.remove("correlationId");
    }
}
```

---

## 4. Utilisation de MDC

`MDC` permet d'associer automatiquement des informations au contexte de logging de la requête.

```java
MDC.put("correlationId", correlationId);
```

Le `correlationId` peut ensuite être utilisé dans les logs sans devoir le transmettre explicitement à chaque appel de logger.

```java
log.info(
        "Création de la simulation id={}",
        simulationId
);
```

Avec une configuration Logback contenant :

```xml
<pattern>
    %d{yyyy-MM-dd HH:mm:ss.SSS}
    %-5level
    [correlationId=%X{correlationId}]
    [%thread]
    %logger{36} - %msg%n
</pattern>
```

Le log obtenu sera par exemple :

```text
2026-10-07 15:20:31.125 INFO
[correlationId=8f52a6c1-...]
[http-nio-8080-exec-1]
SimulationService - Création de la simulation id=12345
```

---

## 5. Nettoyage du contexte

Le contexte MDC est associé au thread utilisé pour traiter la requête.

Il est donc important de supprimer les informations à la fin du traitement :

```java
@Override
public void afterCompletion(
        HttpServletRequest request,
        HttpServletResponse response,
        Object handler,
        Exception exception) {

    MDC.remove("correlationId");
}
```

Cette étape permet d'éviter qu'une information d'une requête soit réutilisée par une autre requête traitée par le même thread.

---

## 6. Enregistrement de l'interceptor

L'interceptor doit être enregistré dans la configuration Spring MVC :

```java
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final RequestContextInterceptor requestContextInterceptor;

    @Override
    public void addInterceptors(
            InterceptorRegistry registry) {

        registry.addInterceptor(
                requestContextInterceptor
        );
    }
}
```

Il est possible de limiter son application à certains endpoints :

```java
registry.addInterceptor(requestContextInterceptor)
        .addPathPatterns("/api/**");
```

Ou d'exclure certains endpoints :

```java
registry.addInterceptor(requestContextInterceptor)
        .addPathPatterns("/api/**")
        .excludePathPatterns(
                "/actuator/**",
                "/swagger-ui/**",
                "/v3/api-docs/**"
        );
```

---

## 7. Flux de traitement

Le fonctionnement est le suivant :

```text
HTTP Request
     |
     v
RequestContextInterceptor
     |
     |-- récupération correlationId
     |-- création si absent
     |-- MDC.put(...)
     |
     v
Controller
     |
     v
Service
     |
     v
Logs avec correlationId
     |
     v
afterCompletion()
     |
     |-- MDC.remove(...)
     |
     v
HTTP Response
```

---

## 8. Interaction avec LogRequestResponseFilter

Les deux composants ont des responsabilités différentes.

| Composant                   | Responsabilité                                |
| --------------------------- | --------------------------------------------- |
| `RequestContextInterceptor` | Gestion du contexte de la requête             |
| `MDC`                       | Stockage du contexte utilisé par les logs     |
| `LogRequestResponseFilter`  | Journalisation de la requête et de la réponse |
| `SLF4J`                     | API de logging                                |
| `Logback`                   | Implémentation et configuration des logs      |

Le `RequestContextInterceptor` peut donc fournir le `correlationId` tandis que `LogRequestResponseFilter` journalise les informations HTTP.

Exemple :

```text
[correlationId=abc-123]
>>> before request [POST /api/simulations]

[correlationId=abc-123]
SimulationService - Création de la simulation

[correlationId=abc-123]
<<< response [POST /api/simulations, HttpStatus 200, executionTime 125ms]
```

Cela permet de retrouver facilement tous les logs associés à une même requête.

---

## 9. Bonnes pratiques

* Toujours nettoyer le `MDC` dans `afterCompletion()`.
* Ne pas stocker de données sensibles dans le MDC.
* Utiliser un identifiant unique pour chaque requête.
* Retourner le `correlationId` dans la réponse si celui-ci est utilisé pour le support ou le diagnostic.
* Utiliser un nom de header standardisé dans toute l'application.
* Éviter de mettre des informations métier volumineuses dans le MDC.
* Ne pas utiliser le `correlationId` comme mécanisme d'authentification.
* Pour les traitements asynchrones, prévoir explicitement la propagation du contexte MDC.

---

## 10. Différence avec un Filter

Un `HandlerInterceptor` intervient dans le cycle Spring MVC, avant l'exécution du contrôleur.

Un `Filter` intervient plus tôt dans la chaîne HTTP.

```text
HTTP Request
     |
     v
Filter
     |
     v
DispatcherServlet
     |
     v
HandlerInterceptor
     |
     v
Controller
```

Pour la gestion du contexte métier lié aux controllers, `HandlerInterceptor` est adapté.

Pour une gestion HTTP plus globale, notamment la journalisation de toutes les requêtes, un `Filter` est généralement plus approprié.
