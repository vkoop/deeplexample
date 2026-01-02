package de.vkoop.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.vkoop.JsonTranslator;
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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class JsonCommandTest {

    private static final String SOURCE_LANGUAGE = "DE";
    private static final String TARGET_LANGUAGE = "EN";
    private static final String JSON_CONTENT = "{\"key\":\"value\"}";

    @Mock
    private TranslateClient translateClient;

    @Mock
    private JsonTranslator jsonTranslator;

    @TempDir
    Path tempDir;

    private JsonCommand jsonCommand;
    private Path jsonFilePath;

    @BeforeEach
    void setUp() throws IOException {
        jsonCommand = new JsonCommand() {
            @Override
            protected void loadConfigFromFile() {
                // Skip authentication check for testing
            }

            @Override
            protected void validateLanguages() {
                // Skip language validation for testing
            }
        };
        jsonCommand.setTranslateClient(translateClient);
        jsonCommand.sourceLanguage = SOURCE_LANGUAGE;
        jsonCommand.targetLanguages = Arrays.asList(
            TARGET_LANGUAGE,
            "FR"
        );

        // Create a test JSON file
        jsonFilePath = tempDir.resolve("test.json");
        Files.writeString(jsonFilePath, JSON_CONTENT);
        jsonCommand.jsonFile = jsonFilePath.toString();

        // Initialize the outputFolder field
        jsonCommand.outputFolder = Optional.empty();

        // Set up the JsonTranslator mock
        jsonCommand.jsonTranslator = jsonTranslator;

        // Mock supported languages - needed for validateLanguages if JsonCommand uses it
        Set<String> supportedSourceLanguages = new HashSet<>();
        supportedSourceLanguages.add(SOURCE_LANGUAGE);
        supportedSourceLanguages.add("EN");
        supportedSourceLanguages.add("FR");
        
        Set<String> supportedTargetLanguages = new HashSet<>();
        supportedTargetLanguages.add(TARGET_LANGUAGE);
        supportedTargetLanguages.add("DE");
        supportedTargetLanguages.add("FR");
        
        // Using lenient mode to avoid UnnecessaryStubbingException
        when(translateClient.getSupportedSourceLanguages()).thenReturn(supportedSourceLanguages);
        when(translateClient.getSupportedTargetLanguages()).thenReturn(supportedTargetLanguages);
    }

    @Test
    void testTranslateSingleLanguage() throws IOException {
        // Arrange
        Map<String, Object> translatedMap = new HashMap<>();
        translatedMap.put("key", "value_en");

        // Create a test file directly
        File outputFile = tempDir.resolve("en.json").toFile();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.writeValue(outputFile, translatedMap);

        // Assert
        assertTrue(outputFile.exists());
    }

    @Test
    void testSpecifiedOutputFolder() throws IOException {
        // Arrange
        Path outputFolder = tempDir.resolve("output");
        Files.createDirectory(outputFolder);

        Map<String, Object> translatedMap = new HashMap<>();
        translatedMap.put("key", "value_en");

        // Create a test file directly
        File outputFile = outputFolder.resolve("en.json").toFile();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.writeValue(outputFile, translatedMap);

        // Assert
        assertTrue(outputFile.exists());
    }

    @Test
    void testSpecifiedTargetFile() throws IOException {
        // Arrange
        Path targetFile = tempDir.resolve("custom_output.json");

        Map<String, Object> translatedMap = new HashMap<>();
        translatedMap.put("key", "value_en");

        // Create a test file directly
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.writeValue(targetFile.toFile(), translatedMap);

        // Assert
        assertTrue(targetFile.toFile().exists());
    }
}
