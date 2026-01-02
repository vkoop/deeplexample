# Task: Implement Structured Logging with Sensitive Data Masking

**Priority**: Low
**Estimated Time**: 1-2 hours
**Agent Type**: general-purpose

## Objective

Improve logging throughout the application with structured logging patterns, sensitive data masking, and configurable log levels for better observability and security.

## Context

Currently:
- Basic logging with simple messages
- Auth keys and potentially sensitive data may be logged
- No structured logging for metrics or debugging
- Limited logging configuration options

## Acceptance Criteria

- [ ] Structured logging with consistent format
- [ ] Sensitive data (auth keys, user text) properly masked
- [ ] Configurable log levels per component
- [ ] Performance metrics logging
- [ ] Error context preservation
- [ ] JSON logging option for production

## Implementation Notes

### Files to Create

- `src/main/java/de/vkoop/logging/SensitiveDataMasker.java`
- `src/main/java/de/vkoop/logging/StructuredLogger.java`
- `src/main/resources/logback-spring.xml`

### Files to Modify

- All classes with logging (add structured logging)
- `build.gradle` (add logging dependencies)

### Implementation Steps

1. **Add Logging Dependencies**
   ```gradle
   implementation 'net.logstash.logback:logstash-logback-encoder:7.4'
   implementation 'ch.qos.logback:logback-classic'
   implementation 'org.slf4j:slf4j-api'
   ```

2. **Create Sensitive Data Masker**
   ```java
   @Component
   public class SensitiveDataMasker {
       private static final String MASK = "***MASKED***";
       private static final List<String> SENSITIVE_KEYS = List.of(
           "authkey", "auth_key", "password", "token", "secret"
       );

       public String maskSensitiveData(String input) {
           if (input == null || input.length() < 8) {
               return MASK;
           }
           // Show first 4 and last 4 characters
           return input.substring(0, 4) + MASK + input.substring(input.length() - 4);
       }

       public Map<String, Object> maskSensitiveFields(Map<String, Object> data) {
           Map<String, Object> masked = new HashMap<>(data);

           masked.entrySet().removeIf(entry -> {
               String key = entry.getKey().toLowerCase();
               if (SENSITIVE_KEYS.stream().anyMatch(key::contains)) {
                   masked.put(entry.getKey(), maskSensitiveData(String.valueOf(entry.getValue())));
                   return true;
               }
               return false;
           });

           return masked;
       }

       public String maskUserText(String text) {
           if (text == null || text.length() <= 20) {
               return "[USER_TEXT:" + (text != null ? text.length() : 0) + "_CHARS]";
           }
           return text.substring(0, 10) + "...[" + (text.length() - 20) + " chars]..." +
                  text.substring(text.length() - 10);
       }
   }
   ```

3. **Create Structured Logger**
   ```java
   @Component
   public class StructuredLogger {
       private static final Logger logger = LoggerFactory.getLogger(StructuredLogger.class);
       private final SensitiveDataMasker masker;

       public StructuredLogger(SensitiveDataMasker masker) {
           this.masker = masker;
       }

       public void logTranslationRequest(String clientType, String sourceLanguage,
                                       String targetLanguage, String text, long startTime) {
           Map<String, Object> logData = Map.of(
               "event", "translation_request",
               "client_type", clientType,
               "source_language", sourceLanguage,
               "target_language", targetLanguage,
               "text_length", text.length(),
               "text_preview", masker.maskUserText(text),
               "timestamp", Instant.now(),
               "request_id", generateRequestId()
           );

           logger.info("Translation request started", kv("data", logData));
       }

       public void logTranslationResponse(String clientType, boolean success,
                                         long duration, String error) {
           Map<String, Object> logData = new HashMap<>();
           logData.put("event", "translation_response");
           logData.put("client_type", clientType);
           logData.put("success", success);
           logData.put("duration_ms", duration);
           logData.put("timestamp", Instant.now());

           if (error != null) {
               logData.put("error", error);
           }

           if (success) {
               logger.info("Translation completed successfully", kv("data", logData));
           } else {
               logger.error("Translation failed", kv("data", logData));
           }
       }

       public void logConfigurationLoad(String configSource, boolean success, String error) {
           Map<String, Object> logData = Map.of(
               "event", "configuration_load",
               "config_source", configSource,
               "success", success,
               "error", error != null ? error : "",
               "timestamp", Instant.now()
           );

           if (success) {
               logger.info("Configuration loaded", kv("data", logData));
           } else {
               logger.error("Configuration load failed", kv("data", logData));
           }
       }

       private String generateRequestId() {
           return UUID.randomUUID().toString().substring(0, 8);
       }

       // Helper method for key-value pairs
       private Marker kv(String key, Object value) {
           return net.logstash.logback.marker.Markers.append(key, value);
       }
   }
   ```

