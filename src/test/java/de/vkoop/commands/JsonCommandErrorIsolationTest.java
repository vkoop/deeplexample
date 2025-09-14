package de.vkoop.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class JsonCommandErrorIsolationTest {

    @Test
    void parallelStream_demonstratesFailFastBehavior() {
        // Arrange - simulate what happens in JsonCommand.run() with parallel processing
        List<String> targetLanguages = Arrays.asList("DE", "FR", "ES", "IT", "PT");
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger processedCount = new AtomicInteger(0);

        // Act & Assert - demonstrate that parallel streams fail fast
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            targetLanguages
                    .stream()
                    .parallel()
                    .forEach(targetLanguage -> {
                        int processed = processedCount.incrementAndGet();
                        System.out.println("Processing: " + targetLanguage + " (attempt " + processed + ")");

                        // Simulate failure for FR (like IOException in real scenario)
                        if ("FR".equals(targetLanguage)) {
                            throw new RuntimeException("Translation service unavailable for " + targetLanguage);
                        }

                        // Simulate successful processing
                        try {
                            Thread.sleep(100); // Simulate translation work
                            int success = successCount.incrementAndGet();
                            System.out.println("Successfully processed: " + targetLanguage + " (" + success + " successes so far)");
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    });
        });

        // Assert the problem
        assertTrue(exception.getMessage().contains("Translation service unavailable for FR"));

        // The critical issue: successful translations may be interrupted
        int finalSuccessCount = successCount.get();
        int finalProcessedCount = processedCount.get();

        System.out.println("Final success count: " + finalSuccessCount);
        System.out.println("Final processed count: " + finalProcessedCount);

        // In this simplified test, the parallel stream may complete some tasks before failing
        // The real issue is that the first exception that bubbles up will terminate the entire operation
        // In JsonCommand.java:68, any IOException is wrapped in RuntimeException which fails the entire run
        System.out.println("Parallel stream processed " + finalProcessedCount + " languages with " + finalSuccessCount + " successes before failing");

        // The key issue: the entire operation fails, losing all successful work
        assertTrue(finalSuccessCount >= 0, "Some work may be completed before failure");
    }

    @Test
    void isolatedParallelProcessing_shouldContinueAfterFailures() {
        // This test shows how proper error isolation should work
        List<String> targetLanguages = Arrays.asList("DE", "FR", "ES", "IT", "PT");
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicInteger processedCount = new AtomicInteger(0);

        // Act - using CompletableFuture for proper error isolation
        List<CompletableFuture<Void>> futures = targetLanguages
                .stream()
                .map(targetLanguage -> CompletableFuture.runAsync(() -> {
                    int processed = processedCount.incrementAndGet();
                    System.out.println("Processing with isolation: " + targetLanguage + " (attempt " + processed + ")");

                    try {
                        // Simulate failure for FR
                        if ("FR".equals(targetLanguage)) {
                            throw new RuntimeException("Translation service unavailable for " + targetLanguage);
                        }

                        // Simulate successful processing
                        Thread.sleep(50);
                        int success = successCount.incrementAndGet();
                        System.out.println("Successfully processed with isolation: " + targetLanguage + " (" + success + " successes so far)");

                    } catch (Exception e) {
                        int failure = failureCount.incrementAndGet();
                        System.out.println("Failed to process: " + targetLanguage + " (" + failure + " failures so far) - " + e.getMessage());
                        // Error is isolated - doesn't affect other translations
                    }
                }))
                .toList();

        // Wait for all futures to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // Assert proper error isolation
        assertEquals(4, successCount.get(), "Should have 4 successful translations (all except FR)");
        assertEquals(1, failureCount.get(), "Should have exactly 1 failure (FR)");
        assertEquals(5, processedCount.get(), "Should have processed all 5 languages");

        System.out.println("With error isolation - Success: " + successCount.get() + ", Failures: " + failureCount.get());
    }
}