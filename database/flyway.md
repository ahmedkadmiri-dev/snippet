# Flyway — Automatisation des migrations de base de données et déploiement

## 1. Objectif

Flyway permet de versionner et d'automatiser les migrations de la base de données lors du déploiement d'une application.

Il permet notamment de :

* versionner les modifications de la base de données dans Git ;
* éviter l'exécution manuelle des scripts SQL ;
* garantir l'ordre d'exécution des migrations ;
* tracer les migrations déjà appliquées ;
* détecter les migrations manquantes ou modifiées grâce à la validation ;
* automatiser les déploiements sur les environnements DEV, UAT et PROD ;
* maintenir la cohérence entre la version de l'application et celle de la base de données.

Flyway est compatible avec plusieurs bases de données, notamment Oracle et PostgreSQL.

**Point important :** Flyway exécute les migrations versionnées. Il ne détecte pas automatiquement toutes les modifications effectuées directement dans la base et ne génère pas, à lui seul, tous les scripts de comparaison de schémas. Ces besoins nécessitent des fonctionnalités ou des outils complémentaires.

---

## 2. Principe de fonctionnement

Les migrations sont définies dans des fichiers SQL versionnés.

Exemple :

```text
V1__create_simulation.sql
V2__create_idempotency.sql
V3__add_simulation_group.sql
V4__add_code_type_montage.sql
```

Flyway identifie chaque migration grâce à son numéro de version.

Lors du démarrage de l'application :

1. Flyway recherche les migrations disponibles.
2. Il consulte l'historique des migrations exécutées.
3. Il vérifie la cohérence des migrations versionnées.
4. Il exécute les nouvelles migrations dans l'ordre.
5. Il enregistre leur résultat dans sa table d'historique.

Une migration déjà exécutée n'est pas réexécutée lors des démarrages suivants.

---

## 3. Structure des fichiers

Les scripts Flyway sont généralement stockés dans le projet Spring Boot :

```text
src/
└── main/
    └── resources/
        └── db/
            └── migration/
                ├── V1__create_simulation.sql
                ├── V2__create_idempotency.sql
                ├── V3__create_idempotency_sequence.sql
                └── V4__add_code_type_montage.sql
```

Le répertoire `db/migration` est le répertoire par défaut de Flyway.

### Convention de nommage

Le format standard des migrations versionnées est :

```text
V<version>__<description>.sql
```

Exemples :

```text
V1__create_simulation.sql
V2__create_idempotency.sql
V3__add_simulation_group.sql
V4__add_code_type_montage.sql
V5__create_provenance_projet.sql
```

Règles à respecter :

* `V` indique une migration versionnée ;
* la version doit être unique ;
* `__` sépare la version de la description ;
* la description utilise généralement des underscores ;
* l'extension est `.sql`.

Les migrations sont exécutées selon leur version, et non selon l'ordre alphabétique des descriptions.

---

## 4. Dépendances Maven

Dans le `pom.xml` d'un projet Spring Boot, ajouter Flyway.

### Dépendance principale

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
```

### Module spécifique à la base de données

Avec les versions récentes de Flyway, un module supplémentaire est nécessaire pour certaines bases de données.

Pour Oracle :

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-oracle</artifactId>
</dependency>
```

Pour PostgreSQL :

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

Ajouter uniquement le module correspondant à la base utilisée par l'application.

Les dépendances doivent utiliser une version compatible avec celle de Spring Boot et de Flyway. Si Spring Boot gère la version via son BOM, éviter de définir une version indépendante sans nécessité.

Le pilote JDBC Oracle ou PostgreSQL doit également être présent dans le projet.

---

## 5. Configuration Spring Boot

