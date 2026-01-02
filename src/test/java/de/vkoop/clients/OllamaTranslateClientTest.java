package de.vkoop.clients;

import de.vkoop.data.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.core.io.Resource;

import java.util.Set;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OllamaTranslateClientTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClientRequestSpec requestSpec;

    @Mock
    private CallResponseSpec callSpec;

    @Mock
    private Resource translationPrompt;

    private OllamaTranslateClient client;

    @BeforeEach
    void setUp() {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        client = new OllamaTranslateClient(chatClientBuilder, translationPrompt);
    }

    @Test
    @SuppressWarnings("unchecked")
    void translate_shouldCallChatClientAndReturnResponse() {
        // Arrange
        String sourceLang = "EN";
        String targetLang = "DE";
        String text = "Hello";
        String translatedText = "Hallo";

        when(chatClient.prompt()).thenReturn(requestSpec);
        // Use typed ArgumentMatchers to avoid unchecked cast warnings
        when(requestSpec.system(any(Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn(translatedText);

        // Act
        Response response = client.translate(text, sourceLang, targetLang);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.translations.size());
        assertEquals(translatedText, response.translations.get(0).text);
        assertEquals(sourceLang, response.translations.get(0).detectedSourceLanguage);
    }

    @Test
    void getSupportedLanguages_shouldReturnNonEmptySet() {
        Set<String> sourceLangs = client.getSupportedSourceLanguages();
        Set<String> targetLangs = client.getSupportedTargetLanguages();

        assertNotNull(sourceLangs);
        assertNotNull(targetLangs);
        assertEquals(sourceLangs, targetLangs);
    }
}
