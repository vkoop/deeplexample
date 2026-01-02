package de.vkoop.clients;

import de.vkoop.data.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.ollama.OllamaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(args = "--translate.client=ollama")
@Testcontainers
@Tag("integration")
class OllamaIntegrationTest {

    @Container
    static OllamaContainer ollama = new OllamaContainer("ollama/ollama:0.1.26");

    @Autowired
    private OllamaTranslateClient client;

    @BeforeAll
    static void beforeAll() throws IOException, InterruptedException {
        // We need to pull a model.
        ollama.execInContainer("ollama", "pull", "tinyllama");
    }

    @DynamicPropertySource
    static void registerOllamaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.ollama.baseUrl", ollama::getEndpoint);

        // Spring AI auto-configuration usually defaults to 'mistral' or 'llama2'.
        // We must tell it to use the model we pulled.
        registry.add("spring.ai.ollama.chat.options.model", () -> "tinyllama");
    }

    @Test
    void translate_shouldReturnTranslation_whenOllamaIsRunning() {
        // Arrange
        String text = "Hello";
        String sourceLang = "EN";
        String targetLang = "DE";

        // Act
        Response response = client.translate(text, sourceLang, targetLang);

        // Assert
        assertNotNull(response, "Response should not be null");
        assertFalse(response.translations.isEmpty(), "Translations should not be empty");
        Response.Translation translation = response.translations.get(0);
        assertNotNull(translation.text, "Translated text should not be null");
        assertFalse(translation.text.isEmpty(), "Translated text should not be empty");

        // Note: We don't assert the exact translation content because LLMs are
        // non-deterministic,
        // especially smaller ones like tinyllama. We just verify it returns
        // *something*.
    }
}
