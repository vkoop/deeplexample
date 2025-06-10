package de.vkoop.commands;

import de.vkoop.interfaces.TranslateClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class BaseCommandTest {

    private static final String AUTH_KEY = "test-auth-key";
    private static final String SOURCE_LANGUAGE = "DE";
    private static final String TARGET_LANGUAGES = "EN,FR";

    @Mock
    private TranslateClient translateClient;

    @TempDir
    Path tempDir;

    private TestCommand testCommand;
    private final ByteArrayOutputStream errContent =
        new ByteArrayOutputStream();

    @BeforeEach
    void setUp() {
        System.setErr(new PrintStream(errContent));
        testCommand = new TestCommand();
        testCommand.setTranslateClient(translateClient);
    }

    @Test
    void loadConfigFromFile_shouldLoadConfigFromSpecifiedFile()
        throws IOException {
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
        
        // Mock supported languages - needed for validateLanguages
        Set<String> supportedSourceLanguages = new HashSet<>();
        supportedSourceLanguages.add(SOURCE_LANGUAGE);
        supportedSourceLanguages.add("EN");
        supportedSourceLanguages.add("FR");
        
        Set<String> supportedTargetLanguages = new HashSet<>();
        supportedTargetLanguages.add("EN");
        supportedTargetLanguages.add("DE");
        supportedTargetLanguages.add("FR");
        
        // Using lenient() to avoid UnnecessaryStubbingException
        when(translateClient.getSupportedSourceLanguages()).thenReturn(supportedSourceLanguages);
        when(translateClient.getSupportedTargetLanguages()).thenReturn(supportedTargetLanguages);

        // Act & Assert (no exception should be thrown)
        testCommand.validateLanguages();
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
