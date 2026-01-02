# Task: Comprehensive Testing Improvements

**Priority**: Medium
**Estimated Time**: 3-4 hours
**Agent Type**: general-purpose

## Objective

Improve test coverage, add integration tests, and implement better testing strategies for the translation CLI application.

## Context

Current testing issues:
- Limited unit test coverage
- No integration tests for command execution
- Mock-heavy tests that don't test real behavior
- Tests create actual files without cleanup
- No testing of configuration loading scenarios

## Acceptance Criteria

- [ ] Comprehensive unit test coverage (>80%)
- [ ] Integration tests for CLI commands
- [ ] Test configuration loading from different sources
- [ ] Proper test isolation with temporary directories
- [ ] Performance test for parallel processing
- [ ] Contract tests for external API integration

## Implementation Notes

### Files to Create

- `src/test/java/de/vkoop/integration/CommandIntegrationTest.java`
- `src/test/java/de/vkoop/config/ConfigurationServiceTest.java`
- `src/test/java/de/vkoop/performance/ParallelTranslationTest.java`
- `src/test/java/de/vkoop/testutil/TestDataBuilder.java`
- `src/test/java/de/vkoop/testutil/TemporaryFileManager.java`

### Dependencies to Add

```gradle
// Testing dependencies
testImplementation 'org.springframework.boot:spring-boot-starter-test'
testImplementation 'org.testcontainers:testcontainers:1.19.0'
testImplementation 'org.testcontainers:junit-jupiter:1.19.0'
testImplementation 'com.github.tomakehurst:wiremock-jre8:2.35.0'
testImplementation 'org.awaitility:awaitility:4.2.0'

// Test fixtures
testImplementation 'org.springframework.boot:spring-boot-test-autoconfigure'
```

### Implementation Steps

1. **Create Test Utilities**
   ```java
   @Component
   @TestComponent
   public class TemporaryFileManager {
       private final List<Path> createdFiles = new ArrayList<>();
       private Path tempDir;

       @PostConstruct
       public void initTempDir() throws IOException {
           tempDir = Files.createTempDirectory("transcli-test");
       }

       public File createTempFile(String name, String content) throws IOException {
           Path file = tempDir.resolve(name);
           Files.write(file, content.getBytes());
           createdFiles.add(file);
           return file.toFile();
       }

       public File createTempConfigFile(String authKey, String sourceLanguage, String targetLanguages) throws IOException {
           Properties props = new Properties();
           props.setProperty("authKey", authKey);
           props.setProperty("sourceLanguage", sourceLanguage);
           props.setProperty("targetLanguages", targetLanguages);

           Path configFile = tempDir.resolve("test.properties");
           try (FileOutputStream fos = new FileOutputStream(configFile.toFile())) {
               props.store(fos, "Test configuration");
           }
           createdFiles.add(configFile);
           return configFile.toFile();
       }

       @PreDestroy
       public void cleanup() {
           createdFiles.forEach(path -> {
               try {
                   Files.deleteIfExists(path);
               } catch (IOException e) {
                   // Log but don't fail test cleanup
               }
           });

           if (tempDir != null) {
               try {
                   Files.deleteIfExists(tempDir);
               } catch (IOException e) {
                   // Log but don't fail
               }
           }
       }
   }
   ```

2. **Create Test Data Builder**
   ```java
   public class TestDataBuilder {
       public static String createTestJson() {
           Map<String, Object> testData = Map.of(
               "greeting", "Hello",
               "farewell", "Goodbye",
               "nested", Map.of(
                   "message", "Welcome",
                   "count", 42
               )
           );

           try {
               return new ObjectMapper().writeValueAsString(testData);
           } catch (JsonProcessingException e) {
               throw new RuntimeException(e);
           }
       }

       public static TranslationConfig createTestConfig() {
           TranslationConfig config = new TranslationConfig();
           config.setAuthKey("test-auth-key-123");
           config.setSourceLanguage("EN");
           config.setTargetLanguages(List.of("DE", "FR"));
           return config;
       }

       public static Response createMockResponse(String translatedText) {
           Response response = new Response();
           Response.Translation translation = new Response.Translation();
           translation.text = translatedText;
           translation.detectedSourceLanguage = "EN";
           response.translations = List.of(translation);
           return response;
       }
   }
   ```

