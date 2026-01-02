# Task: Implement Dynamic Client Selection Strategy

**Priority**: Medium
**Estimated Time**: 2-3 hours
**Agent Type**: general-purpose

## Objective

Replace the current `@ConditionalOnProperty` approach with a dynamic client selection strategy that allows runtime switching between translation clients.

## Context

Currently:
- Client selection is determined at startup via `@ConditionalOnProperty`
- Cannot switch clients without restarting the application
- Limited flexibility for different use cases
- Hard to implement fallback strategies

## Acceptance Criteria

- [ ] Runtime client selection based on configuration or command-line arguments
- [ ] Support for client fallback strategies
- [ ] Easy to add new translation clients
- [ ] Maintain backward compatibility
- [ ] Clear client selection logic and documentation

## Implementation Notes

### Files to Create

- `src/main/java/de/vkoop/client/TranslateClientFactory.java`
- `src/main/java/de/vkoop/client/ClientSelectionStrategy.java`
- `src/test/java/de/vkoop/client/TranslateClientFactoryTest.java`

### Files to Modify

- `src/main/java/de/vkoop/commands/BaseCommand.java`
- `src/main/java/de/vkoop/clients/DeeplTranslateClient.java`
- `src/main/java/de/vkoop/clients/OllamaTranslateClient.java`

### Implementation Steps

1. **Create Client Factory Interface**
   ```java
   @Component
   public class TranslateClientFactory {
       private final Map<String, TranslateClient> clients;

       public TranslateClientFactory(List<TranslateClient> availableClients) {
           this.clients = availableClients.stream()
               .collect(Collectors.toMap(
                   this::getClientName,
                   Function.identity()
               ));
       }

       public TranslateClient getClient(String clientName) throws TranslationException {
           TranslateClient client = clients.get(clientName.toLowerCase());
           if (client == null) {
               throw new TranslationException(
                   String.format("Unknown translation client: %s. Available clients: %s",
                       clientName, getAvailableClientNames()));
           }
           return client;
       }

       public Set<String> getAvailableClientNames() {
           return clients.keySet();
       }

       private String getClientName(TranslateClient client) {
           if (client instanceof DeeplTranslateClient) return "deepl";
           if (client instanceof OllamaTranslateClient) return "ollama";
           throw new IllegalArgumentException("Unknown client type: " + client.getClass());
       }
   }
   ```

2. **Create Selection Strategy**
   ```java
   @Component
   public class ClientSelectionStrategy {

       private final TranslateClientFactory clientFactory;

       public ClientSelectionStrategy(TranslateClientFactory clientFactory) {
           this.clientFactory = clientFactory;
       }

       public TranslateClient selectClient(String preferredClient, String fallbackClient) {
           try {
               return clientFactory.getClient(preferredClient);
           } catch (TranslationException e) {
               if (fallbackClient != null) {
                   logger.warn("Preferred client '{}' not available, falling back to '{}'",
                       preferredClient, fallbackClient);
                   return clientFactory.getClient(fallbackClient);
               }
               throw e;
           }
       }

       public TranslateClient selectBestAvailableClient() {
           Set<String> available = clientFactory.getAvailableClientNames();

           // Preference order: deepl (most accurate) -> ollama (local/free)
           if (available.contains("deepl")) {
               return clientFactory.getClient("deepl");
           }
           if (available.contains("ollama")) {
               return clientFactory.getClient("ollama");
           }

           throw new TranslationException("No translation clients available");
       }
   }
   ```

3. **Update Client Implementations**
   ```java
   @Component("deeplClient")
   public class DeeplTranslateClient implements TranslateClient {
       // Remove @ConditionalOnProperty annotation

       @Override
       public boolean isAvailable() {
           // Check if DeepL API is accessible and auth key is valid
           return authKey != null && !authKey.isEmpty();
       }
   }

   @Component("ollamaClient")
   public class OllamaTranslateClient implements TranslateClient {
       // Remove @ConditionalOnProperty annotation

       @Override
       public boolean isAvailable() {
           try {
               // Simple health check to Ollama service
               chatModel.call(new Prompt("test"));
               return true;
           } catch (Exception e) {
               return false;
           }
       }
   }
   ```

4. **Update TranslateClient Interface**
   ```java
   public interface TranslateClient {
       Response translate(String text, String sourceLanguage, String targetLanguage);
       Set<String> getSupportedSourceLanguages();
       Set<String> getSupportedTargetLanguages();
       void setAuthKey(String authKey);

       // New methods for dynamic selection
       boolean isAvailable();
       String getClientName();
       String getDescription();
   }
   ```

5. **Update BaseCommand**
   ```java
   public abstract class BaseCommand implements Runnable {
       @Autowired
       private ClientSelectionStrategy clientSelectionStrategy;

       @CommandLine.Option(names = "-n", description = "Translation client (deepl, ollama)")
       private String translationClientName;

       @CommandLine.Option(names = "--fallback-client", description = "Fallback client if primary fails")
       private String fallbackClient;

       protected TranslateClient translateClient;

       protected void initializeTranslateClient() {
           if (translationClientName != null) {
               translateClient = clientSelectionStrategy.selectClient(translationClientName, fallbackClient);
           } else {
               translateClient = clientSelectionStrategy.selectBestAvailableClient();
           }

           // Apply configuration
           if (config.getAuthKey() != null) {
               translateClient.setAuthKey(config.getAuthKey());
           }
       }
   }
   ```

### Advanced Features

1. **Load Balancing Strategy**
   ```java
   @Component
   public class LoadBalancingClientStrategy {
       private final List<TranslateClient> clients;
       private final AtomicInteger counter = new AtomicInteger(0);

       public TranslateClient getNextClient() {
           List<TranslateClient> availableClients = clients.stream()
               .filter(TranslateClient::isAvailable)
               .collect(Collectors.toList());

           if (availableClients.isEmpty()) {
               throw new TranslationException("No available translation clients");
           }

           int index = counter.getAndIncrement() % availableClients.size();
           return availableClients.get(index);
       }
   }
   ```

2. **Health Check and Monitoring**
   ```java
   @Component
   public class ClientHealthMonitor {
       @Scheduled(fixedDelay = 30000) // Check every 30 seconds
       public void checkClientHealth() {
           clients.forEach((name, client) -> {
               boolean healthy = client.isAvailable();
               logger.info("Client {} is {}", name, healthy ? "healthy" : "unhealthy");
           });
       }
   }
   ```

### Configuration Options

Add to `application.properties`:
```properties
# Client selection
translation.client.preferred=deepl
translation.client.fallback=ollama
translation.client.auto-select=true

# Client-specific settings
translation.deepl.auth-key=${DEEPL_API_KEY:}
translation.ollama.base-url=http://localhost:11434
```

### Benefits

- **Flexibility**: Runtime client switching
- **Reliability**: Fallback mechanisms
- **Extensibility**: Easy to add new clients
- **Monitoring**: Health checks and availability tracking
- **Performance**: Load balancing capabilities

### Testing Strategy

```java
@Test
void shouldSelectPreferredClient() {
    // Test client selection logic
}

@Test
void shouldFallbackWhenPreferredUnavailable() {
    // Test fallback behavior
}

@Test
void shouldThrowWhenNoClientsAvailable() {
    // Test error handling
}
```

### Migration Path

1. Keep existing `@ConditionalOnProperty` for backward compatibility
2. Add factory-based selection as optional feature
3. Gradually migrate commands to use factory
4. Remove conditional properties in future version

### Dependencies

- Task 01 (exception handling) should be completed first
- Works well with Task 01 (configuration service)