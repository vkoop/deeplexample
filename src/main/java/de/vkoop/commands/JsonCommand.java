package de.vkoop.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import de.vkoop.JsonTranslator;
import de.vkoop.exceptions.TranslationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Command(name = "json", description = "Translate JSON files using DeepL API")
public class JsonCommand extends BaseCommand {
    private static final Logger logger = LoggerFactory.getLogger(JsonCommand.class);

    @Option(names = "--json-file", required = true)
    public String jsonFile;

    @Option(names = "--json-target-file")
    public String jsonTargetFile;

    @Option(names = "--output-folder")
    public Optional<String> outputFolder;

    @Autowired
    public JsonTranslator jsonTranslator;

    @Override
    public void run() {
        loadConfigFromFile();
        validateLanguages();

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // Use CompletableFuture for proper error isolation
        List<CompletableFuture<Void>> translationFutures = targetLanguages
                .stream()
                .map(targetLanguage -> CompletableFuture.runAsync(() -> {
                    try {
                        translateSingleLanguage(jsonTranslator, targetLanguage);
                        int successes = successCount.incrementAndGet();
                        logger.info("Successfully translated to {}: {} of {} languages completed",
                                   targetLanguage, successes, targetLanguages.size());
                    } catch (Exception e) {
                        int failures = failureCount.incrementAndGet();
                        logger.error("Failed to translate to {} ({} of {} failed): {}",
                                    targetLanguage, failures, targetLanguages.size(), e.getMessage(), e);
                        // Error is isolated - doesn't affect other translations
                    }
                }))
                .toList();

        // Wait for all translations to complete
        CompletableFuture.allOf(translationFutures.toArray(new CompletableFuture[0])).join();

        int totalSuccesses = successCount.get();
        int totalFailures = failureCount.get();

        logger.info("Translation completed: {} successes, {} failures out of {} total languages",
                   totalSuccesses, totalFailures, targetLanguages.size());

        // Only throw exception if ALL translations failed
        if (totalSuccesses == 0 && totalFailures > 0) {
            throw new TranslationException("All translations failed. No output files were generated.");
        } else if (totalFailures > 0) {
            logger.warn("Some translations failed, but {} files were successfully generated", totalSuccesses);
        }
    }

    private void translateSingleLanguage(
            JsonTranslator jsonParser,
            String targetLanguage
    ) throws IOException {
        final Map<String, Object> stringObjectMap =
                jsonParser.translateJsonFile(
                        jsonFile,
                        sourceLanguage,
                        targetLanguage
                );
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(
                SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS,
                true
        );

        final File resultFile = getFile(targetLanguage);
        objectMapper
                .writerWithDefaultPrettyPrinter()
                .writeValue(resultFile, stringObjectMap);
    }

    private File getFile(String targetLanguage) throws IOException {
        final File resultFile;
        String targetLanguageLowerCase = targetLanguage
                .toLowerCase()
                .replace('-', '_');
        if (outputFolder.isPresent() && jsonTargetFile == null) {
            final File parentFolder = new File(outputFolder.get());
            if (!parentFolder.exists()) {
                parentFolder.mkdirs();
            }
            resultFile = new File(
                    parentFolder,
                    targetLanguageLowerCase + ".json"
            );
        } else {
            resultFile = new File(
                    Objects.requireNonNullElseGet(
                            jsonTargetFile,
                            () -> targetLanguageLowerCase + ".json"
                    )
            );
        }
        resultFile.createNewFile();
        return resultFile;
    }
}
