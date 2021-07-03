package de.vkoop;

import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

public class TranslationTask implements Callable<Integer> {

    @Option(names = "-c")
    File configurationFile;

    @Option(names = "-k")
    String authKey;

    @Option(names = "-s")
    String sourceLanguage;

    @Option(names = "-t", split = ",")
    List<String> targetLanguages;

    @Parameters
    String text;

    private TranslateClient translateClient;

    @Override
    public Integer call() {
        loadConfigFromFile();

        if (!TranslateClient.SUPPORTED_SOURCE_LANGUAGES.contains(sourceLanguage)) {
            System.err.println("Unsupported source language: " + sourceLanguage);
        }

        for (String targetLanguage : targetLanguages) {
            if (!TranslateClient.SUPPORTED_TARGET_LANGUAGES.contains(targetLanguage)) {
                System.err.println("Unsupported target language: " + targetLanguage);
                System.exit(1);
            }
        }

        var translatedCsvLine = targetLanguages.stream()
                .parallel()
                .map(targetLanguage -> getTranslateClient().translate(text, sourceLanguage, targetLanguage))
                .filter(Objects::nonNull)
                .map(response -> response.translations)
                .map(translations -> translations.isEmpty() ? "" : translations.get(0).text)
                .map(translation -> "\"" + translation + "\"")
                .collect(Collectors.joining(";"));

        System.out.println(translatedCsvLine);

        return 0;
    }

    private void loadConfigFromFile() {
        if (authKey == null && configurationFile == null) {
            System.exit(1);
        } else if (configurationFile != null) {
            Properties properties = new Properties();
            try {
                properties.load(new FileInputStream(configurationFile));
                Optional.ofNullable(properties.getProperty("authKey"))
                        .ifPresent(value -> this.authKey = value);
                Optional.ofNullable(properties.getProperty("sourceLanguage"))
                        .ifPresent(value -> this.sourceLanguage = value);
                Optional.ofNullable(properties.getProperty("targetLanguages"))
                        .map(value -> List.of( value.split(",")))
                        .ifPresent(value -> this.targetLanguages = value);
            } catch (IOException e) {
                System.err.println("Failed to load file: " + configurationFile);
                System.exit(1);
            }
        }
    }

    public TranslateClient getTranslateClient() {
        if (null == translateClient) {
            translateClient = new TranslateClient(authKey);
        }

        return translateClient;
    }

    public void setTranslateClient(TranslateClient translateClient) {
        this.translateClient = translateClient;
    }
}