Dans `application.yml` :

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    validate-on-migrate: true
```

La configuration de connexion à la base reste celle de Spring Boot :

```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: ${DB_DRIVER}
```

Les variables `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` et `DB_DRIVER` doivent être définies dans l'environnement de déploiement.

Exemple pour Oracle :

```properties
DB_URL=jdbc:oracle:thin:@//oracle-host:1521/SERVICE_NAME
DB_DRIVER=oracle.jdbc.OracleDriver
```

Exemple pour PostgreSQL :

```properties
DB_URL=jdbc:postgresql://postgres-host:5432/site_db
DB_DRIVER=org.postgresql.Driver
```

Les identifiants ne doivent pas être stockés en clair dans Git.

### Activation par environnement

Il est possible de conserver une configuration commune et d'utiliser des variables d'environnement différentes en DEV, UAT et PROD.

La migration doit être exécutée avec un compte disposant des droits SQL nécessaires, idéalement dédié aux migrations.

---

## 6. Création d'une migration SQL

Chaque évolution de la base doit être ajoutée dans un nouveau fichier versionné.

Exemple : création d'une table `SIMULATION_IDEMPOTENCY` dans PostgreSQL.

Fichier :

`V2__create_idempotency.sql`

```sql
CREATE TABLE simulation_idempotency (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    simulation_id BIGINT NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_simulation_idempotency_key
        UNIQUE (idempotency_key)
);
```

Pour Oracle, le script doit être adapté à la syntaxe Oracle et aux conventions de nommage du projet.

Exemple :

```sql
CREATE TABLE SIMULATION_IDEMPOTENCY (
    ID NUMBER(19) GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    SIMULATION_ID NUMBER(19) NOT NULL,
    IDEMPOTENCY_KEY VARCHAR2(100) NOT NULL,
    CREATED_AT TIMESTAMP NOT NULL,
    CONSTRAINT UK_SIMULATION_IDEMPOTENCY_KEY
        UNIQUE (IDEMPOTENCY_KEY)
);
```

Chaque migration doit être testée sur une base correspondant au moteur utilisé en production.

---

## 7. Création d'une séquence Oracle

Pour une séquence Oracle, créer une migration spécifique.

Fichier :

`V3__create_idempotency_sequence.sql`

```sql
CREATE SEQUENCE SIMULATION_IDEMPOTENCY_SEQ
    START WITH 1
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;
```

Cette migration sera exécutée après les migrations de versions précédentes.

Attention : une séquence Oracle est un objet distinct de la table. Sa création et ses modifications doivent également être versionnées.

---

## 8. Modification d'une table existante

Un fichier de migration déjà appliqué en environnement ne doit pas être modifié.

Si une nouvelle modification est nécessaire, créer une nouvelle migration.

Exemple : ajout de la colonne `CODE_TYPE_MONTAGE` à la table Oracle `PROJET`.

Fichier :

`V4__add_code_type_montage.sql`

```sql
ALTER TABLE PROJET
ADD CODE_TYPE_MONTAGE VARCHAR2(20);
```

Pour PostgreSQL, le type serait par exemple :

```sql
ALTER TABLE projet
ADD COLUMN code_type_montage VARCHAR(20);
```

Après déploiement de la migration `V4`, toute nouvelle modification devra être ajoutée dans un fichier `V5`, `V6`, etc.

Ne pas modifier le fichier `V4` après son exécution, car Flyway vérifie notamment les sommes de contrôle des migrations déjà appliquées.

---

## 9. Traçabilité des migrations

Flyway utilise par défaut la table :

```text
flyway_schema_history
```

Cette table contient notamment :

* la version de la migration ;
* sa description ;
* le nom du fichier SQL ;
* la somme de contrôle ;
* la date d'exécution ;
* le temps d'exécution ;
* le statut de la migration.

Elle permet à Flyway de savoir quelles migrations ont été appliquées et de détecter certaines incohérences.

Exemple de consultation sous Oracle ou PostgreSQL :

```sql
SELECT *
FROM flyway_schema_history
ORDER BY installed_rank;
```

Le nom de cette table peut être modifié dans la configuration.

---

## 10. Fonctionnement au démarrage

```text
Déploiement de l'application
          |
          v
Démarrage Spring Boot
          |
          v
Initialisation de Flyway
          |
          v
Lecture des fichiers db/migration
          |
          v
Lecture de flyway_schema_history
          |
          v
Validation des migrations
          |
          v
Recherche des migrations non exécutées
          |
          +-----------------------+
          |                       |
          v                       v
Migration déjà appliquée    Nouvelle migration
          |                       |
          v                       v
Pas de réexécution          Exécution du SQL
                                  |
                                  v
                         Mise à jour de l'historique
                                  |
                                  v
                         Initialisation du DataSource
                         et démarrage de l'application
```

En cas d'échec d'une migration, le comportement dépend du moteur de base de données et de la nature du script. Certaines opérations DDL Oracle provoquent notamment un commit implicite. Il faut donc prévoir une stratégie de correction adaptée plutôt que compter systématiquement sur un rollback automatique.

---

## 11. Automatisation dans le pipeline CI/CD

Les scripts Flyway sont versionnés dans le même dépôt que l'application, ou dans un dépôt dédié aux migrations.

Le pipeline peut suivre ce processus :

```text
Commit Git
    |
    v
Build Maven
    |
    v
Tests automatisés
    |
    v
Vérification des migrations
    |
    v
Package de l'application
    |
    v
Déploiement en DEV / UAT / PROD
    |
    v
Exécution de Flyway
    |
    v
Application des nouvelles migrations
    |
    v
Vérification de l'état de la base
    |
    v
