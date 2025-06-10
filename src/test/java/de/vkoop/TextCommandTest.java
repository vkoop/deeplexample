package de.vkoop;

import de.vkoop.commands.TextCommand;
import de.vkoop.data.Response;
import de.vkoop.interfaces.TranslateClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TextCommandTest {

    private static final String SOURCE_LANGUAGE = "DE";
    private static final String TARGET_LANGUAGE_1 = "EN";
    private static final String TARGET_LANGUAGE_2 = "FR";
    private static final String TEXT_TO_TRANSLATE = "Hallo Welt";
    private static final String TRANSLATED_TEXT_1 = "Hello World";
    private static final String TRANSLATED_TEXT_2 = "Bonjour le Monde";

    @Mock
    private TranslateClient translateClient;

    private TextCommand textCommand;
    private final ByteArrayOutputStream outContent =
        new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(outContent));
        
        // Mock supported languages
        Set<String> supportedSourceLanguages = new HashSet<>();
        supportedSourceLanguages.add(SOURCE_LANGUAGE);
        supportedSourceLanguages.add(TARGET_LANGUAGE_1);
        supportedSourceLanguages.add(TARGET_LANGUAGE_2);
        
        Set<String> supportedTargetLanguages = new HashSet<>();
        supportedTargetLanguages.add(TARGET_LANGUAGE_1);
        supportedTargetLanguages.add(TARGET_LANGUAGE_2);
        supportedTargetLanguages.add(SOURCE_LANGUAGE);
        
        when(translateClient.getSupportedSourceLanguages()).thenReturn(supportedSourceLanguages);
        when(translateClient.getSupportedTargetLanguages()).thenReturn(supportedTargetLanguages);
        
        textCommand = new TextCommand() {
            @Override
            protected void loadConfigFromFile() {
                // Do nothing in tests
            }
        };
        textCommand.setTranslateClient(translateClient);
        textCommand.authKey = "test-auth-key";
        textCommand.sourceLanguage = SOURCE_LANGUAGE;
    }

    @AfterEach
    void restoreStreams() {
        System.setOut(originalOut);
    }

    @Test
    void run_shouldTranslateTextToSingleLanguage() {
        // Arrange
        textCommand.targetLanguages = Collections.singletonList(TARGET_LANGUAGE_1);
        textCommand.text = TEXT_TO_TRANSLATE;

        Response response = new Response();
        Response.Translation translation = new Response.Translation();
        translation.text = TRANSLATED_TEXT_1;
        response.translations = Collections.singletonList(translation);

        when(
            translateClient.translate(
                eq(TEXT_TO_TRANSLATE),
                eq(SOURCE_LANGUAGE),
                eq(TARGET_LANGUAGE_1)
            )
        ).thenReturn(response);

        // Act
        textCommand.run();

        // Assert
        assertTrue(outContent.toString().contains(TRANSLATED_TEXT_1));
    }

    @Test
    void run_shouldTranslateTextToMultipleLanguages() {
        // Arrange
        textCommand.targetLanguages = Arrays.asList(
            TARGET_LANGUAGE_1,
            TARGET_LANGUAGE_2
        );
        textCommand.text = TEXT_TO_TRANSLATE;

        Response response1 = new Response();
        Response.Translation translation1 = new Response.Translation();
        translation1.text = TRANSLATED_TEXT_1;
        response1.translations = Collections.singletonList(translation1);

        Response response2 = new Response();
        Response.Translation translation2 = new Response.Translation();
        translation2.text = TRANSLATED_TEXT_2;
        response2.translations = Collections.singletonList(translation2);

        when(
            translateClient.translate(
                eq(TEXT_TO_TRANSLATE),
                eq(SOURCE_LANGUAGE),
                eq(TARGET_LANGUAGE_1)
            )
        ).thenReturn(response1);

        when(
            translateClient.translate(
                eq(TEXT_TO_TRANSLATE),
                eq(SOURCE_LANGUAGE),
                eq(TARGET_LANGUAGE_2)
            )
        ).thenReturn(response2);

        // Act
        textCommand.run();

        // Assert
        assertTrue(outContent.toString().contains(TRANSLATED_TEXT_1));
        assertTrue(outContent.toString().contains(TRANSLATED_TEXT_2));
    }

    @Test
    void run_shouldHandleNullResponse() {
        // Arrange
        textCommand.targetLanguages = Collections.singletonList(TARGET_LANGUAGE_1);
        textCommand.text = TEXT_TO_TRANSLATE;

        when(
            translateClient.translate(
                anyString(),
                anyString(),
                anyString()
            )
        ).thenReturn(null);

        // Act
        textCommand.run();

        // Assert
        // Output could be empty or just contain quotes and/or whitespace
        String output = outContent.toString();
        assertTrue(output.isEmpty() || output.trim().equals("\"\"") || output.trim().isEmpty(),
                "Expected empty or quotes-only output but got: '" + output + "'");
    }

    @Test
    void run_shouldHandleEmptyTranslations() {
        // Arrange
        textCommand.targetLanguages = Collections.singletonList(TARGET_LANGUAGE_1);
        textCommand.text = TEXT_TO_TRANSLATE;

        Response response = new Response();
        response.translations = Collections.emptyList();

        when(
            translateClient.translate(
                anyString(),
                anyString(),
                anyString()
            )
        ).thenReturn(response);

        // Act
        textCommand.run();

        // Assert
        // Output could be empty or just contain quotes and/or whitespace
        String output = outContent.toString();
        assertTrue(output.isEmpty() || output.trim().equals("\"\"") || output.trim().isEmpty(),
                "Expected empty or quotes-only output but got: '" + output + "'");
    }
}
