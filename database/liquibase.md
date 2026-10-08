# Liquibase — Automatisation des scripts de base de données et déploiement

## 1. Objectif

Liquibase permet de versionner les évolutions de la base de données et d'automatiser leur exécution lors du déploiement de l'application.

Les objectifs sont :

* versionner les modifications de la base de données ;
* éviter l'exécution manuelle des scripts SQL ;
* garantir l'ordre d'exécution des évolutions ;
* tracer les modifications appliquées ;
* automatiser les déploiements sur les différents environnements ;
* maintenir la cohérence entre la version de l'application et la version de la base de données.

---

## 2. Structure des fichiers

Les scripts Liquibase sont stockés dans le projet :

```text
src/
└── main/
    └── resources/
        └── db/
            └── changelog/
                ├── db.changelog-master.yaml
                ├── 001-create-simulation.yaml
                ├── 002-create-idempotency.yaml
                └── 003-add-simulation-group.yaml
```

Le fichier `db.changelog-master.yaml` est le point d'entrée principal.

Exemple :

```yaml
databaseChangeLog:

  - include:
      file: db/changelog/001-create-simulation.yaml

  - include:
      file: db/changelog/002-create-idempotency.yaml

  - include:
      file: db/changelog/003-add-simulation-group.yaml
```

---

## 3. Configuration Spring Boot

Dans `application.yml` :

```yaml
spring:
  liquibase:
    enabled: true
    change-log: classpath:db/changelog/db.changelog-master.yaml
```

Liquibase est exécuté automatiquement au démarrage de l'application.

---

## 4. Création d'un changeSet

Chaque évolution de la base doit être définie dans un `changeSet` possédant un identifiant unique.

Exemple :

```yaml
databaseChangeLog:

  - changeSet:
      id: 002-create-idempotency
      author: equipe-dev

      changes:
        - createTable:
            tableName: SIMULATION_IDEMPOTENCY
            columns:
              - column:
                  name: ID
                  type: BIGINT
                  constraints:
                    primaryKey: true
                    nullable: false

              - column:
                  name: SIMULATION_ID
                  type: BIGINT
                  constraints:
                    nullable: false

              - column:
                  name: IDEMPOTENCY_KEY
                  type: VARCHAR(100)
                  constraints:
                    nullable: false

              - column:
                  name: CREATED_AT
                  type: TIMESTAMP
                  constraints:
                    nullable: false

        - addUniqueConstraint:
            tableName: SIMULATION_IDEMPOTENCY
            columnNames: IDEMPOTENCY_KEY
            constraintName: UK_SIMULATION_IDEMPOTENCY_KEY
```

---

## 5. Séquences Oracle

Les séquences doivent également être versionnées avec Liquibase.

Exemple :

```yaml
databaseChangeLog:

  - changeSet:
      id: 003-create-idempotency-sequence
      author: equipe-dev

      changes:
        - sql:
            sql: |
              CREATE SEQUENCE simulation_idempotency_seq
              START WITH 1
              INCREMENT BY 1
              NOCACHE
              NOCYCLE
```

---

## 6. Modification d'une table existante

Un `changeSet` déjà déployé ne doit pas être modifié.

Si une nouvelle modification est nécessaire, créer un nouveau `changeSet`.

Exemple :

```yaml
databaseChangeLog:

  - changeSet:
      id: 004-add-id-groupe
      author: equipe-dev

      changes:
        - addColumn:
            tableName: SIMULATION
            columns:
              - column:
                  name: ID_GROUPE
                  type: BIGINT
```

Il faut éviter de modifier directement le `changeSet` précédent :

```text
001-create-simulation.yaml
```

Une fois déployé, son contenu doit rester stable.

---

## 7. Traçabilité des migrations

Liquibase utilise principalement les tables :

```text
DATABASECHANGELOG
DATABASECHANGELOGLOCK
```

### DATABASECHANGELOG

Cette table contient l'historique des `changeSet` exécutés.

Elle permet notamment de déterminer si une migration doit être exécutée ou non.

### DATABASECHANGELOGLOCK

Cette table permet de gérer le verrouillage lors de l'exécution de Liquibase afin d'éviter que plusieurs instances exécutent simultanément les migrations.

---

## 8. Fonctionnement au démarrage

Le fonctionnement est le suivant :

```text
Déploiement de l'application
          |
          v
Démarrage Spring Boot
          |
          v
Initialisation Liquibase
          |
          v
Lecture du master changelog
          |
          v
Vérification des changeSets
          |
          +----------------------+
          |                      |
          v                      v
    Déjà exécuté            Nouveau
          |                      |
          v                      v
       Ignoré                 Exécuté
                                 |
                                 v
                       DATABASECHANGELOG
                                 |
                                 v
                      Démarrage application
```

---

## 9. Automatisation dans le pipeline CI/CD

Les scripts Liquibase sont versionnés avec le code source.

Le pipeline de déploiement peut donc suivre le processus :

```text
Commit
   |
   v
Build
   |
   v
Tests
   |
   v
Package
   |
   v
Déploiement environnement
   |
   v
Démarrage application
   |
   v
Liquibase
   |
   v
Exécution des nouveaux changeSets
   |
   v
Application disponible
```

Aucune exécution manuelle des scripts de base ne doit être nécessaire dans le processus normal de déploiement.

---

## 10. Convention de nommage

Une convention simple et explicite doit être utilisée.

Exemple :

```text
001-create-simulation
002-create-idempotency
003-create-idempotency-sequence
004-add-id-group
```
