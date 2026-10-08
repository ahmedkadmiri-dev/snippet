# Validation custom

## 1. Présentation

La validation custom permet d'implémenter des **règles métier complexes** qui ne peuvent pas être exprimées uniquement avec les contraintes standards de Jakarta Validation.

Elle est particulièrement adaptée aux règles portant sur **plusieurs propriétés d'un même DTO**.

Exemple :

* `dateNaissance` ou `age` doit être renseigné ;
* si `age` est renseigné, il doit être strictement positif.

---

## 2. Création de l'annotation

L'annotation custom est déclarée au niveau de la classe avec `@Target(ElementType.TYPE)`.

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = SimulationInputValidator.class)
public @interface ValidSimulationInput {

    String message() default "Données de simulation invalides";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
```

### Éléments principaux

| Élément                               | Rôle                                                     |
| ------------------------------------- | -------------------------------------------------------- |
| `@Target(ElementType.TYPE)`           | Permet d'utiliser l'annotation sur une classe            |
| `@Retention(RetentionPolicy.RUNTIME)` | Rend l'annotation disponible à l'exécution               |
| `@Constraint`                         | Déclare l'annotation comme contrainte Jakarta Validation |
| `validatedBy`                         | Définit le validator chargé d'effectuer le contrôle      |
| `message`                             | Message/code retourné en cas d'échec                     |
| `groups`                              | Gestion des groupes de validation                        |
| `payload`                             | Métadonnées associées à la contrainte                    |

---

## 3. Implémentation du Validator

Le validator implémente `ConstraintValidator`.

```java
public class SimulationInputValidator
        implements ConstraintValidator<ValidSimulationInput, SimulationInputDTO> {

    @Override
    public boolean isValid(
            SimulationInputDTO dto,
            ConstraintValidatorContext context) {

        if (dto == null) {
            return true;
        }

        boolean dateNaissanceRenseignee =
                dto.getDateNaissance() != null;

        boolean ageRenseigne =
                dto.getAge() != null;

        // Aucun des deux n'est renseigné
        if (!dateNaissanceRenseignee && !ageRenseigne) {

            addViolation(
                    context,
                    "error.simulation.date-naissance.age.required",
                    "dateNaissance"
            );

            return false;
        }

        // Age renseigné mais invalide
        if (ageRenseigne && dto.getAge() <= 0) {

            addViolation(
                    context,
                    "error.simulation.date-naissance.age.required",
                    "age"
            );

            return false;
        }

        return true;
    }

    private void addViolation(
            ConstraintValidatorContext context,
            String errorCode,
            String field) {

        context.disableDefaultConstraintViolation();

        context.buildConstraintViolationWithTemplate(errorCode)
                .addPropertyNode(field)
                .addConstraintViolation();
    }
}
```

---

## 4. Association au DTO

L'annotation custom est placée directement sur le DTO.

```java
@ValidSimulationInput
public class SimulationInputDTO {

    private LocalDate dateNaissance;

    private Integer age;

    // ...
}
```

La validation est ensuite déclenchée automatiquement lorsque le DTO est utilisé avec `@Valid`.

```java
@PostMapping("/simulations")
public ResponseEntity<SimulationOutputDTO> create(
        @Valid @RequestBody SimulationInputDTO request) {

    return ResponseEntity.ok(
            service.create(request)
    );
}
```

---

## 5. Association de l'erreur à un champ

Une validation custom peut être associée à une propriété spécifique du DTO.

```java
context.buildConstraintViolationWithTemplate(errorCode)
        .addPropertyNode(field)
        .addConstraintViolation();
```

Dans l'exemple précédent, l'erreur peut être associée à :

```text
dateNaissance
```

ou :

```text
age
```

Cela permet au gestionnaire global d'exceptions de retourner une erreur associée au champ concerné.

---

## 6. Utilisation des codes d'erreur

Les violations utilisent les **codes d'erreur métier** de l'API.

```java
addViolation(
        context,
        "error.simulation.date-naissance.age.required",
        "dateNaissance"
);
```

Le code d'erreur peut ensuite être récupéré par le gestionnaire global d'exceptions afin de construire la réponse API standardisée.

Exemple :

```json
{
    "errorCode": "error.simulation.date-naissance.age.required",
    "field": "dateNaissance"
}
```

---

## 7. Gestion du DTO `null`

Le validator retourne `true` lorsque le DTO est `null` :

```java
if (dto == null) {
    return true;
}
```

La présence du DTO doit être contrôlée séparément si elle est obligatoire.

Par exemple :

```java
@NotNull
@Valid
private SimulationInputDTO simulation;
```

La validation custom est donc responsable des **règles métier internes au DTO**, et non de la présence de l'objet lui-même.

---

## 8. Quand utiliser une validation custom ?

Utiliser une validation custom lorsque la règle concerne plusieurs propriétés ou nécessite une logique métier spécifique.

### Exemple adapté

```text
dateNaissance OU age doit être renseigné
```

### Autres exemples

```text
dateDebut < dateFin
```

```text
montantMin <= montantMax
```

```text
Si typeClient = "PRO", alors raisonSociale est obligatoire
```

```text
Si typeOffre = "AJUSTEE", alors duree doit être renseignée
```

---

## 9. Validation standard vs validation custom

| Besoin                                      | Solution                             |
| ------------------------------------------- | ------------------------------------ |
| Champ obligatoire                           | `@NotNull`, `@NotBlank`, `@NotEmpty` |
| Valeur positive                             | `@Positive`                          |
| Valeur maximale                             | `@Max`                               |
| Format spécifique                           | `@Pattern`, `@Email`, etc.           |
| Règle portant sur plusieurs champs          | Validation custom                    |
| Condition métier entre plusieurs propriétés | Validation custom                    |
| Règle nécessitant une logique spécifique    | Validation custom                    |

La validation standard doit être privilégiée lorsque la règle peut être exprimée simplement avec une contrainte Jakarta Validation.

La validation custom intervient lorsque cette approche devient insuffisante.
