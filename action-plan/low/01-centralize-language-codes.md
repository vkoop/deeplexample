# Task: Centralize Language Code Management

**Priority**: Low
**Estimated Time**: 1-2 hours
**Agent Type**: general-purpose

## Objective

Centralize language code definitions and validation logic to eliminate duplication and make language support management easier.

## Context

Currently language codes are:
- Hardcoded in multiple client classes
- Duplicated between `DeeplTranslateClient` and `OllamaTranslateClient`
- Different sets for each client without clear mapping
- No central place to manage language support

## Acceptance Criteria

- [ ] Single source of truth for language codes
- [ ] Clear mapping between different client language codes
- [ ] External configuration support for language codes
- [ ] Validation logic centralized
- [ ] Easy to add support for new languages

## Implementation Notes

### Files to Create

- `src/main/java/de/vkoop/language/LanguageCodeRegistry.java`
- `src/main/java/de/vkoop/language/LanguageMapping.java`
- `src/main/resources/language-mappings.json`

### Files to Modify

- `src/main/java/de/vkoop/clients/DeeplTranslateClient.java`
- `src/main/java/de/vkoop/clients/OllamaTranslateClient.java`

### Implementation Steps

1. **Create Language Code Registry**
   ```java
   @Component
   public class LanguageCodeRegistry {
       private final Map<String, LanguageMapping> languageMappings;

       public LanguageCodeRegistry() {
           this.languageMappings = loadLanguageMappings();
       }

       public Set<String> getSupportedLanguages() {
           return languageMappings.keySet();
       }

       public String getClientLanguageCode(String standardCode, String clientType) {
           LanguageMapping mapping = languageMappings.get(standardCode);
           if (mapping == null) {
               throw new IllegalArgumentException("Unsupported language: " + standardCode);
           }
           return mapping.getClientCode(clientType);
       }

       public boolean isLanguageSupported(String languageCode, String clientType) {
           LanguageMapping mapping = languageMappings.get(languageCode);
           return mapping != null && mapping.supportsClient(clientType);
       }

       private Map<String, LanguageMapping> loadLanguageMappings() {
           // Load from JSON configuration file
           try (InputStream is = getClass().getResourceAsStream("/language-mappings.json")) {
               ObjectMapper mapper = new ObjectMapper();
               LanguageMappingConfig config = mapper.readValue(is, LanguageMappingConfig.class);
               return config.getMappings().stream()
                   .collect(Collectors.toMap(
                       LanguageMapping::getStandardCode,
                       Function.identity()
                   ));
           } catch (IOException e) {
               throw new RuntimeException("Failed to load language mappings", e);
           }
       }
   }
   ```

2. **Create Language Mapping Model**
   ```java
   public class LanguageMapping {
       private String standardCode;
       private String name;
       private Map<String, String> clientCodes;
       private Set<String> supportedAsSource;
       private Set<String> supportedAsTarget;

       public String getClientCode(String clientType) {
           return clientCodes.getOrDefault(clientType, standardCode);
       }

       public boolean supportsClient(String clientType) {
           return clientCodes.containsKey(clientType);
       }

       public boolean isSupportedAsSource(String clientType) {
           return supportedAsSource.contains(clientType);
       }

       public boolean isSupportedAsTarget(String clientType) {
           return supportedAsTarget.contains(clientType);
       }

       // getters, setters
   }
   ```

3. **Create Language Configuration File**
   ```json
   {
     "mappings": [
       {
         "standardCode": "EN",
         "name": "English",
         "clientCodes": {
           "deepl": "EN",
           "ollama": "EN"
         },
         "supportedAsSource": ["deepl", "ollama"],
         "supportedAsTarget": ["deepl", "ollama"]
       },
       {
         "standardCode": "EN-US",
         "name": "English (US)",
         "clientCodes": {
           "deepl": "EN-US",
           "ollama": "EN"
         },
         "supportedAsSource": ["deepl"],
         "supportedAsTarget": ["deepl"]
       },
       {
         "standardCode": "DE",
         "name": "German",
         "clientCodes": {
           "deepl": "DE",
           "ollama": "DE"
         },
         "supportedAsSource": ["deepl", "ollama"],
         "supportedAsTarget": ["deepl", "ollama"]
       }
     ]
   }
   ```

