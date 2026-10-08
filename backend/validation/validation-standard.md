# Validation standard

## 1. Présentation

L'API utilise **Jakarta Bean Validation** pour effectuer les contrôles standards sur les données d'entrée des API REST.

La validation est déclenchée automatiquement à l'aide de l'annotation `@Valid` sur les paramètres des contrôleurs.

---

## 2. Dépendance

Avec Spring Boot, ajouter le starter de validation :

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

---

## 3. Contraintes disponibles

Les principales contraintes utilisées sont :

| Annotation        | Description                                                              |
| ----------------- | ------------------------------------------------------------------------ |
| `@NotNull`        | La valeur ne doit pas être `null`                                        |
| `@NotBlank`       | La chaîne ne doit pas être `null`, vide ou composée uniquement d'espaces |
| `@NotEmpty`       | La valeur ne doit pas être `null` ou vide                                |
| `@Positive`       | La valeur doit être strictement positive                                 |
| `@PositiveOrZero` | La valeur doit être positive ou égale à zéro                             |
| `@Min`            | Valeur minimale                                                          |
| `@Max`            | Valeur maximale                                                          |
| `@Size`           | Taille minimale/maximale                                                 |
| `@Pattern`        | Respect d'une expression régulière                                       |
| `@Email`          | Format d'adresse email valide                                            |

---

## 4. Exemple

```java
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class SimulationInputDTO {

    @NotBlank(message = "error.simulation.csp.required")
    private String csp;

    @NotNull(message = "error.simulation.montant.projet.invalid")
    @Positive(message = "error.simulation.montant.projet.invalid")
    private BigDecimal montantProjet;

    @NotNull(message = "error.simulation.duree.invalid")
    @Positive(message = "error.simulation.duree.invalid")
    @Max(value = 300, message = "error.simulation.duree.invalid")
    private Integer duree;
}
```

---

## 5. Activation de la validation

L'annotation `@Valid` doit être utilisée sur le `@RequestBody`.

```java
@PostMapping("/simulations")
public ResponseEntity<SimulationOutputDTO> create(
        @Valid @RequestBody SimulationInputDTO request) {

    return ResponseEntity.ok(
            service.create(request)
    );
}
```

Lorsque les contraintes du DTO ne sont pas respectées, Spring déclenche automatiquement une exception de validation.

La gestion de cette exception est réalisée par le **gestionnaire global d'exceptions** de l'application.

---

## 6. Code d'erreur

Les messages des contraintes utilisent les **codes d'erreur métier** de l'API :

```java
@NotBlank(message = "error.simulation.csp.required")
```

Le `message` contient ici le **code d'erreur**, et non le message final destiné au consommateur.

Le code peut ensuite être utilisé par le gestionnaire d'exceptions pour construire la réponse API standardisée.

Exemple :

```json
{
    "errorCode": "error.simulation.csp.required"
}
```

---

## 7. Validation de plusieurs contraintes

Plusieurs contraintes peuvent être appliquées au même attribut.

```java
@NotNull(message = "error.simulation.duree.invalid")
@Positive(message = "error.simulation.duree.invalid")
@Max(value = 300, message = "error.simulation.duree.invalid")
private Integer duree;
```

Dans cet exemple :

* `@NotNull` vérifie que la valeur est renseignée ;
* `@Positive` vérifie que la durée est supérieure à `0` ;
* `@Max` vérifie que la durée ne dépasse pas `300`.

---

## 8. Validation imbriquée

Pour valider un objet imbriqué, utiliser `@Valid`.

```java
public class SimulationInputDTO {

    @Valid
    @NotNull
    private ClientDTO client;
}
```

```java
public class ClientDTO {

    @NotBlank(message = "error.client.nom.required")
    private String nom;
}
```

---

## 9. Bonnes pratiques

* Utiliser les contraintes Jakarta Validation pour les contrôles simples et déclaratifs.
* Utiliser des **codes d'erreur stables** plutôt que des messages métier en dur.
* Regrouper la gestion des erreurs de validation dans le handler global.
* Ne pas dupliquer les contrôles standards dans le service.
* Réserver la validation custom aux règles qui ne peuvent pas être exprimées simplement avec les contraintes standards.

---

## 10. Limite de la validation standard

La validation standard est adaptée aux règles portant sur **une valeur ou un ensemble simple de propriétés**.

Pour une règle métier nécessitant :

* plusieurs champs ;
* une condition métier ;
* un appel à un service ou repository ;
* une vérification dépendant du contexte ;

utiliser une **validation custom**.
