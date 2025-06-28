package de.vkoop.commands;

import de.vkoop.interfaces.TranslateClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import picocli.CommandLine;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

public abstract class BaseCommand implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(BaseCommand.class);
    
    @CommandLine.Option(names = "-c")
    public File configurationFile;

    @CommandLine.Option(names = "-f", description = "load configuration from home. E.g. ~/.transcli.properties")
    public boolean loadConfigFromHome;

    @CommandLine.Option(names = "-k")
    public String authKey;

    @CommandLine.Option(names = "-s")
    public String sourceLanguage;

    @CommandLine.Option(names = "-t", split = ",")
    public List<String> targetLanguages;

    @CommandLine.Option(names = "-n")
    private String translationClientName;

    @Autowired
    protected TranslateClient translateClient;

    protected void validateLanguages() {
        if (!translateClient.getSupportedSourceLanguages().contains(sourceLanguage)) {
            logger.error("Unsupported source language: {}", sourceLanguage);
            System.exit(1);
        }

        for (String targetLanguage : targetLanguages) {
            if (!translateClient.getSupportedTargetLanguages().contains(targetLanguage)) {
                logger.error("Unsupported target language: {}", targetLanguage);
                System.exit(1);
            }
        }
    }

    protected void loadConfigFromFile() {
        if (authKey == null && configurationFile == null && !loadConfigFromHome) {
            logger.error("No authentication provided will exit.");
            System.exit(1);
        } else if (loadConfigFromHome || configurationFile != null) {
            Properties properties = new Properties();
            try (FileInputStream inStream = (loadConfigFromHome)
                    ? new FileInputStream(System.getProperty("user.home") + File.separator + ".transcli.properties")
                    : new FileInputStream(configurationFile)) {

                properties.load(inStream);
                Optional.ofNullable(properties.getProperty("authKey"))
                        .ifPresent(value -> this.authKey = value);
                Optional.ofNullable(properties.getProperty("sourceLanguage"))
                        .ifPresent(value -> this.sourceLanguage = value);
                Optional.ofNullable(properties.getProperty("targetLanguages"))
                        .map(value -> List.of(value.split(",")))
                        .ifPresent(value -> this.targetLanguages = value);

                translateClient.setAuthKey(this.authKey);
            } catch (IOException e) {
                logger.error("Failed to load file: {}", configurationFile);
                System.exit(1);
            }
        }
    }



    public void setTranslateClient(TranslateClient translateClient) {
        this.translateClient = translateClient;
    }
}
