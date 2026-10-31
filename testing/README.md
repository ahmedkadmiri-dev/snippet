# Software Testing

## Définition

Le Software Testing (tests logiciels) regroupe l'ensemble des pratiques permettant de vérifier et de valider le comportement d'une application afin de garantir sa qualité, sa fiabilité, sa sécurité et sa conformité aux exigences fonctionnelles et techniques.

Les tests permettent de détecter les anomalies, prévenir les régressions et réduire les risques avant la mise en production.

## Pyramide des tests

La pyramide des tests est un modèle qui définit la répartition recommandée des différents niveaux de tests selon leur portée, leur coût et leur rapidité d'exécution.

```text
                 /\
                /  \
               / E2E \
              /------\
             /  API   \
            /Integration\
           /------------\
          /  Unit Tests  \
         /________________\
```

- **Tests unitaires** : vérifient le fonctionnement d'une unité de code de manière isolée.
- **Tests d'intégration** : vérifient les interactions entre plusieurs composants ou systèmes.
- **Tests End-to-End** : valident un parcours applicatif complet, de bout en bout.

L'objectif est de privilégier les tests unitaires, rapides et nombreux, de compléter avec des tests d'intégration et de limiter les tests End-to-End aux parcours essentiels.

## Types de tests

| Type | Description |
|---|---|
| Unit Testing | Vérification des composants de manière isolée. |
| Integration Testing | Vérification des interactions entre composants et systèmes. |
| Functional Testing | Validation des fonctionnalités par rapport aux exigences métier. |
| Acceptance Testing | Vérification de la conformité aux critères d'acceptation métier. |
| End-to-End Testing | Validation des parcours complets de l'application. |
| Contract Testing | Vérification des contrats d'échange entre services. |
| Performance Testing | Évaluation des performances, de la charge et de la scalabilité. |
| Security Testing | Identification des vulnérabilités et vérification de la sécurité. |
| Test Automation | Automatisation de l'exécution et de la validation des tests. |

## Documentation

- [Unit Testing](unit-testing.md)
- [Integration Testing](integration-testing.md)
- [Functional Testing](functional-testing.md)
- [Acceptance Testing](acceptance-testing.md)
- [End-to-End Testing](end-to-end-testing.md)
- [Contract Testing](contract-testing.md)
- [Performance Testing](performance-testing.md)
- [Security Testing](security-testing.md)
- [Test Automation](test-automation.md)
