package de.vkoop;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
public class BaseCommandTest {

    private static final String AUTH_KEY = "test-auth-key";
    private static final String SOURCE_LANGUAGE = "DE";
    private static final String TARGET_LANGUAGES = "EN,FR";

    @TempDir
    Path tempDir;

    private TestCommand testCommand;
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();

    @BeforeEach
    void setUp() {
        System.setErr(new PrintStream(errContent));
        testCommand = new TestCommand();
    }

    @Test
    void loadConfigFromFile_shouldLoadConfigFromSpecifiedFile() throws IOException {
        // Arrange
        testCommand.configurationFile = createConfigFile();

        // Act
        testCommand.loadConfigFromFile();

        // Assert
        assertEquals(AUTH_KEY, testCommand.authKey);
        assertEquals(SOURCE_LANGUAGE, testCommand.sourceLanguage);
        assertEquals(Arrays.asList("EN", "FR"), testCommand.targetLanguages);
    }

    @Test
    void validateLanguages_shouldAcceptValidLanguages() {
        // Arrange
        testCommand.sourceLanguage = "DE";
        testCommand.targetLanguages = Arrays.asList("EN", "FR");

        // Act & Assert (no exception should be thrown)
        testCommand.validateLanguages();
    }

    @Test
    void getTranslateClient_shouldCreateClientWithAuthKey() {
        // Arrange
        testCommand.authKey = AUTH_KEY;

        // Act
        TranslateClient client = testCommand.getTranslateClient();

        // Assert
        assertNotNull(client);
    }

    private File createConfigFile() throws IOException {
        File configFile = tempDir.resolve("config.properties").toFile();
        Properties properties = new Properties();
        properties.setProperty("authKey", AUTH_KEY);
        properties.setProperty("sourceLanguage", SOURCE_LANGUAGE);
        properties.setProperty("targetLanguages", TARGET_LANGUAGES);
        
        try (FileOutputStream fos = new FileOutputStream(configFile)) {
            properties.store(fos, "Test Configuration");
        }
        
        return configFile;
    }

    // Test implementation of BaseCommand
    private static class TestCommand extends BaseCommand {
        @Override
        public void run() {
            // Test implementation
        }
    }
} 