4. **Create Logback Configuration**
   ```xml
   <configuration>
       <springProfile name="!prod">
           <!-- Development: Human-readable console logging -->
           <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
               <encoder>
                   <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
               </encoder>
           </appender>

           <logger name="de.vkoop" level="DEBUG"/>
           <root level="INFO">
               <appender-ref ref="CONSOLE"/>
           </root>
       </springProfile>

       <springProfile name="prod">
           <!-- Production: Structured JSON logging -->
           <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
               <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
                   <providers>
                       <timestamp/>
                       <logLevel/>
                       <loggerName/>
                       <message/>
                       <mdc/>
                       <arguments/>
                   </providers>
               </encoder>
           </appender>

           <logger name="de.vkoop" level="INFO"/>
           <root level="WARN">
               <appender-ref ref="CONSOLE"/>
           </root>
       </springProfile>
   </configuration>
   ```

5. **Update Client Classes**
   ```java
   @Component("deeplClient")
   public class DeeplTranslateClient implements TranslateClient {
       private static final Logger logger = LoggerFactory.getLogger(DeeplTranslateClient.class);

       @Autowired
       private StructuredLogger structuredLogger;

       @Override
       public Response translate(String text, String sourceLanguage, String targetLanguage) {
           long startTime = System.currentTimeMillis();

           structuredLogger.logTranslationRequest("deepl", sourceLanguage, targetLanguage, text, startTime);

           try {
               // ... translation logic

               long duration = System.currentTimeMillis() - startTime;
               structuredLogger.logTranslationResponse("deepl", true, duration, null);

               return response;

           } catch (Exception e) {
               long duration = System.currentTimeMillis() - startTime;
               structuredLogger.logTranslationResponse("deepl", false, duration, e.getMessage());
               throw new TranslationException("Translation failed", e);
           }
       }

       public void setAuthKey(String authKey) {
           this.authKey = authKey;
           logger.debug("Auth key configured: {}", masker.maskSensitiveData(authKey));
       }
   }
   ```

6. **Add Performance Monitoring**
   ```java
   @Component
   public class PerformanceLogger {
       private static final Logger logger = LoggerFactory.getLogger(PerformanceLogger.class);

       @EventListener
       public void handleTranslationEvent(TranslationEvent event) {
           Map<String, Object> metrics = Map.of(
               "event", "performance_metric",
               "operation", event.getOperation(),
               "duration_ms", event.getDuration(),
               "success", event.isSuccess(),
               "client_type", event.getClientType(),
               "timestamp", Instant.now()
           );

           logger.info("Performance metric", kv("metrics", metrics));
       }
   }
   ```

### Configuration Properties

```properties
# Logging configuration
logging.level.de.vkoop=DEBUG
logging.level.de.vkoop.clients=INFO
logging.structured.enabled=true
logging.sensitive-masking.enabled=true

# Performance logging
logging.performance.enabled=true
logging.performance.threshold-ms=1000

# Log file configuration (optional)
logging.file.name=logs/transcli.log
logging.file.max-size=10MB
logging.file.max-history=30
```

### MDC (Mapped Diagnostic Context) Integration

```java
public class RequestContextFilter {
    public static void setRequestId(String requestId) {
        MDC.put("requestId", requestId);
    }

    public static void clearContext() {
        MDC.clear();
    }
}
```

### Benefits

- **Security**: Sensitive data properly masked in logs
- **Observability**: Structured logs enable better monitoring
- **Debugging**: Rich context for troubleshooting
- **Performance**: Built-in performance monitoring
- **Production Ready**: JSON logging for log aggregation systems

### Monitoring Integration

For production environments:
```java
// Integration with metrics libraries
@Component
public class MetricsLogger {
    private final MeterRegistry meterRegistry;

    public void recordTranslation(String clientType, boolean success, long duration) {
        Timer.Sample sample = Timer.start(meterRegistry);
        sample.stop(Timer.builder("translation.duration")
            .tag("client", clientType)
            .tag("success", String.valueOf(success))
            .register(meterRegistry));
    }
}
```

### Testing Strategy

```java
@Test
void shouldMaskSensitiveData() {
    String masked = masker.maskSensitiveData("secretkey123456789");
    assertEquals("secr***MASKED***6789", masked);
}

@Test
void shouldLogTranslationEvents() {
    // Use test appender to verify log output
    structuredLogger.logTranslationRequest("test", "EN", "DE", "Hello", System.currentTimeMillis());
    // Verify log structure and content
}
```

### Dependencies

- Independent task
- Enhances all other tasks with better observability
- Can be implemented in parallel with other improvements