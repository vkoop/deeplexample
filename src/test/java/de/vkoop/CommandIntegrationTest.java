package de.vkoop;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CommandIntegrationTest {

    private static final String AUTH_KEY = "test-auth-key";
    private static final String SOURCE_LANGUAGE = "DE";
    private static final String TARGET_LANGUAGE = "EN";
    private static final String TEXT_TO_TRANSLATE = "Hallo Welt";
    private static final String TRANSLATED_TEXT = "Hello World";

    @TempDir
    Path tempDir;

    @Mock
    private TranslateClient translateClient;

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(outContent));
    }
    
    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    @Test
    void textCommand_shouldTranslateText() {
        // Arrange
        // Create a response with the translated text
        de.vkoop.data.Response response = new de.vkoop.data.Response();
        de.vkoop.data.Response.Translation translation = new de.vkoop.data.Response.Translation();
        translation.text = TRANSLATED_TEXT;
        response.translations = Collections.singletonList(translation);
        
        // Mock the translate client to return the response
        when(translateClient.translate(anyString(), eq(SOURCE_LANGUAGE), eq(TARGET_LANGUAGE)))
            .thenReturn(response);
        
        // Create and configure the text command
        TextCommand textCommand = new TextCommand();
        textCommand.setTranslateClient(translateClient);
        textCommand.authKey = AUTH_KEY;
        textCommand.sourceLanguage = SOURCE_LANGUAGE;
        textCommand.targetLanguages = Collections.singletonList(TARGET_LANGUAGE);
        textCommand.text = TEXT_TO_TRANSLATE;
        
        // Act
        textCommand.run();
        
        // Assert
        assertTrue(outContent.toString().contains(TRANSLATED_TEXT));
    }

    @Test
    void jsonCommand_shouldTranslateJsonFile() throws IOException {
        // Arrange
        // Create a test JSON file
        String jsonContent = "{\"key\":\"value\"}";
        Path jsonFile = tempDir.resolve("test.json");
        Files.writeString(jsonFile, jsonContent);
        
        // Create output directory
        Path outputDir = tempDir.resolve("output");
        Files.createDirectory(outputDir);
        
        // Create a mock response
        de.vkoop.data.Response response = new de.vkoop.data.Response();
        de.vkoop.data.Response.Translation translation = new de.vkoop.data.Response.Translation();
        translation.text = TRANSLATED_TEXT;
        response.translations = Collections.singletonList(translation);
        
        // Mock the translate client to return the response
        when(translateClient.translate(anyString(), eq(SOURCE_LANGUAGE), eq(TARGET_LANGUAGE)))
            .thenReturn(response);
        
        // Create and configure the JSON command
        JsonCommand jsonCommand = new JsonCommand();
        jsonCommand.setTranslateClient(translateClient);
        jsonCommand.authKey = AUTH_KEY;
        jsonCommand.sourceLanguage = SOURCE_LANGUAGE;
        jsonCommand.targetLanguages = Collections.singletonList(TARGET_LANGUAGE);
        jsonCommand.jsonFile = jsonFile.toString();
        jsonCommand.outputFolder = java.util.Optional.of(outputDir.toString());
        
        // Act
        jsonCommand.run();
        
        // Assert
        File outputFile = outputDir.resolve("en.json").toFile();
        assertTrue(outputFile.exists());
    }
} 