package de.vkoop.clients;

import de.vkoop.data.Response;
import de.vkoop.interfaces.TranslateClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ConditionalOnProperty(name = "translate.client", havingValue = "ollama")
@Component("ollamaClient")
public class OllamaTranslateClient implements TranslateClient {

    private static final Logger logger = LoggerFactory.getLogger(OllamaTranslateClient.class);

    private final ChatClient chatClient;
    private final Resource translationPrompt;

    // Common language codes supported by most LLMs
    private static final Set<String> SUPPORTED_LANGUAGES = new HashSet<>(Arrays.asList(
            "EN", "DE", "FR", "ES", "IT", "NL", "PL", "PT", "RU", "ZH", "JA", "KO",
            "AR", "BG", "CS", "DA", "EL", "ET", "FI", "HU", "ID", "LT", "LV",
            "NO", "RO", "SK", "SL", "SV", "TR", "UK", "VI", "HE", "HI", "TH",
            "CA", "HR", "IS", "MS", "FA", "SR", "BS", "MK", "GA", "SQ", "NB", "PT-BR"));

    public OllamaTranslateClient(ChatClient.Builder chatClientBuilder,
            @Value("classpath:/prompts/translation.st") Resource translationPrompt) {
        this.chatClient = chatClientBuilder.build();
        this.translationPrompt = translationPrompt;
    }

    @Override
    public Response translate(String text, String sourceLanguage, String targetLanguage) {
        if (text == null || text.trim().isEmpty()) {
            logger.warn("Empty text provided for translation");
            return null;
        }

        logger.debug("Translating text from {} to {}", sourceLanguage, targetLanguage);

        try {
            String translatedText = chatClient.prompt()
                    .system(s -> s.text(translationPrompt)
                            .param("sourceLanguage", sourceLanguage)
                            .param("targetLanguage", targetLanguage))
                    .user(text)
                    .call()
                    .content();

            // Create and return Response object
            Response response = new Response();
            Response.Translation translation = new Response.Translation();
            translation.detectedSourceLanguage = sourceLanguage;
            translation.text = translatedText;
            response.translations = List.of(translation);

            logger.debug("Translation successful");
            return response;

        } catch (Exception e) {
            logger.error("Translation failed: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public Set<String> getSupportedSourceLanguages() {
        return SUPPORTED_LANGUAGES;
    }

    @Override
    public Set<String> getSupportedTargetLanguages() {
        return SUPPORTED_LANGUAGES;
    }

    @Override
    public void setAuthKey(String authKey) {
        logger.debug("Auth key set");
        // Note: Ollama typically doesn't require an auth key for local instances,
        // but we implement this method to satisfy the interface
    }
}