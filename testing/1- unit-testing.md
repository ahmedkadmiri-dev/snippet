# 01 - Unit Testing (Tests unitaires)

## 1. Introduction

Le **Unit Testing (test unitaire)** est une pratique de test logiciel qui consiste à vérifier le comportement d'une unité de code de manière isolée, indépendamment de ses dépendances externes.

Dans une architecture microservices, chaque microservice possède son propre périmètre fonctionnel, ses responsabilités et ses composants. Les tests doivent donc être organisés indépendamment pour chaque microservice.

L'approche consiste à tester séparément les différentes couches de chaque microservice :

1. **REST Layer (Controller)** : vérifier l'exposition des endpoints REST et le comportement HTTP.
2. **Business Layer (Service)** : vérifier les règles métier, les traitements et les orchestrations.
3. **Persistence Layer (Repository)** : vérifier les opérations d'accès et de manipulation des données.

Chaque couche est testée indépendamment en simulant ses dépendances lorsque cela est nécessaire.

Cette approche permet de détecter rapidement les anomalies, de limiter les effets de bord et de faciliter la maintenance des microservices.

**Technologies principales :** JUnit 5, Mockito, AssertJ et Spring Test.

## 2. Concepts fondamentaux

### 2.1. Isolation des tests

L'isolation consiste à tester une couche sans exécuter réellement les couches dont elle dépend.

Par exemple :

- Le Controller est testé sans exécuter le Service réel.
- Le Service est testé sans accéder réellement au Repository ou aux API externes.
- Le Repository est testé avec un contexte de persistence dédié et une base de données de test.

Les dépendances peuvent être simulées avec Mockito ou remplacées par des composants de test adaptés.

### 2.2. Pattern AAA (Arrange – Act – Assert)

Le pattern AAA est une convention permettant de structurer les tests en trois étapes :

- **Arrange** : préparer les données et les dépendances nécessaires.
- **Act** : exécuter la méthode ou l'opération à tester.
- **Assert** : vérifier le résultat obtenu.

```java
@Test
void shouldCalculateTotalPrice() {

    // Arrange
    double price = 100.0;
    int quantity = 2;

    // Act
    double result = price * quantity;

    // Assert
    assertEquals(200.0, result);
}
```

### 2.3. JUnit 5

JUnit 5 est un framework de test Java permettant de structurer, exécuter et vérifier les tests.

| Annotation | Description |
|---|---|
| `@Test` | Déclare une méthode de test. |
| `@BeforeEach` | Exécutée avant chaque test. |
| `@AfterEach` | Exécutée après chaque test. |
| `@BeforeAll` | Exécutée une seule fois avant tous les tests. |
| `@AfterAll` | Exécutée une seule fois après tous les tests. |
| `@ParameterizedTest` | Exécute un test avec plusieurs jeux de données. |
| `@DisplayName` | Définit un nom explicite pour un test. |
| `@Disabled` | Désactive temporairement un test. |

### 2.4. Mockito

Mockito permet de simuler les dépendances d'une classe afin de tester son comportement indépendamment de leur implémentation réelle.

| Concept | Description |
|---|---|
| Mock | Objet simulé dont le comportement peut être configuré et vérifié. |
| Stub | Comportement prédéfini d'une dépendance simulée. |
| Spy | Objet réel dont certaines méthodes peuvent être interceptées ou remplacées. |
| `when()` | Configure le comportement d'un Mock. |
| `thenReturn()` | Définit la valeur retournée par un Mock. |
| `thenThrow()` | Définit une exception à lever. |
| `verify()` | Vérifie qu'une méthode a été appelée. |
| `ArgumentCaptor` | Capture les arguments transmis à une méthode. |
| `@Mock` | Crée un objet simulé. |
| `@InjectMocks` | Injecte les mocks dans la classe testée. |

## 3. REST Layer – Tests des Controllers

### 3.1. Présentation

La REST Layer représente la couche d'exposition des services. Elle est généralement constituée des Controllers qui exposent les endpoints REST et assurent la communication HTTP entre les consommateurs et les Services.

Les tests de cette couche permettent de vérifier que les endpoints fonctionnent conformément au contrat API défini.

Ils ne doivent pas tester la logique métier, qui relève de la Business Layer.

### 3.2. Éléments testés

Les tests des Controllers permettent notamment de vérifier :

