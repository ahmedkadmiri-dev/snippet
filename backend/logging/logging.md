# Logging

## 1. Présentation

L'application utilise **SLF4J** comme API de logging et **Logback** comme implémentation par défaut de Spring Boot.

L'utilisation de **Lombok `@Slf4j`** permet d'injecter automatiquement un logger dans les classes applicatives.

Aucune classe `@Configuration` dédiée n'est nécessaire pour l'utilisation standard du logging.

---

## 2. Utilisation

Ajouter l'annotation `@Slf4j` sur les classes nécessitant des logs.

### Exemple

```java
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SimulationServiceImpl {

    public void save(Long simulationId) {
        log.info("Sauvegarde de la simulation id={}", simulationId);

        try {
            // Traitement
        } catch (Exception exception) {
            log.error(
                "Erreur lors de la sauvegarde de la simulation id={}",
                simulationId,
                exception
            );
            throw exception;
        }
    }
}
```

---

## 3. Niveaux de logs

| Niveau  | Utilisation                                          |
| ------- | ---------------------------------------------------- |
| `ERROR` | Erreur nécessitant une intervention ou un diagnostic |
| `WARN`  | Situation anormale mais non bloquante                |
| `INFO`  | Événement important du traitement applicatif         |
| `DEBUG` | Informations utiles au diagnostic                    |
| `TRACE` | Informations très détaillées                         |

Exemple :

```java
log.error("Erreur lors du traitement");
log.warn("Simulation non trouvée : id={}", simulationId);
log.info("Simulation créée : id={}", simulationId);
log.debug("Paramètres de simulation : {}", request);
log.trace("Détail du traitement : {}", data);
```

---

## 4. Configuration

La configuration des niveaux de logs peut être réalisée dans `application.properties`.

```properties
# Niveau global
logging.level.root=INFO

# Niveau pour l'application
logging.level.ma.wafaimmobilier=DEBUG
```

Pour une configuration avancée du format des logs ou des appenders, utiliser un fichier :

```text
src/main/resources/logback-spring.xml
```

Exemple minimal :

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>

    <appender name="CONSOLE"
              class="ch.qos.logback.core.ConsoleAppender">

        <encoder>
            <pattern>
                %d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] %logger{36} - %msg%n
            </pattern>
        </encoder>

    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>

</configuration>
```

---

## 5. Bonnes pratiques

### Utiliser les paramètres SLF4J

Privilégier :

```java
log.info("Simulation créée avec id={}", simulationId);
```

Éviter :

```java
log.info("Simulation créée avec id=" + simulationId);
```

### Logger les exceptions avec la stack trace

Privilégier :

```java
log.error("Erreur lors de la sauvegarde de la simulation", exception);
```

plutôt que :

```java
log.error("Erreur : {}", exception.getMessage());
```

La première version conserve la stack trace et facilite le diagnostic.

### Ne pas logger de données sensibles

Ne jamais logger :

* mots de passe ;
* tokens d'authentification ;
* informations bancaires ;
* données personne
