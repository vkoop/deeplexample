package de.vkoop.interfaces;

import de.vkoop.data.Response;

import java.util.Set;

/**
 * Interface for translation services
 */
public interface TranslateClient {
    
    /**
     * Translates text from source language to target language
     * 
     * @param text The text to translate
     * @param sourceLanguage The source language code
     * @param targetLanguage The target language code
     * @return A Response object containing the translation, or null if translation failed
     */
    Response translate(String text, String sourceLanguage, String targetLanguage);
    
    /**
     * Returns a set of supported source language codes
     * 
     * @return Set of language codes that can be used as source languages
     */
    Set<String> getSupportedSourceLanguages();
    
    /**
     * Returns a set of supported target language codes
     * 
     * @return Set of language codes that can be used as target languages
     */
    Set<String> getSupportedTargetLanguages();

    void setAuthKey(String authKey);
}