- Les mappings HTTP : GET, POST, PUT, PATCH et DELETE.
- Les paramètres de requête : `@PathVariable`, `@RequestParam` et `@RequestBody`.
- Les codes de réponse HTTP : 200, 201, 204, 400, 404, 500, etc.
- La structure et le contenu des réponses JSON.
- La désérialisation et la sérialisation des DTO.
- Les validations des entrées avec `@Valid`, `@NotNull`, `@NotBlank`, etc.
- La gestion des exceptions et la structure des erreurs HTTP.
- La délégation correcte des traitements au Service.

### 3.3. Technologies utilisées

| Technologie | Rôle |
|---|---|
| JUnit 5 | Organisation et exécution des tests. |
| MockMvc | Simulation des requêtes HTTP sans démarrer un serveur réel. |
| Mockito | Simulation des Services appelés par le Controller. |
| `@WebMvcTest` | Chargement ciblé de la couche Web. |
| AssertJ | Vérification expressive des résultats. |
| JsonPath | Vérification du contenu des réponses JSON. |

### 3.4. Exemple de test Controller

Controller :

```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.findById(id);
    }
}
```

Test du Controller :

```java
@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void shouldReturnUser() throws Exception {

        // Arrange
        User user = new User(1L, "Ahmed");

        when(userService.findById(1L))
                .thenReturn(user);

        // Act & Assert
        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Ahmed"));

        verify(userService).findById(1L);
    }
}
```

**Résultat attendu :** vérifier que l'endpoint retourne une réponse HTTP 200 avec les données JSON attendues et qu'il délègue le traitement au Service.

## 4. Business Layer – Tests des Services

### 4.1. Présentation

La Business Layer contient la logique métier du microservice. Elle est généralement constituée des Services qui implémentent les règles de gestion et orchestrent les traitements.

C'est la couche principale pour les tests unitaires, car elle porte les comportements fonctionnels de l'application.

Les tests doivent vérifier la logique métier indépendamment de la base de données, des Controllers et des services externes.

### 4.2. Éléments testés

Les tests des Services permettent notamment de vérifier :

- Les règles de gestion métier.
- Les calculs et les traitements fonctionnels.
- Les conditions et les décisions métier.
- Les validations métier.
- Les transformations de données entre DTO et entités.
- L'orchestration des appels aux repositories et aux clients externes.
- La gestion des exceptions métier.
- Les différents scénarios fonctionnels : succès, absence de données, données invalides et erreurs externes.

### 4.3. Technologies utilisées

| Technologie | Rôle |
|---|---|
| JUnit 5 | Organisation et exécution des tests. |
| Mockito | Simulation des repositories et clients externes. |
| AssertJ | Vérification expressive des résultats. |
| `@ExtendWith(MockitoExtension.class)` | Initialisation de Mockito sans contexte Spring. |
| `@Mock` | Simulation des dépendances. |
| `@InjectMocks` | Injection des mocks dans le Service testé. |

### 4.4. Exemple de test Service

Service :

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() ->
                        new UserNotFoundException("Utilisateur introuvable"));
    }
}
```

Test du Service :

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void shouldReturnUserWhenFound() {

        // Arrange
        User user = new User(1L, "Ahmed");

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        // Act
        User result = userService.findById(1L);

        // Assert
        assertThat(result)
                .isNotNull()
                .extracting(User::getName)
                .isEqualTo("Ahmed");

        verify(userRepository).findById(1L);
    }

    @Test
    void shouldThrowExceptionWhenUserNotFound() {

        // Arrange
        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.findById(1L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("Utilisateur introuvable");
    }
}
```

**Résultat attendu :** vérifier que le Service applique correctement les règles métier, retourne le résultat attendu et gère les exceptions indépendamment de la base de données.

## 5. Persistence Layer – Tests des Repositories

### 5.1. Présentation

La Persistence Layer représente la couche responsable de l'accès aux données et de leur manipulation.

Dans Spring Boot, elle est généralement constituée des interfaces Repository basées sur Spring Data JPA.

Contrairement aux tests des Controllers et des Services, les tests des Repositories nécessitent généralement un environnement de persistence réel ou simulé, car ils doivent vérifier le comportement des requêtes et des opérations SQL/JPA.

Ces tests sont donc principalement des **tests d'intégration ciblés**, et non des tests unitaires purs.

### 5.2. Éléments testés

Les tests des Repositories permettent notamment de vérifier :

- Les opérations CRUD : création, lecture, modification et suppression.
- Les méthodes générées automatiquement par Spring Data JPA.
- Les requêtes personnalisées avec `@Query`.
- Les requêtes JPQL et natives SQL.
- Les critères de recherche et les filtres.
- Les relations entre entités : `@OneToMany`, `@ManyToOne`, `@OneToOne`, etc.
- Les contraintes de persistence et les mappings JPA.
- La pagination et le tri.
- Le comportement des transactions et du contexte de persistence lorsque cela est pertinent.

