# Task: Replace System.exit() Calls with Exception Handling

**Priority**: Critical
**Estimated Time**: 2-3 hours
**Agent Type**: general-purpose

## Objective

Replace all `System.exit(1)` calls in the codebase with proper exception handling to make the code more testable and follow better practices.

## Context

Currently `BaseCommand` has multiple `System.exit(1)` calls that:
- Make unit testing impossible
- Prevent proper error handling by callers
- Violate separation of concerns
- Make the CLI framework handle termination

## Acceptance Criteria

- [ ] All `System.exit()` calls removed from application code
- [ ] Custom exception classes created for different error scenarios
- [ ] Error handling propagated properly to CLI framework
- [ ] Existing tests still pass
- [ ] New tests can be written for error scenarios

## Implementation Notes

### Files to Modify

- `src/main/java/de/vkoop/commands/BaseCommand.java` (lines 43, 49, 57, 76)

### Steps

1. **Create Custom Exceptions**
   ```java
   // Create src/main/java/de/vkoop/exceptions/TranslationException.java
   public class TranslationException extends RuntimeException {
       public TranslationException(String message) { super(message); }
       public TranslationException(String message, Throwable cause) { super(message, cause); }
   }

   // Create src/main/java/de/vkoop/exceptions/ConfigurationException.java
   public class ConfigurationException extends RuntimeException {
       public ConfigurationException(String message) { super(message); }
       public ConfigurationException(String message, Throwable cause) { super(message, cause); }
   }
   ```

2. **Replace System.exit() calls**
   - Line 43-44: `throw new TranslationException("Unsupported source language: " + sourceLanguage)`
   - Line 48-49: `throw new TranslationException("Unsupported target language: " + targetLanguage)`
   - Line 56-57: `throw new ConfigurationException("No authentication provided")`
   - Line 75-76: `throw new ConfigurationException("Failed to load configuration file: " + configurationFile, e)`

3. **Update Picocli Integration**
   - Picocli will automatically convert uncaught exceptions to exit codes
   - Consider using `@Command(exitCodeOnException = {TranslationException.class: 2, ConfigurationException.class: 3})`

### Testing Strategy

- Create unit tests for `BaseCommand` methods
- Mock the `TranslateClient` to test language validation
- Test configuration loading with invalid files

### Dependencies

None - this is a foundational change that other tasks can build on.

## Implementation Example

```java
protected void validateLanguages() {
    if (!translateClient.getSupportedSourceLanguages().contains(sourceLanguage)) {
        logger.error("Unsupported source language: {}", sourceLanguage);
        throw new TranslationException("Unsupported source language: " + sourceLanguage);
    }

    for (String targetLanguage : targetLanguages) {
        if (!translateClient.getSupportedTargetLanguages().contains(targetLanguage)) {
            logger.error("Unsupported target language: {}", targetLanguage);
            throw new TranslationException("Unsupported target language: " + targetLanguage);
        }
    }
}
```