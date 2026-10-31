# 01 - Unit Testing (Tests unitaires)

## 1. Définition

Le test unitaire est une pratique de test logiciel qui consiste à vérifier le comportement d'une unité de code de manière isolée.

Une unité peut être une méthode, une classe ou un composant ayant une responsabilité précise.

L'objectif est de vérifier que chaque unité fonctionne conformément au comportement attendu, indépendamment de ses dépendances externes.

Dans l'écosystème Java Spring Boot, les tests unitaires reposent principalement sur **JUnit 5** et **Mockito**.

## 2. Concepts fondamentaux

### 2.1. Isolation

Un test unitaire vérifie une unité sans exécuter réellement ses dépendances.

Les dépendances telles que les repositories, les clients REST ou les autres services sont simulées avec des objets Mock.

Cela permet de contrôler les réponses des dépendances et de tester uniquement la logique de la classe ciblée.

### 2.2. Pattern AAA (Arrange – Act – Assert)

Le pattern AAA est une convention de structuration des tests en trois étapes :

- **Arrange** : préparer les données et les dépendances nécessaires.
- **Act** : exécuter la méthode à tester.
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

Mockito est un framework Java permettant de simuler les dépendances d'une classe afin de tester son comportement indépendamment de leur implémentation réelle.

Concepts principaux :

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

## 3. Tests unitaires des Services

Les Services contiennent généralement la logique métier de l'application : calculs, règles de gestion, validations métier et orchestration des appels aux dépendances.

Les tests unitaires des Services utilisent **JUnit 5 et Mockito**.

Les dépendances du Service, comme les repositories ou les clients REST, sont simulées afin de tester uniquement la logique métier.

### Exemple : test d'un Service

Classe métier :

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

    public User save(User user) {
        return userRepository.save(user);
    }
}
```

Test unitaire :

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
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Ahmed", result.getName());

        verify(userRepository).findById(1L);
    }

    @Test
    void shouldThrowExceptionWhenUserNotFound() {

        // Arrange
        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class,
                () -> userService.findById(1L));

        verify(userRepository).findById(1L);
    }
}
```

## 4. Tests des ressources REST (Controllers)

Les Controllers exposent les endpoints REST et assurent la communication HTTP entre les consommateurs et les Services.

Pour tester cette couche, on utilise généralement **JUnit 5 + MockMvc + Mockito**.

- **MockMvc** : simule les requêtes HTTP sans démarrer un véritable serveur web.
- **Mockito** : simule le Service appelé par le Controller.
- **@WebMvcTest** : charge uniquement la couche Web ciblée.

Ces tests vérifient notamment les mappings HTTP, les codes de réponse, les DTO, la sérialisation JSON et les validations.

### Exemple : test d'un Controller REST

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

## 5. Différence entre les tests Service et Controller

| Critère | Test Service | Test Controller |
|---|---|---|
| Framework | JUnit 5 | JUnit 5 |
| Simulation des dépendances | Mockito | Mockito |
| Simulation HTTP | Non | MockMvc |
| Chargement du contexte Spring | Non | Contexte Web ciblé |
| Vérification de la logique métier | Oui | Non, elle est déléguée au Service |
| Vérification des codes HTTP | Non | Oui |
| Vérification du JSON | Non | Oui |
| Type de test | Test unitaire | Test de couche Web |

**Remarque :** un test `@WebMvcTest` est un test ciblé de la couche Web, souvent appelé *Web Layer Test*. Il ne constitue pas un test unitaire pur au même sens qu'un test de Service isolé.

## 6. Bonnes pratiques

- Respecter le pattern Arrange – Act – Assert.
- Tester une responsabilité précise par test.
- Utiliser des noms explicites décrivant le comportement attendu.
- Couvrir les cas nominaux, les cas limites et les cas d'erreur.
- Isoler les dépendances externes avec Mockito.
- Éviter les dépendances entre les tests.
- Vérifier les interactions uniquement lorsqu'elles sont pertinentes.
- Privilégier les tests de comportement plutôt que les tests d'implémentation.
- Éviter `@SpringBootTest` pour les tests unitaires simples.
- Ne pas utiliser MockMvc pour tester directement la logique métier d'un Service.

## 7. À retenir

Les tests unitaires permettent de vérifier individuellement les composants métier et de détecter rapidement les anomalies.

Dans une application Spring Boot :

- **Services** : JUnit 5 + Mockito.
- **Controllers REST** : JUnit 5 + MockMvc + Mockito.
- **Repositories** : `@DataJpaTest` pour les tests d'intégration avec la couche persistence.

**Technologies principales :** JUnit 5, Mockito, AssertJ et Spring Test.