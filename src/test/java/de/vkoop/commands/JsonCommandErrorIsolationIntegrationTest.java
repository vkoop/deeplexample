package de.vkoop.commands;

import de.vkoop.JsonTranslator;
import de.vkoop.data.Response;
import de.vkoop.exceptions.TranslationException;
import de.vkoop.interfaces.TranslateClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class JsonCommandErrorIsolationIntegrationTest {

    @Mock
    private TranslateClient translateClient;

    @TempDir
    Path tempDir;

    private JsonCommand jsonCommand;

    @BeforeEach
    void setUp() throws IOException {
        JsonTranslator jsonTranslator = new JsonTranslator(translateClient);
        jsonCommand = new JsonCommand();
        jsonCommand.jsonTranslator = jsonTranslator;
        jsonCommand.setTranslateClient(translateClient);

        // Create test JSON file
        File testJsonFile = tempDir.resolve("test.json").toFile();
        try (FileWriter writer = new FileWriter(testJsonFile)) {
            writer.write("{\"message\": \"Hello World\", \"greeting\": \"Good morning\"}");
        }

        jsonCommand.jsonFile = testJsonFile.getAbsolutePath();
        jsonCommand.outputFolder = java.util.Optional.of(tempDir.toString());
        jsonCommand.authKey = "test-key";
        jsonCommand.sourceLanguage = "EN";
        jsonCommand.targetLanguages = Arrays.asList("DE", "FR", "ES");

        // Mock supported languages
        Set<String> supportedSourceLanguages = new HashSet<>(Set.of("EN"));
        Set<String> supportedTargetLanguages = new HashSet<>(Set.of("DE", "FR", "ES"));
        when(translateClient.getSupportedSourceLanguages()).thenReturn(supportedSourceLanguages);
        when(translateClient.getSupportedTargetLanguages()).thenReturn(supportedTargetLanguages);
    }

    @Test
    void run_shouldContinueProcessingWhenOneLanguageFails() throws IOException {
        // Arrange - mock successful responses for DE and ES, but failure for FR
        Response successResponse = createSuccessResponse("Translated text");

        // DE succeeds
        when(translateClient.translate(eq("Hello World"), eq("EN"), eq("DE")))
                .thenReturn(successResponse);
        when(translateClient.translate(eq("Good morning"), eq("EN"), eq("DE")))
                .thenReturn(successResponse);

        // FR fails (simulate network error)
        when(translateClient.translate(anyString(), eq("EN"), eq("FR")))
                .thenThrow(new RuntimeException("Network timeout"));

        // ES succeeds
        when(translateClient.translate(eq("Hello World"), eq("EN"), eq("ES")))
                .thenReturn(successResponse);
        when(translateClient.translate(eq("Good morning"), eq("EN"), eq("ES")))
                .thenReturn(successResponse);

        // Act - should complete successfully with partial results
        jsonCommand.run();

        // Assert - successful translations should have created files
        File deFile = tempDir.resolve("de.json").toFile();
        File frFile = tempDir.resolve("fr.json").toFile();
        File esFile = tempDir.resolve("es.json").toFile();

        assertTrue(deFile.exists(), "DE translation should have succeeded");
        assertFalse(frFile.exists(), "FR translation should have failed");
        assertTrue(esFile.exists(), "ES translation should have succeeded");

        // Verify file content for successful translations
        assertTrue(deFile.length() > 0, "DE file should not be empty");
        assertTrue(esFile.length() > 0, "ES file should not be empty");
    }

    @Test
    void run_shouldThrowExceptionWhenAllLanguagesFail() {
        // Arrange - all languages fail
        when(translateClient.translate(anyString(), eq("EN"), eq("DE")))
                .thenThrow(new RuntimeException("DE service down"));
        when(translateClient.translate(anyString(), eq("EN"), eq("FR")))
                .thenThrow(new RuntimeException("FR service down"));
        when(translateClient.translate(anyString(), eq("EN"), eq("ES")))
                .thenThrow(new RuntimeException("ES service down"));

        // Act & Assert - should throw exception when all fail
        TranslationException exception = assertThrows(TranslationException.class, () -> {
            jsonCommand.run();
        });

        assertEquals("All translations failed. No output files were generated.", exception.getMessage());

        // Verify no files were created
        File deFile = tempDir.resolve("de.json").toFile();
        File frFile = tempDir.resolve("fr.json").toFile();
        File esFile = tempDir.resolve("es.json").toFile();

        assertFalse(deFile.exists(), "No files should be created when all translations fail");
        assertFalse(frFile.exists(), "No files should be created when all translations fail");
        assertFalse(esFile.exists(), "No files should be created when all translations fail");
    }

    @Test
    void run_shouldSucceedWhenAllLanguagesSucceed() throws IOException {
        // Arrange - all languages succeed
        Response successResponse = createSuccessResponse("Translated text");

        when(translateClient.translate(anyString(), eq("EN"), eq("DE")))
                .thenReturn(successResponse);
        when(translateClient.translate(anyString(), eq("EN"), eq("FR")))
                .thenReturn(successResponse);
        when(translateClient.translate(anyString(), eq("EN"), eq("ES")))
                .thenReturn(successResponse);

        // Act - should complete successfully
        jsonCommand.run();

        // Assert - all files should be created
        File deFile = tempDir.resolve("de.json").toFile();
        File frFile = tempDir.resolve("fr.json").toFile();
        File esFile = tempDir.resolve("es.json").toFile();

        assertTrue(deFile.exists(), "DE translation should have succeeded");
        assertTrue(frFile.exists(), "FR translation should have succeeded");
        assertTrue(esFile.exists(), "ES translation should have succeeded");

        assertTrue(deFile.length() > 0, "DE file should not be empty");
        assertTrue(frFile.length() > 0, "FR file should not be empty");
        assertTrue(esFile.length() > 0, "ES file should not be empty");
    }

    private Response createSuccessResponse(String translatedText) {
        Response response = new Response();
        Response.Translation translation = new Response.Translation();
        translation.text = translatedText;
        translation.detectedSourceLanguage = "EN";
        response.translations = Collections.singletonList(translation);
        return response;
    }
}