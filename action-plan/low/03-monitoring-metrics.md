# Task: Add Application Monitoring and Metrics

**Priority**: Low
**Estimated Time**: 2-3 hours
**Agent Type**: general-purpose

## Objective

Implement comprehensive application monitoring with metrics collection, health checks, and observability features using Spring Boot Actuator and Micrometer.

## Context

Currently the application lacks:
- Health monitoring capabilities
- Performance metrics collection
- API usage tracking
- System resource monitoring
- Operational insights

## Acceptance Criteria

- [ ] Health check endpoints for application and dependencies
- [ ] Translation performance metrics
- [ ] API usage statistics
- [ ] System resource monitoring
- [ ] Custom business metrics
- [ ] Integration with monitoring systems (Prometheus/Grafana ready)

## Implementation Notes

### Files to Create

- `src/main/java/de/vkoop/monitoring/TranslationMetrics.java`
- `src/main/java/de/vkoop/monitoring/CustomHealthIndicator.java`
- `src/main/java/de/vkoop/monitoring/MetricsEventListener.java`

### Dependencies to Add

```gradle
// Spring Boot Actuator for health checks and metrics
implementation 'org.springframework.boot:spring-boot-starter-actuator'

// Micrometer for metrics collection
implementation 'io.micrometer:micrometer-registry-prometheus'
implementation 'io.micrometer:micrometer-core'

// Optional: For advanced monitoring
implementation 'io.micrometer:micrometer-registry-influx'  // InfluxDB
implementation 'io.micrometer:micrometer-registry-jmx'     // JMX
```

### Implementation Steps

1. **Configure Actuator Endpoints**
   ```properties
   # application.properties

   # Actuator configuration
   management.endpoints.web.exposure.include=health,info,metrics,prometheus
   management.endpoint.health.show-details=when_authorized
   management.endpoint.health.show-components=always
   management.metrics.export.prometheus.enabled=true

   # Custom application info
   info.app.name=@project.name@
   info.app.version=@project.version@
   info.app.description=Translation CLI application
   ```

2. **Create Translation Metrics Service**
   ```java
   @Component
   public class TranslationMetrics {
       private final MeterRegistry meterRegistry;
       private final Counter translationRequests;
       private final Counter translationErrors;
       private final Timer translationDuration;
       private final Gauge activeTranslations;

       private final AtomicLong activeTranslationCount = new AtomicLong(0);

       public TranslationMetrics(MeterRegistry meterRegistry) {
           this.meterRegistry = meterRegistry;

           this.translationRequests = Counter.builder("translation.requests")
               .description("Total number of translation requests")
               .tag("type", "total")
               .register(meterRegistry);

           this.translationErrors = Counter.builder("translation.errors")
               .description("Total number of translation errors")
               .register(meterRegistry);

           this.translationDuration = Timer.builder("translation.duration")
               .description("Translation request duration")
               .register(meterRegistry);

           this.activeTranslations = Gauge.builder("translation.active")
               .description("Number of active translation requests")
               .register(meterRegistry, this, TranslationMetrics::getActiveTranslations);
       }

       public Timer.Sample startTranslation(String clientType, String sourceLanguage, String targetLanguage) {
           activeTranslationCount.incrementAndGet();

           translationRequests.increment(
               Tags.of(
                   "client", clientType,
                   "source_language", sourceLanguage,
                   "target_language", targetLanguage
               )
           );

           return Timer.start(meterRegistry);
       }

       public void recordSuccess(Timer.Sample sample, String clientType) {
           activeTranslationCount.decrementAndGet();
           sample.stop(Timer.builder("translation.duration")
               .tag("client", clientType)
               .tag("status", "success")
               .register(meterRegistry));
       }

       public void recordError(Timer.Sample sample, String clientType, String errorType) {
           activeTranslationCount.decrementAndGet();

           translationErrors.increment(
               Tags.of(
                   "client", clientType,
                   "error_type", errorType
               )
           );

           sample.stop(Timer.builder("translation.duration")
               .tag("client", clientType)
               .tag("status", "error")
               .register(meterRegistry));
       }

       private double getActiveTranslations() {
           return activeTranslationCount.get();
       }
   }
   ```

3. **Create Custom Health Indicators**
   ```java
   @Component
   public class TranslationClientsHealthIndicator implements HealthIndicator {
       private final List<TranslateClient> translateClients;

       public TranslationClientsHealthIndicator(List<TranslateClient> translateClients) {
           this.translateClients = translateClients;
       }

       @Override
       public Health health() {
           Health.Builder builder = new Health.Builder();

           Map<String, Object> details = new HashMap<>();
           boolean allHealthy = true;

           for (TranslateClient client : translateClients) {
               try {
                   boolean isAvailable = client.isAvailable();
                   details.put(getClientName(client), isAvailable ? "UP" : "DOWN");

                   if (!isAvailable) {
                       allHealthy = false;
                   }
               } catch (Exception e) {
                   details.put(getClientName(client), "ERROR: " + e.getMessage());
                   allHealthy = false;
               }
           }

           return allHealthy ?
               builder.up().withDetails(details).build() :
               builder.down().withDetails(details).build();
       }

       private String getClientName(TranslateClient client) {
           return client.getClass().getSimpleName().toLowerCase().replace("translateclient", "");
       }
   }

   @Component
   public class ExternalApiHealthIndicator implements HealthIndicator {
       private final DeeplTranslateClient deeplClient;

       @Override
       public Health health() {
           try {
               // Perform lightweight health check to DeepL API
               // This could be a simple connectivity test
               boolean isHealthy = checkDeeplApiHealth();

               return isHealthy ?
                   Health.up()
                       .withDetail("api", "deepl")
                       .withDetail("status", "accessible")
                       .build() :
                   Health.down()
                       .withDetail("api", "deepl")
                       .withDetail("status", "inaccessible")
                       .build();

           } catch (Exception e) {
               return Health.down()
                   .withDetail("api", "deepl")
                   .withDetail("error", e.getMessage())
                   .build();
           }
       }

       private boolean checkDeeplApiHealth() {
           // Implement lightweight health check
           // Could use HTTP HEAD request or simple ping
           return true; // Simplified for example
       }
   }
   ```

