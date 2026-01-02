# Task: Extract Configuration Service

**Priority**: Medium
**Estimated Time**: 2-3 hours
**Agent Type**: general-purpose

## Objective

Extract configuration loading logic from `BaseCommand` into a dedicated service to improve separation of concerns, testability, and reusability.

## Context

Currently `BaseCommand:54-79` contains complex configuration loading logic that:
- Mixes command-line argument handling with file I/O
- Has nested conditionals that are hard to test
- Cannot be reused by other components
- Violates single responsibility principle

## Acceptance Criteria

- [ ] Configuration loading extracted to separate service class
- [ ] `BaseCommand` simplified and focused on CLI concerns
- [ ] Configuration service is easily testable
- [ ] Support for multiple configuration sources (file, home, environment)
- [ ] Configuration precedence clearly defined
- [ ] Existing functionality preserved

## Implementation Notes

### Files to Create

- `src/main/java/de/vkoop/config/ConfigurationService.java`
- `src/main/java/de/vkoop/config/TranslationConfig.java`
- `src/test/java/de/vkoop/config/ConfigurationServiceTest.java`

### Files to Modify

- `src/main/java/de/vkoop/commands/BaseCommand.java`

### Steps

1. **Create Configuration Model**
   ```java
   @Component
   @ConfigurationProperties(prefix = "translation")
   public class TranslationConfig {
       private String authKey;
       private String sourceLanguage;
       private List<String> targetLanguages;
       private String clientName;

       // getters, setters, validation annotations
   }
   ```

2. **Create Configuration Service**
   ```java
   @Service
   public class ConfigurationService {

       public TranslationConfig loadConfiguration(ConfigurationOptions options) throws ConfigurationException {
           TranslationConfig config = new TranslationConfig();

           // Priority order: CLI args > config file > home config > defaults
           if (options.hasHomeConfig()) {
               mergeHomeConfig(config);
           }

           if (options.hasConfigFile()) {
               mergeFileConfig(config, options.getConfigFile());
           }

           mergeCliOptions(config, options);
           validateConfiguration(config);

           return config;
       }

       private void mergeHomeConfig(TranslationConfig config) throws ConfigurationException {
           Path homeConfigPath = Paths.get(System.getProperty("user.home"), ".transcli.properties");
           if (Files.exists(homeConfigPath)) {
               mergePropertiesFile(config, homeConfigPath.toFile());
           }
       }
   }
   ```

3. **Create Configuration Options DTO**
   ```java
   public class ConfigurationOptions {
       private final File configFile;
       private final boolean loadFromHome;
       private final String authKey;
       private final String sourceLanguage;
       private final List<String> targetLanguages;

       // constructor, getters, builder pattern
   }
   ```

4. **Update BaseCommand**
   ```java
   public abstract class BaseCommand implements Runnable {
       @Autowired
       private ConfigurationService configurationService;

       @Autowired
       protected TranslateClient translateClient;

       protected TranslationConfig config;

       protected void initializeConfiguration() {
           ConfigurationOptions options = ConfigurationOptions.builder()
               .configFile(configurationFile)
               .loadFromHome(loadConfigFromHome)
               .authKey(authKey)
               .sourceLanguage(sourceLanguage)
               .targetLanguages(targetLanguages)
               .build();

           config = configurationService.loadConfiguration(options);
           translateClient.setAuthKey(config.getAuthKey());
       }

       protected void validateLanguages() {
           if (!translateClient.getSupportedSourceLanguages().contains(config.getSourceLanguage())) {
               throw new TranslationException("Unsupported source language: " + config.getSourceLanguage());
           }

           for (String targetLanguage : config.getTargetLanguages()) {
               if (!translateClient.getSupportedTargetLanguages().contains(targetLanguage)) {
                   throw new TranslationException("Unsupported target language: " + targetLanguage);
               }
           }
       }
   }
   ```

### Configuration Precedence

1. **Command-line arguments** (highest priority)
2. **Configuration file** (`-c` option)
3. **Home configuration** (`~/.transcli.properties`)
4. **Environment variables**
5. **Application defaults** (lowest priority)

### Testing Strategy

```java
@ExtendWith(MockitoExtension.class)
class ConfigurationServiceTest {

    @Test
    void shouldLoadFromHomeDirectory() throws Exception {
        // Test loading from ~/.transcli.properties
    }

    @Test
    void shouldMergeConfigurationSources() throws Exception {
        // Test precedence: CLI > file > home
    }

    @Test
    void shouldValidateRequiredFields() throws Exception {
        // Test validation of required configuration
    }
}
```

### Benefits

- **Testability**: Easy to unit test configuration loading
- **Separation of Concerns**: Commands focus on business logic
- **Reusability**: Configuration service can be used by other components
- **Extensibility**: Easy to add new configuration sources
- **Maintainability**: Complex logic isolated in one place

### Dependencies

- Task 01 (System.exit replacement) should be completed first
- This task enables better error handling in configuration loading

## Migration Guide

1. Extract configuration logic to service
2. Update all command classes to use configuration service
3. Add comprehensive tests for configuration service
4. Update documentation for new configuration precedence rules