3. **Create Integration Tests**
   ```java
   @SpringBootTest
   @TestPropertySource(properties = {
       "spring.test.context.cache.maxSize=1",
       "logging.level.de.vkoop=DEBUG"
   })
   class CommandIntegrationTest {

       @Autowired
       private TestRestTemplate restTemplate;

       @Autowired
       private TemporaryFileManager fileManager;

       @MockBean
       private TranslateClient mockTranslateClient;

       @Test
       void shouldExecuteTextTranslationCommand() throws IOException {
           // Arrange
           when(mockTranslateClient.getSupportedSourceLanguages())
               .thenReturn(Set.of("EN"));
           when(mockTranslateClient.getSupportedTargetLanguages())
               .thenReturn(Set.of("DE"));
           when(mockTranslateClient.translate("Hello", "EN", "DE"))
               .thenReturn(TestDataBuilder.createMockResponse("Hallo"));

           File configFile = fileManager.createTempConfigFile("test-key", "EN", "DE");

           // Act
           String[] args = {
               "translate", "text",
               "--text", "Hello",
               "-c", configFile.getAbsolutePath(),
               "-s", "EN",
               "-t", "DE"
           };

           CommandLineRunner runner = context.getBean(CommandLineRunner.class);
           runner.run(args);

           // Assert
           verify(mockTranslateClient).translate("Hello", "EN", "DE");
       }

       @Test
       void shouldExecuteJsonTranslationCommand() throws IOException {
           // Arrange
           String jsonContent = TestDataBuilder.createTestJson();
           File jsonFile = fileManager.createTempFile("test.json", jsonContent);
           File configFile = fileManager.createTempConfigFile("test-key", "EN", "DE");

           when(mockTranslateClient.getSupportedSourceLanguages())
               .thenReturn(Set.of("EN"));
           when(mockTranslateClient.getSupportedTargetLanguages())
               .thenReturn(Set.of("DE"));
           when(mockTranslateClient.translate(anyString(), eq("EN"), eq("DE")))
               .thenReturn(TestDataBuilder.createMockResponse("Translated"));

           // Act
           String[] args = {
               "translate", "json",
               "--json-file", jsonFile.getAbsolutePath(),
               "-c", configFile.getAbsolutePath(),
               "-s", "EN",
               "-t", "DE"
           };

           CommandLineRunner runner = context.getBean(CommandLineRunner.class);
           runner.run(args);

           // Assert
           verify(mockTranslateClient, atLeastOnce()).translate(anyString(), eq("EN"), eq("DE"));
       }
   }
   ```

4. **Create Configuration Tests**
   ```java
   @ExtendWith(MockitoExtension.class)
   class ConfigurationServiceTest {

       @Mock
       private Environment environment;

       @InjectMocks
       private ConfigurationService configurationService;

       @TempDir
       Path tempDir;

       @Test
       void shouldLoadConfigurationFromFile() throws IOException {
           // Arrange
           File configFile = createConfigFile("file-auth-key", "EN", "DE,FR");
           ConfigurationOptions options = ConfigurationOptions.builder()
               .configFile(configFile)
               .build();

           // Act
           TranslationConfig config = configurationService.loadConfiguration(options);

           // Assert
           assertEquals("file-auth-key", config.getAuthKey());
           assertEquals("EN", config.getSourceLanguage());
           assertEquals(List.of("DE", "FR"), config.getTargetLanguages());
       }

       @Test
       void shouldMergeConfigurationSources() throws IOException {
           // Arrange - CLI args should override file config
           File configFile = createConfigFile("file-key", "EN", "DE");
           ConfigurationOptions options = ConfigurationOptions.builder()
               .configFile(configFile)
               .authKey("cli-key")
               .targetLanguages(List.of("FR", "ES"))
               .build();

           // Act
           TranslationConfig config = configurationService.loadConfiguration(options);

           // Assert
           assertEquals("cli-key", config.getAuthKey()); // CLI override
           assertEquals("EN", config.getSourceLanguage()); // From file
           assertEquals(List.of("FR", "ES"), config.getTargetLanguages()); // CLI override
       }

       @Test
       void shouldThrowExceptionWhenNoAuthProvided() {
           // Arrange
           ConfigurationOptions options = ConfigurationOptions.builder().build();

           // Act & Assert
           assertThrows(ConfigurationException.class, () ->
               configurationService.loadConfiguration(options));
       }

       private File createConfigFile(String authKey, String sourceLanguage, String targetLanguages) throws IOException {
           Properties props = new Properties();
           props.setProperty("authKey", authKey);
           props.setProperty("sourceLanguage", sourceLanguage);
           props.setProperty("targetLanguages", targetLanguages);

           File configFile = tempDir.resolve("test.properties").toFile();
           try (FileOutputStream fos = new FileOutputStream(configFile)) {
               props.store(fos, "Test config");
           }
           return configFile;
       }
   }
   ```

