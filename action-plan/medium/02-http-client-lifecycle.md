# Task: Proper HTTP Client Lifecycle Management

**Priority**: Medium
**Estimated Time**: 1-2 hours
**Agent Type**: general-purpose

## Objective

Implement proper HTTP client lifecycle management to improve performance, resource utilization, and thread safety.

## Context

Currently `DeeplTranslateClient`:
- Creates HTTP client instances per translation (inefficient)
- Doesn't implement proper connection pooling
- May have thread safety issues
- Doesn't configure timeouts or retry policies

## Acceptance Criteria

- [ ] Single HTTP client instance shared across translations
- [ ] Proper connection pooling configured
- [ ] Appropriate timeouts configured
- [ ] Thread-safe implementation
- [ ] Proper resource cleanup
- [ ] Performance improvement in concurrent scenarios

## Implementation Notes

### Files to Modify

- `src/main/java/de/vkoop/clients/DeeplTranslateClient.java`
- `build.gradle` (if adding dependencies)

### Configuration Class

Create HTTP client as Spring Bean:

```java
@Configuration
public class HttpClientConfig {

    @Bean
    @ConditionalOnProperty(name = "translate.client", havingValue = "deepl")
    public HttpClient deeplHttpClient() {
        return HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    }
}
```

### Update DeeplTranslateClient

```java
@ConditionalOnProperty(name = "translate.client", havingValue = "deepl")
@Component("deeplClient")
public class DeeplTranslateClient implements TranslateClient {
    private static final Logger logger = LoggerFactory.getLogger(DeeplTranslateClient.class);

    private final HttpClient httpClient;
    private String authKey;

    // Constructor injection for HTTP client
    public DeeplTranslateClient(HttpClient httpClient) {
        this.httpClient = httpClient;
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @Override
    public Response translate(String text, String sourceLanguage, String targetLanguage) {
        try {
            final URI uri = buildTranslationUri(text, sourceLanguage, targetLanguage);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Accept", "application/json")
                .header("User-Agent", "transcli/1.0")
                .timeout(Duration.ofSeconds(30))  // Request timeout
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

            return processResponse(response);

        } catch (InterruptedException | IOException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new TranslationException("Translation API call failed", e);
        }
    }

    // Remove createHttpClient and getHttpClient methods
}
```

### Advanced Configuration (Optional)

For high-performance scenarios, consider using Apache HttpClient 5:

```gradle
implementation 'org.apache.httpcomponents.client5:httpclient5:5.2.1'
```

```java
@Configuration
public class HttpClientConfig {

    @Bean
    @ConditionalOnProperty(name = "translate.client", havingValue = "deepl")
    public CloseableHttpClient deeplHttpClient() {
        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(Timeout.ofSeconds(10))
            .setResponseTimeout(Timeout.ofSeconds(30))
            .build();

        PoolingHttpClientConnectionManager connectionManager =
            PoolingHttpClientConnectionManagerBuilder.create()
                .setMaxConnPerRoute(10)
                .setMaxConnTotal(50)
                .build();

        return HttpClients.custom()
            .setConnectionManager(connectionManager)
            .setDefaultRequestConfig(requestConfig)
            .setRetryStrategy(new DefaultHttpRequestRetryStrategy(3, TimeValue.ofSeconds(1)))
            .build();
    }

    @PreDestroy
    public void cleanup() {
        // Spring will handle this automatically with @Bean
    }
}
```

### Error Handling and Retry Logic

```java
private Response processResponse(HttpResponse<String> response) throws TranslationException {
    int statusCode = response.statusCode();
    String responseBody = response.body();

    switch (statusCode) {
        case 200:
            try {
                return objectMapper.readValue(responseBody, Response.class);
            } catch (JsonProcessingException e) {
                throw new TranslationException("Invalid response format from DeepL API", e);
            }

        case 429: // Rate limited
            throw new TranslationException("Rate limit exceeded. Please try again later.");

        case 403: // Forbidden - likely auth issue
            throw new TranslationException("Authentication failed. Check your API key.");

        case 456: // Quota exceeded
            throw new TranslationException("Translation quota exceeded.");

        default:
            logger.error("DeepL API returned error code {}: {}", statusCode, responseBody);
            throw new TranslationException(
                String.format("DeepL API error: %d - %s", statusCode, extractErrorMessage(responseBody)));
    }
}

private String extractErrorMessage(String responseBody) {
    try {
        JsonNode errorNode = objectMapper.readTree(responseBody);
        return errorNode.has("message") ? errorNode.get("message").asText() : "Unknown error";
    } catch (Exception e) {
        return "Could not parse error response";
    }
}
```

### Testing Strategy

- Test connection reuse with multiple concurrent translations
- Test timeout behavior
- Test error scenarios (rate limiting, auth failures)
- Performance test comparing old vs new implementation

### Configuration Properties

Add to `application.properties`:
```properties
# HTTP client configuration
http.client.connect-timeout=10s
http.client.read-timeout=30s
http.client.max-connections=50
http.client.max-connections-per-route=10
```

### Benefits

- **Performance**: Connection pooling and reuse
- **Resource Efficiency**: Proper connection management
- **Reliability**: Timeouts and retry logic
- **Maintainability**: Centralized HTTP configuration
- **Scalability**: Better handling of concurrent requests

### Dependencies

- This task is independent but benefits from Task 01 (exception handling)
- Can be implemented alongside other tasks

## Performance Comparison

Before: New HTTP client per request
After: Shared connection pool

Expected improvements:
- 30-50% reduction in connection establishment time
- Better resource utilization
- Improved throughput for concurrent translations