4. **Update Client Implementations**
   ```java
   @Component("deeplClient")
   public class DeeplTranslateClient implements TranslateClient {
       private final LanguageCodeRegistry languageRegistry;

       public DeeplTranslateClient(LanguageCodeRegistry languageRegistry) {
           this.languageRegistry = languageRegistry;
       }

       @Override
       public Set<String> getSupportedSourceLanguages() {
           return languageRegistry.getSupportedLanguages().stream()
               .filter(lang -> languageRegistry.isLanguageSupported(lang, "deepl"))
               .filter(lang -> languageRegistry.getMapping(lang).isSupportedAsSource("deepl"))
               .collect(Collectors.toSet());
       }

       @Override
       public Set<String> getSupportedTargetLanguages() {
           return languageRegistry.getSupportedLanguages().stream()
               .filter(lang -> languageRegistry.isLanguageSupported(lang, "deepl"))
               .filter(lang -> languageRegistry.getMapping(lang).isSupportedAsTarget("deepl"))
               .collect(Collectors.toSet());
       }

       @Override
       public Response translate(String text, String sourceLanguage, String targetLanguage) {
           // Convert standard codes to client-specific codes
           String deeplSourceLang = languageRegistry.getClientLanguageCode(sourceLanguage, "deepl");
           String deeplTargetLang = languageRegistry.getClientLanguageCode(targetLanguage, "deepl");

           // Use converted codes in API call
           final URI uri = buildTranslationUri(text, deeplSourceLang, deeplTargetLang);
           // ... rest of implementation
       }
   }
   ```

5. **Add Language Validation Service**
   ```java
   @Service
   public class LanguageValidationService {
       private final LanguageCodeRegistry languageRegistry;

       public void validateLanguageSupport(String sourceLanguage, List<String> targetLanguages, String clientType) {
           if (!languageRegistry.isLanguageSupported(sourceLanguage, clientType)) {
               throw new TranslationException(
                   String.format("Source language '%s' is not supported by %s client", sourceLanguage, clientType));
           }

           for (String targetLanguage : targetLanguages) {
               if (!languageRegistry.isLanguageSupported(targetLanguage, clientType)) {
                   throw new TranslationException(
                       String.format("Target language '%s' is not supported by %s client", targetLanguage, clientType));
               }
           }
       }

       public List<String> getSupportedLanguages(String clientType, boolean sourceOnly) {
           return languageRegistry.getSupportedLanguages().stream()
               .filter(lang -> languageRegistry.isLanguageSupported(lang, clientType))
               .filter(lang -> !sourceOnly || languageRegistry.getMapping(lang).isSupportedAsSource(clientType))
               .collect(Collectors.toList());
       }
   }
   ```

### CLI Enhancement

Add language listing command:
```java
@Command(name = "list-languages", description = "List supported languages for translation clients")
public class ListLanguagesCommand implements Runnable {
    @Autowired
    private LanguageValidationService languageService;

    @Option(names = "--client", description = "Filter by client type (deepl, ollama)")
    private String clientType;

    @Override
    public void run() {
        if (clientType != null) {
            System.out.println("Supported languages for " + clientType + ":");
            languageService.getSupportedLanguages(clientType, false)
                .forEach(lang -> System.out.println("  " + lang));
        } else {
            System.out.println("All supported languages:");
            // Show all languages with client support matrix
        }
    }
}
```

### Benefits

- **Maintainability**: Single place to manage language codes
- **Flexibility**: Easy to add new languages or clients
- **Consistency**: Uniform language code handling across clients
- **Extensibility**: External configuration allows customization
- **Validation**: Centralized validation logic

### Testing Strategy

```java
@Test
void shouldMapLanguageCodesCorrectly() {
    assertEquals("EN-US", registry.getClientLanguageCode("EN-US", "deepl"));
    assertEquals("EN", registry.getClientLanguageCode("EN-US", "ollama"));
}

@Test
void shouldValidateLanguageSupport() {
    assertTrue(registry.isLanguageSupported("DE", "deepl"));
    assertFalse(registry.isLanguageSupported("INVALID", "deepl"));
}
```

### Configuration Options

Allow runtime language configuration:
```properties
# Language configuration
language.config.file=classpath:language-mappings.json
language.config.reload-interval=300s
language.validation.strict=true
```

### Dependencies

- Independent task that can be implemented anytime
- Enhances other tasks but doesn't block them
- Can be implemented after client selection strategy for better integration