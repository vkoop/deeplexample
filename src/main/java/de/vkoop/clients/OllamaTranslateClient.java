package de.vkoop.clients;

import de.vkoop.data.Response;
import de.vkoop.interfaces.TranslateClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ConditionalOnProperty(name = "translate.client", havingValue = "ollama")
@Component("ollamaClient")
public class OllamaTranslateClient implements TranslateClient {

    private static final Logger logger = LoggerFactory.getLogger(OllamaTranslateClient.class);
    
    private final ChatModel chatModel;

    // Common language codes supported by most LLMs
    private static final Set<String> SUPPORTED_LANGUAGES = new HashSet<>(Arrays.asList(
        "EN", "DE", "fr", "es", "it", "nl", "pl", "pt", "ru", "zh", "ja", "ko"
    ));

    @Autowired
    public OllamaTranslateClient(OllamaChatModel chatModel) {
        this.chatModel = chatModel;
        logger.info("OllamaTranslateClient initialized");
    }

    @Override
    public Response translate(String text, String sourceLanguage, String targetLanguage) {
        if (text == null || text.trim().isEmpty()) {
            logger.warn("Empty text provided for translation");
            return null;
        }
        
        logger.debug("Translating text from {} to {}", sourceLanguage, targetLanguage);
        
        try {
            // Create system message with translation instructions
            Prompt prompt = getPrompt(text, sourceLanguage, targetLanguage);

            // Get response from Ollama
            ChatResponse chatResponse = chatModel.call(prompt);
            String translatedText = chatResponse.getResult().getOutput().getText();
            
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

    private static Prompt getPrompt(String text, String sourceLanguage, String targetLanguage) {
        SystemMessage systemMessage = new SystemMessage(
                "You are a professional translator. " +
                "Translate the provided text from " + sourceLanguage + " to " + targetLanguage + ". " +
                "Return ONLY the translated text without any explanations, notes, or additional content."
        );

        // Create user message with the text to translate
        UserMessage userMessage = new UserMessage(text);

        // Create prompt with both messages
        return new Prompt(List.of(systemMessage, userMessage));
    }

}