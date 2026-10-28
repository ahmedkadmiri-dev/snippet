# Strategy Pattern

## 1. Concept

Le **Strategy Pattern** est un patron de conception comportemental (*Behavioral Design Pattern*) qui permet de définir plusieurs algorithmes ou comportements, de les encapsuler dans des classes distinctes et de sélectionner dynamiquement celui qui convient au contexte.

L'objectif est de **remplacer une logique conditionnelle complexe par des stratégies interchangeables**, tout en séparant le comportement de son utilisation.

Au lieu de regrouper plusieurs variantes dans une même classe avec des `if/else` ou des `switch`, chaque variante est implémentée dans une classe dédiée respectant une interface commune.

### Problématique

Lorsqu'une application doit exécuter un même traitement avec des comportements différents, les conditions peuvent rapidement se multiplier :

```java
if (type.equals("A")) {
    // Traitement A
} else if (type.equals("B")) {
    // Traitement B
} else if (type.equals("C")) {
    // Traitement C
}
```

Cette approche devient difficile à maintenir lorsque de nouveaux comportements apparaissent.

Le Strategy Pattern consiste à déléguer le traitement à une implémentation adaptée, sans que le code appelant ait besoin de connaître ses détails.

## 2. Principe de fonctionnement

Le pattern repose généralement sur trois éléments :

- **Strategy** : interface qui définit le contrat commun des traitements.
- **Concrete Strategy** : implémentation qui réalise un comportement spécifique.
- **Context** : composant qui utilise la stratégie sélectionnée sans dépendre de son implémentation concrète.

```text
             Context
                |
                v
         Strategy (interface)
           /     |     \
          v      v      v
     StrategyA StrategyB StrategyC
```

Le contexte reçoit ou sélectionne une stratégie, puis lui délègue l'exécution du traitement.

## 3. Avantages

- **Extensibilité** : ajouter un nouveau comportement sans modifier les stratégies existantes.
- **Maintenabilité** : isoler chaque variante dans une classe dédiée.
- **Réduction du couplage** : le contexte dépend d'une abstraction plutôt que d'implémentations concrètes.
- **Testabilité** : tester chaque comportement indépendamment.
- **Lisibilité** : éviter les longues chaînes de `if/else` ou de `switch`.

Le Strategy Pattern favorise le principe **Open/Closed** : le code est ouvert à l'extension, mais fermé à la modification.

## 4. Exemple d'implémentation Java

### Cas d'utilisation

Une API omnicanale reçoit des demandes provenant de plusieurs partenaires. Avant de transmettre les données au service métier, elle doit appliquer des prétraitements spécifiques au partenaire appelant.

Le prétraitement peut notamment normaliser des champs, compléter des valeurs autorisées ou adapter certains formats.

### 4.1. Définir le contrat commun

```java
public interface InputPreprocessingStrategy {

    void preprocess(SimulationInput input);
}
```

### 4.2. Implémenter une stratégie par partenaire

```java
@Component("selfcareStrategy")
public class SelfcarePreprocessingStrategy
        implements InputPreprocessingStrategy {

    @Override
    public void preprocess(SimulationInput input) {
        // Normalisation spécifique à Selfcare
        input.setTelephone(normalizePhone(input.getTelephone()));
    }

    private String normalizePhone(String telephone) {
        if (telephone == null || telephone.isBlank()) {
            return telephone;
        }

        return telephone.replaceAll("\\s+", "");
    }
}
```

```java
@Component("grantingStrategy")
public class GrantingPreprocessingStrategy
        implements InputPreprocessingStrategy {

    @Override
    public void preprocess(SimulationInput input) {
        // Prétraitement spécifique à Granting
        input.setCsp(normalizeCsp(input.getCsp()));
    }

    private String normalizeCsp(String csp) {
        return csp == null ? null : csp.trim().toUpperCase();
    }
}
```

Chaque classe encapsule son propre comportement et respecte le même contrat.

### 4.3. Sélectionner et utiliser la stratégie

```java
@Service
@RequiredArgsConstructor
public class SimulationService {

    private final Map<String, InputPreprocessingStrategy> strategies;

    public void process(SimulationInput input, String partner) {
        InputPreprocessingStrategy strategy = strategies.get(
                partner.toLowerCase() + "Strategy"
        );

        if (strategy == null) {
            throw new IllegalArgumentException(
                    "Partenaire non supporté : " + partner
            );
        }

        strategy.preprocess(input);

        // Transmission de l'input prétraité au traitement métier
        executeSimulation(input);
    }

    private void executeSimulation(SimulationInput input) {
        // Traitement métier commun
    }
}
```

*Remarque :* cet exemple illustre le principe. En production, il est préférable d'utiliser un identifiant de stratégie explicite et stable, une map construite à partir d'un identifiant déclaré par chaque stratégie, ainsi qu'un contexte partenaire authentifié plutôt qu'une valeur libre. Le DTO doit également être mutable pour cet exemple ou remplacé par un objet transformé retourné par la stratégie.

## 5. Quand utiliser le Strategy Pattern ?

Ce pattern est pertinent lorsque :

- plusieurs algorithmes permettent d'effectuer une même opération ;
- un comportement dépend du contexte ou d'un critère de sélection ;
- les variantes évoluent indépendamment ;
- les conditions se multiplient et rendent le code difficile à maintenir ;
- il est souhaitable de tester et de faire évoluer chaque comportement séparément.

Il est moins utile lorsqu'il n'existe qu'un seul comportement stable ou lorsque quelques conditions simples suffisent.

## 6. À retenir

Le Strategy Pattern permet de **varier un comportement sans modifier le composant qui l'utilise**.

Son principe fondamental est de programmer contre une abstraction et de déléguer l'exécution à une stratégie concrète sélectionnée selon le contexte.

L'exemple du prétraitement des inputs selon le partenaire constitue une application du pattern, mais le Strategy Pattern s'applique également aux calculs de tarification, aux méthodes de paiement, aux règles de validation, aux algorithmes de tri ou aux modes de livraison.
