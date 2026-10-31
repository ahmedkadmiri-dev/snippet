# H2 Database

## 1. Définition

**H2** est un système de gestion de base de données relationnelle (SGBDR), développé en Java.

Il est particulièrement utilisé dans les projets Java et Spring Boot pour :
- Les tests unitaires et les tests d'intégration.
- Le prototypage d'applications.
- Le développement local sans installer un serveur de base de données externe.

H2 est léger, rapide et peut fonctionner directement en mémoire (In-Memory Database).

## 2. Caractéristiques principales

### 2.1 Base de données en mémoire (In-Memory)

H2 peut créer une base de données directement en mémoire vive.

Exemple d'URL :

```properties
spring.datasource.url=jdbc:h2:mem:bankdb
```

Caractéristiques :
- Aucune installation de serveur de base de données nécessaire.
- Les données sont stockées en mémoire.
- Les données disparaissent lorsque la base de données est fermée.
- Chaque exécution peut démarrer avec une base de données vierge.

C'est un mode particulièrement adapté aux tests automatisés.

### 2.2 Base de données persistante (File-Based)

H2 peut également stocker les données dans des fichiers sur le disque.

Exemple :

```properties
spring.datasource.url=jdbc:h2:file:./data/bankdb
```

Contrairement au mode mémoire, les données peuvent être conservées entre plusieurs exécutions.

### 2.3 Compatibilité SQL

H2 prend en charge SQL et les principales opérations relationnelles :

- `SELECT`
- `INSERT`
- `UPDATE`
- `DELETE`
- `JOIN`
- Contraintes d'intégrité
- Clés primaires et étrangères
- Index

Il est compatible avec JDBC et s'intègre naturellement avec Spring Data JPA et Hibernate.

Attention : H2 n'est pas une reproduction exacte d'Oracle, PostgreSQL ou MySQL. Certaines syntaxes SQL, fonctions et contraintes peuvent avoir des comportements différents.

## 3. H2 dans l'écosystème Spring Boot

Dans une application Spring Boot, H2 intervient au niveau de la couche de persistance.

L'intégration repose généralement sur :

| Technologie | Responsabilité |
|---|---|
| H2 | Stockage des données |
| JDBC | Communication avec la base de données |
| Hibernate | Implémentation JPA et mapping objet-relationnel |
| Spring Data JPA | Abstraction et génération des opérations Repository |
| Spring Boot | Configuration automatique des composants |

### Architecture simplifiée

```text
REST Controller
      |
      v
   Service
      |
      v
  Repository
      |
      v
 Spring Data JPA
      |
      v
   Hibernate
      |
      v
      JDBC
      |
      v
      H2
```

## 4. Les différents modes de fonctionnement

| Mode | Description | Utilisation |
|---|---|---|
| In-Memory | Base stockée en mémoire | Tests automatisés |
| File-Based | Base stockée sur disque | Développement local |
| Server Mode | Base accessible via un serveur H2 | Accès depuis plusieurs applications |

Le mode In-Memory est généralement privilégié pour les tests d'intégration afin de garantir l'isolation et la reproductibilité des tests.

## 5. H2 Console

H2 fournit une interface web appelée **H2 Console** permettant de consulter et d'interroger la base de données.

Elle permet notamment de :
- Visualiser les tables.
- Exécuter des requêtes SQL.
- Consulter les données.
- Vérifier les relations entre les tables.

Dans Spring Boot, elle peut être activée avec :

```properties
spring.h2.console.enabled=true
```

Par défaut, son URL est :

```text
/h2-console
```

La console est principalement destinée au développement. Elle doit être désactivée ou correctement sécurisée dans les environnements exposés.

## 6. H2 et les tests automatisés

H2 est particulièrement intéressant pour les tests d'intégration avec Spring Boot.

Il permet de tester le fonctionnement réel de plusieurs composants ensemble :

- Les entités JPA.
- Les repositories.
- Les requêtes JPQL et SQL.
- Les relations entre entités.
- Les transactions.
- Les opérations de persistance.

Contrairement à Mockito, qui simule le comportement des dépendances, H2 permet d'effectuer de véritables opérations sur une base de données.

### Différence entre Mockito et H2

| Critère | Mockito | H2 |
|---|---|---|
| Nature | Framework de mocking | Base de données relationnelle |
| Objectif | Simuler des dépendances | Persister et consulter des données |
| Accès SQL réel | Non | Oui |
| Repository réel | Généralement mocké | Oui, avec JPA |
| Utilisation | Tests unitaires | Tests d'intégration |

## 7. H2 et les tests Spring Boot

Spring Boot propose plusieurs annotations pour organiser les tests.

| Annotation | Objectif |
|---|---|
| `@DataJpaTest` | Tester les repositories JPA |
| `@SpringBootTest` | Charger le contexte complet de l'application |
| `@Transactional` | Exécuter les opérations dans une transaction |
| `@AutoConfigureTestDatabase` | Contrôler la configuration de la base de test |

`@DataJpaTest` est particulièrement adaptée aux tests de la couche Repository. Elle configure notamment les composants JPA et utilise généralement une base embarquée disponible sur le classpath.

## 8. Bonnes pratiques

- Privilégier le mode In-Memory pour les tests automatisés.
- Isoler les données de chaque scénario de test.
- Utiliser des données de test maîtrisées.
- Vérifier les contraintes d'intégrité et les relations entre entités.
- Ne pas considérer H2 comme un remplacement systématique de la base de données de production.
- Tester les requêtes natives et les comportements spécifiques au SGBD cible sur ce même SGBD lorsque nécessaire.
- Ne pas exposer H2 Console dans un environnement de production.

## 9. À retenir

H2 est une base de données relationnelle légère, particulièrement adaptée au développement Java et aux tests automatisés.

Associée à Spring Data JPA, elle permet de tester les opérations de persistance sans dépendre d'une base de données externe.

**Dans une stratégie de tests Spring Boot :**

- Mockito permet de tester isolément la logique métier.
- H2 permet de tester la persistance réelle.
- `@DataJpaTest` permet de tester la couche Repository.
- `@SpringBootTest` permet de tester l'intégration de plusieurs couches de l'application.