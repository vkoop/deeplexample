# Task: Fix URL Construction and Encoding Issues

**Priority**: Critical
**Estimated Time**: 1-2 hours
**Agent Type**: general-purpose

## Objective

Replace unsafe URL string concatenation in `DeeplTranslateClient` with proper URL building and encoding to prevent security issues and API call failures.

## Context

The current implementation in `DeeplTranslateClient:116-123` builds URLs through string concatenation:
- Vulnerable to injection if user input contains special characters
- Manual URL encoding is error-prone
- Difficult to read and maintain

## Acceptance Criteria

- [ ] URL construction uses proper URI building classes
- [ ] All parameters are automatically encoded
- [ ] Code is more readable and maintainable
- [ ] Existing functionality is preserved
- [ ] Tests validate URL construction with special characters

## Implementation Notes

### Files to Modify

- `src/main/java/de/vkoop/clients/DeeplTranslateClient.java` (lines 116-123)

### Steps

1. **Add Apache HTTP Components dependency** (if not present)
   ```gradle
   implementation 'org.apache.httpcomponents.client5:httpclient5:5.2.1'
   ```

2. **Alternative: Use Java 11+ URI Builder pattern**
   ```java
   private URI buildTranslationUri(String text, String sourceLanguage, String targetLanguage) {
       try {
           return new URI("https", "api-free.deepl.com", "/v2/translate",
               buildQueryString(text, sourceLanguage, targetLanguage), null);
       } catch (URISyntaxException e) {
           throw new IllegalArgumentException("Invalid parameters for URL construction", e);
       }
   }

   private String buildQueryString(String text, String sourceLanguage, String targetLanguage) {
       return String.format("auth_key=%s&text=%s&target_lang=%s&source_lang=%s",
           URLEncoder.encode(authKey, StandardCharsets.UTF_8),
           URLEncoder.encode(text, StandardCharsets.UTF_8),
           URLEncoder.encode(targetLanguage, StandardCharsets.UTF_8),
           URLEncoder.encode(sourceLanguage, StandardCharsets.UTF_8));
   }
   ```

3. **Update translate method**
   ```java
   @Override
   public Response translate(String text, String sourceLanguage, String targetLanguage) {
       try {
           final URI uri = buildTranslationUri(text, sourceLanguage, targetLanguage);

           var client = getHttpClient().send(
               HttpRequest.newBuilder()
                   .uri(uri)
                   .header("Accept", "application/json")
                   .GET()
                   .build(),
               HttpResponse.BodyHandlers.ofString());
           // ... rest of method
       }
   }
   ```

### Testing Strategy

- Test with special characters: `äöü`, spaces, quotes, `&`, `=`
- Test with very long text
- Test with empty parameters
- Verify generated URLs match expected format

### Security Considerations

- Ensure auth key is properly encoded
- Validate that special characters in text don't break URL structure
- Consider request size limits

### Dependencies

- Task 01 (System.exit replacement) should be completed first
- This enables better error handling if URL construction fails

## Implementation Example

```java
public Response translate(String text, String sourceLanguage, String targetLanguage) {
    if (text == null || text.trim().isEmpty()) {
        throw new TranslationException("Text cannot be null or empty");
    }

    try {
        final URI uri = buildTranslationUri(text, sourceLanguage, targetLanguage);

        var response = getHttpClient().send(
            HttpRequest.newBuilder()
                .uri(uri)
                .header("Accept", "application/json")
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString());

        return processResponse(response);
    } catch (URISyntaxException e) {
        throw new TranslationException("Failed to construct API URL", e);
    } catch (InterruptedException | IOException e) {
        if (e instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
        throw new TranslationException("Translation API call failed", e);
    }
}
```