Application disponible
```

Deux stratégies sont possibles :

* **Au démarrage Spring Boot :** Flyway exécute les migrations automatiquement lors de l'initialisation de l'application.
* **Étape dédiée du pipeline :** le pipeline exécute Flyway avant le déploiement de la nouvelle version applicative.

Pour la production, une étape dédiée permet de mieux contrôler les droits, l'ordre des opérations, les validations et les éventuelles approbations manuelles.

Il faut éviter que plusieurs instances lancent simultanément des opérations de migration non maîtrisées.

---

## 12. Commandes Flyway

Avec le plugin Maven Flyway configuré dans le projet, les commandes usuelles sont :

```bash
mvn flyway:info
```

Affiche l'état des migrations.

```bash
mvn flyway:validate
```

Vérifie la cohérence des migrations appliquées avec les fichiers disponibles.

```bash
mvn flyway:migrate
```

Applique les migrations en attente.

```bash
mvn flyway:repair
```

Répare certaines métadonnées de l'historique, par exemple après une migration échouée, selon le moteur et la situation.

**Attention :** `repair` ne corrige pas le schéma de la base à lui seul. Il ne doit pas être lancé automatiquement pour masquer une erreur de migration. Il faut d'abord comprendre et corriger l'état réel de la base.

Les commandes Maven nécessitent que le plugin Flyway et la configuration de connexion soient correctement définis.

---

## 13. Détection des changements de schéma (*Schema Drift*)

Le *schema drift* correspond à un écart entre le schéma attendu et le schéma réellement présent dans une base.

Exemple :

* la migration `V4` ajoute `CODE_TYPE_MONTAGE` ;
* un DBA ajoute manuellement une autre colonne en PROD ;
* le dépôt Git ne contient pas cette modification.

Flyway ne garantit pas que toute modification manuelle sera automatiquement détectée par le simple lancement de `migrate`.

Pour contrôler les écarts entre DEV, UAT et PROD, il est possible d'utiliser :

* les outils de comparaison de schémas du moteur de base de données ;
* Flyway Desktop et les fonctionnalités de comparaison compatibles avec l'édition utilisée ;
* un outil de comparaison tiers ;
* des contrôles automatisés intégrés au pipeline CI/CD.

Le résultat de la comparaison peut ensuite servir à créer une migration SQL versionnée et validée avant déploiement.

Il faut distinguer trois opérations :

| Opération | Rôle |
|---|---|
| Migration | Applique un changement versionné |
| Validation Flyway | Vérifie notamment la cohérence de l'historique et des fichiers |
| Comparaison de schémas | Identifie les différences entre deux états réels de bases de données |

---

## 14. Rollback et stratégie de retour arrière

Les migrations SQL versionnées ne doivent pas être considérées comme automatiquement réversibles.

En pratique, il faut prévoir :

* une stratégie de retour arrière documentée ;
* une sauvegarde adaptée aux changements sensibles ;
* des scripts correctifs versionnés ;
* des tests de migration sur un environnement représentatif ;
* une stratégie de compatibilité entre les anciennes et nouvelles versions de l'application.

Certaines éditions de Flyway proposent des fonctionnalités avancées de rollback, selon les licences et les moteurs compatibles. Dans le flux classique, il est courant d'utiliser une nouvelle migration corrective plutôt que de supprimer l'historique d'une migration déjà exécutée.

---

## 15. Convention de nommage

Une convention simple et explicite doit être appliquée.

```text
V1__create_simulation.sql
V2__create_idempotency.sql
V3__create_idempotency_sequence.sql
V4__add_code_type_montage.sql
V5__create_provenance_projet.sql
V6__add_simulation_group.sql
```

Bonnes pratiques :

* ne jamais réutiliser une version ;
* ne jamais modifier une migration déjà appliquée ;
* tester les migrations avant déploiement ;
* utiliser des scripts spécifiques au moteur si Oracle et PostgreSQL sont tous deux pris en charge ;
* vérifier les permissions du compte de migration ;
* conserver les migrations dans Git ;
* surveiller les erreurs Flyway dans les logs du pipeline et de l'application ;
* contrôler les écarts de schéma avec un outil adapté si des modifications manuelles restent possibles.

---

## 16. Résumé

Flyway apporte une gestion versionnée et reproductible des migrations de base de données.

Son intégration avec Spring Boot permet d'automatiser l'exécution des scripts SQL au démarrage ou dans une étape dédiée du pipeline CI/CD.

Pour une utilisation fiable en DEV, UAT et PROD, les migrations doivent être immuables après déploiement, testées avant livraison et accompagnées d'une stratégie de correction et de retour arrière adaptée à la base de données.
