package de.vkoop.config;

import de.vkoop.interfaces.TranslateClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuration class for translation clients
 */
@Configuration
public class TranslationClientConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(TranslationClientConfig.class);
    
    public static final String DEEPL_CLIENT = "deeplClient";
    public static final String OLLAMA_CLIENT = "ollamaClient";
    
    /**
     * Default client type to use if not specified
     */
    public static final String DEFAULT_CLIENT = DEEPL_CLIENT;
    
    /**
     * Returns the appropriate translation client based on the client type
     * 
     * @param clientType The client type to use (deepl or ollama)
     * @param deeplClient The DeepL client implementation
     * @param ollamaClient The Ollama client implementation
     * @return The selected translation client
     */
    @Bean
    @Primary
    public TranslateClient translateClient(
            @Qualifier("clientType") String clientType,
            @Qualifier(DEEPL_CLIENT) TranslateClient deeplClient,
            @Qualifier(OLLAMA_CLIENT) TranslateClient ollamaClient) {
        
        logger.info("Using translation client: {}", clientType);
        
        return switch (clientType.toLowerCase()) {
            case "deepl" -> deeplClient;
            case "ollama" -> ollamaClient;
            default -> {
                logger.warn("Unknown client type: {}. Using default client: {}", clientType, DEFAULT_CLIENT);
                yield DEEPL_CLIENT.equals(DEFAULT_CLIENT) ? deeplClient : ollamaClient;
            }
        };
    }
    
    /**
     * Provides the client type bean with a default value
     * This will be overridden if specified via command line
     * 
     * @return The default client type
     */
    @Bean
    @Qualifier("clientType")
    public String clientType() {
        return DEFAULT_CLIENT.equals(DEEPL_CLIENT) ? "deepl" : "ollama";
    }
}