4. **Create Metrics Event Listener**
   ```java
   @Component
   public class MetricsEventListener {
       private final TranslationMetrics translationMetrics;

       public MetricsEventListener(TranslationMetrics translationMetrics) {
           this.translationMetrics = translationMetrics;
       }

       @EventListener
       public void handleTranslationStartEvent(TranslationStartEvent event) {
           // Event would be published by translation clients
           translationMetrics.startTranslation(
               event.getClientType(),
               event.getSourceLanguage(),
               event.getTargetLanguage()
           );
       }

       @EventListener
       public void handleTranslationCompleteEvent(TranslationCompleteEvent event) {
           if (event.isSuccess()) {
               translationMetrics.recordSuccess(event.getTimerSample(), event.getClientType());
           } else {
               translationMetrics.recordError(
                   event.getTimerSample(),
                   event.getClientType(),
                   event.getErrorType()
               );
           }
       }
   }
   ```

5. **Update Translation Clients with Events**
   ```java
   @Component("deeplClient")
   public class DeeplTranslateClient implements TranslateClient {
       private final ApplicationEventPublisher eventPublisher;

       @Override
       public Response translate(String text, String sourceLanguage, String targetLanguage) {
           Timer.Sample sample = Timer.start();

           // Publish start event
           eventPublisher.publishEvent(new TranslationStartEvent(
               "deepl", sourceLanguage, targetLanguage, text.length()
           ));

           try {
               Response response = performTranslation(text, sourceLanguage, targetLanguage);

               // Publish success event
               eventPublisher.publishEvent(new TranslationCompleteEvent(
                   sample, "deepl", true, null
               ));

               return response;

           } catch (Exception e) {
               // Publish error event
               eventPublisher.publishEvent(new TranslationCompleteEvent(
                   sample, "deepl", false, classifyError(e)
               ));
               throw e;
           }
       }

       private String classifyError(Exception e) {
           if (e instanceof IOException) return "network_error";
           if (e instanceof InterruptedException) return "timeout";
           if (e instanceof TranslationException) return "api_error";
           return "unknown_error";
       }
   }
   ```

6. **Create Custom Business Metrics**
   ```java
   @Component
   public class BusinessMetrics {
       private final MeterRegistry meterRegistry;

       public BusinessMetrics(MeterRegistry meterRegistry) {
           this.meterRegistry = meterRegistry;

           // Track text length distribution
           DistributionSummary.builder("translation.text.length")
               .description("Distribution of translated text lengths")
               .register(meterRegistry);

           // Track language pair popularity
           Counter.builder("translation.language.pairs")
               .description("Popular translation language pairs")
               .register(meterRegistry);

           // Track processing rate
           Gauge.builder("translation.rate")
               .description("Translations per minute")
               .register(meterRegistry, this, BusinessMetrics::getTranslationRate);
       }

       public void recordTextLength(int length, String clientType) {
           DistributionSummary.builder("translation.text.length")
               .tag("client", clientType)
               .register(meterRegistry)
               .record(length);
       }

       public void recordLanguagePair(String sourceLanguage, String targetLanguage) {
           Counter.builder("translation.language.pairs")
               .tag("source", sourceLanguage)
               .tag("target", targetLanguage)
               .register(meterRegistry)
               .increment();
       }

       private double getTranslationRate() {
           // Calculate translations per minute from recent metrics
           // This is a simplified example
           return 0.0;
       }
   }
   ```

### Monitoring Dashboard Configuration

For Prometheus + Grafana:

```yaml
# docker-compose.yml for monitoring stack
version: '3.8'
services:
  prometheus:
    image: prom/prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml

  grafana:
    image: grafana/grafana
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
```

```yaml
# prometheus.yml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'transcli'
    static_configs:
      - targets: ['host.docker.internal:8080']
    metrics_path: '/actuator/prometheus'
```

### Alerting Rules

```yaml
# alert-rules.yml
groups:
  - name: transcli
    rules:
      - alert: HighErrorRate
        expr: rate(translation_errors_total[5m]) > 0.1
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "High translation error rate"

      - alert: TranslationServiceDown
        expr: up{job="transcli"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Translation service is down"
```

### Available Endpoints

After implementation:
- `GET /actuator/health` - Application health status
- `GET /actuator/metrics` - Available metrics
- `GET /actuator/metrics/{metric-name}` - Specific metric
- `GET /actuator/prometheus` - Prometheus metrics format
- `GET /actuator/info` - Application information

### Testing Strategy

```java
@Test
void shouldRecordTranslationMetrics() {
    // Test metrics recording
    translationMetrics.startTranslation("deepl", "EN", "DE");
    Timer.Sample sample = Timer.start();
    translationMetrics.recordSuccess(sample, "deepl");

    // Verify metric values
}

@Test
void shouldReportHealthStatus() {
    Health health = healthIndicator.health();
    assertEquals(Status.UP, health.getStatus());
}
```

### Benefits

- **Observability**: Complete visibility into application performance
- **Alerting**: Proactive issue detection
- **Capacity Planning**: Usage patterns and resource requirements
- **Debugging**: Performance bottleneck identification
- **Business Insights**: Translation usage analytics

### Dependencies

- Independent task that enhances all other components
- Works well with structured logging (Task 02)
- Can be implemented in parallel with other improvements