### 5.3. Technologies utilisées

| Technologie | Rôle |
|---|---|
| JUnit 5 | Organisation et exécution des tests. |
| Spring Data JPA | Couche d'accès aux données. |
| `@DataJpaTest` | Chargement ciblé de la couche JPA. |
| TestEntityManager | Manipulation des entités dans les tests. |
| AssertJ | Vérification des résultats. |
| H2 | Base de données embarquée pour les tests. |
| Testcontainers | Exécution de tests avec une véritable base de données dans un conteneur. |

### 5.4. Exemple de test Repository

Repository :

```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
}
```

Test du Repository :

```java
@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldFindUserByEmail() {

        // Arrange
        User user = new User(null, "Ahmed", "ahmed@test.com");

        userRepository.saveAndFlush(user);

        // Act
        Optional<User> result =
                userRepository.findByEmail("ahmed@test.com");

        // Assert
        assertThat(result)
                .isPresent()
                .get()
                .extracting(User::getName)
                .isEqualTo("Ahmed");
    }
}
```

**Résultat attendu :** vérifier que Spring Data JPA exécute correctement la recherche et retourne l'entité correspondant au critère demandé.

**Remarque :** `@DataJpaTest` utilise généralement une base embarquée si elle est disponible sur le classpath. Pour les projets utilisant des fonctionnalités spécifiques à Oracle ou PostgreSQL, privilégier Testcontainers avec le même moteur de base de données que celui utilisé en production.

## 6. Synthèse des tests par couche

Chaque couche possède son propre périmètre de test et ses propres outils.

| Couche | Éléments testés | Outils principaux | Type de test |
|---|---|---|---|
| REST (Controller) | Endpoints, HTTP, DTO, JSON, validations | JUnit 5, MockMvc, Mockito | Test de couche Web |
| Business (Service) | Règles métier, calculs, exceptions, orchestration | JUnit 5, Mockito, AssertJ | Test unitaire |
| Persistence (Repository) | CRUD, requêtes SQL/JPQL, mappings JPA | JUnit 5, DataJpaTest, Testcontainers | Test d'intégration ciblé |

### 6.1. Principe d'indépendance entre les couches

L'objectif est de garantir que chaque couche puisse être testée sans dépendre de l'exécution réelle des autres couches.

Exemple d'organisation :

```text
Microservice User
│
├── controller/
│   ├── UserController.java
│   └── UserControllerTest.java
│
├── service/
│   ├── UserService.java
│   └── UserServiceTest.java
│
├── repository/
│   ├── UserRepository.java
│   └── UserRepositoryTest.java
│
└── entity/
    └── User.java
```

Cette organisation permet de :

- Détecter rapidement les anomalies dans la couche concernée.
- Limiter les dépendances entre les tests.
- Faciliter la maintenance et les évolutions fonctionnelles.
- Exécuter les tests indépendamment pour chaque microservice.
- Identifier plus facilement l'origine d'une régression.

## 7. Bonnes pratiques

- Organiser les tests par microservice et par couche.
- Respecter le pattern Arrange – Act – Assert.
- Tester une responsabilité précise par test.
- Utiliser des noms explicites décrivant le comportement attendu.
- Couvrir les cas nominaux, les cas limites et les cas d'erreur.
- Isoler les dépendances avec Mockito lorsque cela est pertinent.
- Ne pas tester la logique métier dans les Controllers.
- Ne pas simuler le Repository lorsqu'on souhaite vérifier réellement une requête JPA.
- Privilégier Testcontainers lorsque la compatibilité avec le moteur de base de données est importante.
- Éviter `@SpringBootTest` pour les tests unitaires simples.
- Éviter les dépendances entre les tests.
- Intégrer l'exécution des tests dans le pipeline CI/CD.
- Mesurer la couverture de code avec JaCoCo, sans considérer le taux de couverture comme le seul indicateur de qualité.

## 8. À retenir

Dans une architecture microservices Spring Boot, les tests doivent être organisés indépendamment pour chaque microservice et chaque couche.

- **REST Layer** : tester le contrat HTTP, les endpoints, les validations et les réponses JSON.
- **Business Layer** : tester les règles métier et les traitements fonctionnels.
- **Persistence Layer** : tester les requêtes, les opérations CRUD et les mappings JPA.

Il est important de distinguer les tests unitaires purs des tests d'intégration ciblés :