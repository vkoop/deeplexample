# Task: Add Error Isolation in Parallel Processing

**Priority**: Critical
**Estimated Time**: 1-2 hours
**Agent Type**: general-purpose

## Objective

Improve error handling in `JsonCommand` parallel processing so that one failed translation doesn't crash the entire batch operation.

## Context

Currently in `JsonCommand:40`, the parallel stream processing can fail completely if any single translation fails:
- One API error stops all translations
- No partial results are saved
- Poor user experience with large JSON files

## Acceptance Criteria

- [ ] Individual translation failures are logged but don't stop the batch
- [ ] Partial results are preserved when some translations fail
- [ ] Clear reporting of which translations succeeded/failed
- [ ] Option to continue or fail-fast based on configuration
- [ ] Proper error aggregation and reporting

## Implementation Notes

### Files to Modify

- `src/main/java/de/vkoop/commands/JsonCommand.java` (line 40)
- `src/main/java/de/vkoop/JsonTranslator.java` (method `translateJsonFile`)

### Steps

1. **Add error tracking to JsonTranslator**
   ```java
   public class TranslationResult {
       private final Map<String, Object> translatedMap;
       private final List<TranslationError> errors;
       // constructors, getters
   }

   public static class TranslationError {
       private final String key;
       private final String originalValue;
       private final Exception error;
       // constructors, getters
   }
   ```

2. **Update JsonTranslator to handle errors gracefully**
   ```java
   public TranslationResult translateJsonFile(String filename, String sourceLang, String targetLang) throws IOException {
       final Map<String, Object> stringObjectMap = parseAsMap(filename);
       final List<TranslationError> errors = new ArrayList<>();

       Map<String, Object> result = MapUtils.map(stringObjectMap, (key, value) -> {
           try {
               var response = translateClient.translate(value, sourceLang, targetLang);

               if (response != null && response.translations != null && !response.translations.isEmpty()) {
                   return response.translations.get(0).text;
               } else {
                   errors.add(new TranslationError(key, value, new TranslationException("Empty response")));
                   return value; // Keep original on failure
               }
           } catch (Exception e) {
               errors.add(new TranslationError(key, value, e));
               return value; // Keep original on failure
           }
       });

       return new TranslationResult(result, errors);
   }
   ```

3. **Update JsonCommand to handle partial failures**
   ```java
   @Override
   public void run() {
       loadConfigFromFile();
       validateLanguages();

       final Map<String, List<TranslationError>> allErrors = new ConcurrentHashMap<>();

       targetLanguages.stream()
           .parallel()
           .forEach(targetLanguage -> {
               try {
                   TranslationResult result = jsonTranslator.translateJsonFile(
                       jsonFile, sourceLanguage, targetLanguage);

                   if (!result.getErrors().isEmpty()) {
                       allErrors.put(targetLanguage, result.getErrors());
                       logger.warn("Translation to {} had {} errors", targetLanguage, result.getErrors().size());
                   }

                   writeResultFile(result.getTranslatedMap(), targetLanguage);
                   logger.info("Successfully translated to {}", targetLanguage);

               } catch (Exception e) {
                   logger.error("Failed to translate to language: {}", targetLanguage, e);
                   allErrors.put(targetLanguage, List.of(new TranslationError("FATAL", "", e)));
               }
           });

       reportResults(allErrors);
   }
   ```

4. **Add result reporting**
   ```java
   private void reportResults(Map<String, List<TranslationError>> allErrors) {
       int totalLanguages = targetLanguages.size();
       int successfulLanguages = totalLanguages - allErrors.size();

       System.out.printf("Translation completed: %d/%d languages successful%n",
           successfulLanguages, totalLanguages);

       if (!allErrors.isEmpty()) {
           System.out.println("\nErrors by language:");
           allErrors.forEach((lang, errors) -> {
               System.out.printf("  %s: %d errors%n", lang, errors.size());
               errors.forEach(error ->
                   System.out.printf("    - %s: %s%n", error.getKey(), error.getError().getMessage()));
           });
       }
   }
   ```

### Configuration Options

Add optional fail-fast behavior:
```java
@Option(names = "--fail-fast", description = "Stop on first error instead of continuing")
public boolean failFast = false;
```

### Testing Strategy

- Test with JSON containing mix of translatable and problematic text
- Test with API failures (mock HTTP errors)
- Test with invalid language codes for some entries
- Verify partial results are saved correctly

### Dependencies

- Task 01 (System.exit replacement) should be completed first
- Task 02 (URL encoding) reduces likelihood of translation failures

## Error Handling Flow

```
JSON File → Parse → For each string value:
  ├─ Translation Success → Use translated value
  ├─ Translation Failure → Log error, keep original value
  └─ Fatal Error → Log error, mark language as failed

Result: Partial translations saved, errors reported
```