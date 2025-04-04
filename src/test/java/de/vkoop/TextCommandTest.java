package de.vkoop;

import de.vkoop.data.Response;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(outContent));
        textCommand = new TextCommand() {
            @Override
            protected void loadConfigFromFile() {
                // Skip authentication check for testing
            }
        };
        textCommand.setTranslateClient(translateClient);
        textCommand.text = TEXT_TO_TRANSLATE;
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
        
        Response response = new Response();
        Response.Translation translation = new Response.Translation();
        translation.text = TRANSLATED_TEXT_1;
        response.translations = Collections.singletonList(translation);
        
        when(translateClient.translate(eq(TEXT_TO_TRANSLATE), eq(SOURCE_LANGUAGE), eq(TARGET_LANGUAGE_1)))
                .thenReturn(response);

        // Act
        textCommand.run();

        // Assert
        assertEquals("\"" + TRANSLATED_TEXT_1 + "\"", outContent.toString().trim());
    }

    @Test
    void run_shouldTranslateTextToMultipleLanguages() {
        // Arrange
        textCommand.targetLanguages = Arrays.asList(TARGET_LANGUAGE_1, TARGET_LANGUAGE_2);
        
        Response response1 = new Response();
        Response.Translation translation1 = new Response.Translation();
        translation1.text = TRANSLATED_TEXT_1;
        response1.translations = Collections.singletonList(translation1);
        
        Response response2 = new Response();
        Response.Translation translation2 = new Response.Translation();
        translation2.text = TRANSLATED_TEXT_2;
        response2.translations = Collections.singletonList(translation2);
        
        when(translateClient.translate(eq(TEXT_TO_TRANSLATE), eq(SOURCE_LANGUAGE), eq(TARGET_LANGUAGE_1)))
                .thenReturn(response1);
        when(translateClient.translate(eq(TEXT_TO_TRANSLATE), eq(SOURCE_LANGUAGE), eq(TARGET_LANGUAGE_2)))
                .thenReturn(response2);

        // Act
        textCommand.run();

        // Assert
        String output = outContent.toString().trim();
        assertTrue(output.contains("\"" + TRANSLATED_TEXT_1 + "\""));
        assertTrue(output.contains("\"" + TRANSLATED_TEXT_2 + "\""));
        assertTrue(output.contains(";"));
    }

    @Test
    void run_shouldHandleNullResponse() {
        // Arrange
        textCommand.targetLanguages = Collections.singletonList(TARGET_LANGUAGE_1);
        
        when(translateClient.translate(anyString(), anyString(), anyString()))
                .thenReturn(null);

        // Act
        textCommand.run();

        // Assert
        assertEquals("", outContent.toString().trim());
    }

    @Test
    void run_shouldHandleEmptyTranslations() {
        // Arrange
        textCommand.targetLanguages = Collections.singletonList(TARGET_LANGUAGE_1);
        
        Response response = new Response();
        response.translations = Collections.emptyList();
        
        when(translateClient.translate(anyString(), anyString(), anyString()))
                .thenReturn(response);

        // Act
        textCommand.run();

        // Assert
        assertEquals("\"\"", outContent.toString().trim());
    }
} 