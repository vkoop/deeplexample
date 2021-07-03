package de.vkoop;

import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

public class TranslationTask implements Callable<Integer> {

    @Option(names = "-k")
    String authKey;

    @Option(names = "-s")
    String sourceLanguage;

    @Option(names = "-t", split = ",")
    List<String> targetLanguages;

    @Parameters(index = "0")
    String text;

    private TranslateClient translateClient;

    @Override
    public Integer call() {
        if (!TranslateClient.SUPPORTED_SOURCE_LANGUAGES.contains(sourceLanguage)) {
            System.out.println("Unsupported source language: " + sourceLanguage);
        }

        for (String targetLanguage : targetLanguages) {
            if (!TranslateClient.SUPPORTED_TARGET_LANGUAGES.contains(targetLanguage)) {
                System.out.println("Unsupported target language: " + targetLanguage);
                System.exit(1);
            }
        }

        var translatedCsvLine = targetLanguages.stream()
                .parallel()
                .map(targetLanguage -> getTranslateClient().translate(text, sourceLanguage, targetLanguage))
                .filter(Objects::nonNull)
                .map(response -> response.translations)
                .filter(translations -> !translations.isEmpty())
                .map(map -> map.get(0).text)
                .map(translation -> "\"" + translation + "\"")
                .collect(Collectors.joining(";"));

        System.out.println(translatedCsvLine);

        return 0;
    }

    public TranslateClient getTranslateClient() {
        if(null == translateClient){
            translateClient = new TranslateClient(authKey);
        }

        return translateClient;
    }

    public void setTranslateClient(TranslateClient translateClient) {
        this.translateClient = translateClient;
    }
}