5. **Create Performance Tests**
   ```java
   @SpringBootTest
   class ParallelTranslationPerformanceTest {

       @MockBean
       private TranslateClient mockClient;

       @Autowired
       private JsonTranslator jsonTranslator;

       @Test
       void shouldHandleParallelTranslationsEfficiently() {
           // Arrange
           when(mockClient.translate(anyString(), anyString(), anyString()))
               .thenAnswer(invocation -> {
                   Thread.sleep(100); // Simulate API delay
                   return TestDataBuilder.createMockResponse("translated");
               });

           List<String> targetLanguages = List.of("DE", "FR", "ES", "IT", "PT");

           // Act
           long startTime = System.currentTimeMillis();

           targetLanguages.parallelStream().forEach(lang -> {
               try {
                   jsonTranslator.translateJsonFile("test.json", "EN", lang);
               } catch (IOException e) {
                   fail("Translation failed", e);
               }
           });

           long duration = System.currentTimeMillis() - startTime;

           // Assert
           assertTrue(duration < 1000, "Parallel processing should complete faster than sequential");
           verify(mockClient, times(targetLanguages.size())).translate(anyString(), eq("EN"), anyString());
       }
   }
   ```

6. **Create Contract Tests with WireMock**
   ```java
   @SpringBootTest
   class DeeplApiContractTest {

       @RegisterExtension
       static WireMockExtension wireMock = WireMockExtension.newInstance()
           .options(wireMockConfig().port(8089))
           .build();

       @Autowired
       private DeeplTranslateClient deeplClient;

       @Test
       void shouldHandleSuccessfulApiResponse() {
           // Arrange
           wireMock.stubFor(get(urlPathEqualTo("/v2/translate"))
               .withQueryParam("auth_key", equalTo("test-key"))
               .willReturn(aResponse()
                   .withStatus(200)
                   .withHeader("Content-Type", "application/json")
                   .withBody("{\"translations\":[{\"detected_source_language\":\"EN\",\"text\":\"Hallo Welt\"}]}")));

           // Act
           Response response = deeplClient.translate("Hello World", "EN", "DE");

           // Assert
           assertNotNull(response);
           assertEquals("Hallo Welt", response.translations.get(0).text);
       }

       @Test
       void shouldHandleApiError() {
           // Arrange
           wireMock.stubFor(get(urlPathEqualTo("/v2/translate"))
               .willReturn(aResponse()
                   .withStatus(403)
                   .withBody("{\"message\":\"Authorization failed\"}")));

           // Act & Assert
           assertThrows(TranslationException.class, () ->
               deeplClient.translate("Hello", "EN", "DE"));
       }
   }
   ```

### Testing Configuration

```properties
# application-test.properties
spring.profiles.active=test

# Disable external dependencies in tests
translation.client.deepl.enabled=false
translation.client.ollama.enabled=false

# Use in-memory database for tests
spring.datasource.url=jdbc:h2:mem:testdb
spring.jpa.hibernate.ddl-auto=create-drop

# Faster test execution
logging.level.org.springframework=WARN
logging.level.org.hibernate=WARN
```

### Test Execution Strategy

```gradle
test {
    useJUnitPlatform()

    // Parallel test execution
    maxParallelForks = Runtime.runtime.availableProcessors().intdiv(2) ?: 1

    testLogging {
        events "passed", "skipped", "failed"
        exceptionFormat "full"
    }

    // Test coverage
    finalizedBy jacocoTestReport
}

jacocoTestReport {
    reports {
        xml.required = true
        html.required = true
    }
}
```

### Benefits

- **Confidence**: Comprehensive test coverage ensures reliability
- **Regression Prevention**: Tests catch breaking changes early
- **Documentation**: Tests serve as executable documentation
- **Refactoring Safety**: Safe to refactor with good test coverage
- **Performance Assurance**: Performance tests catch regressions

### Dependencies

- Should be implemented after critical fixes (Tasks 01-03)
- Can run in parallel with medium and low priority tasks
- Helps validate all other improvements