package de.vkoop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;

import java.io.Console;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Properties;

@Command(name = "generate-config", description = "Generate .transcli.properties configuration file")
public class ConfigGeneratorTask implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ConfigGeneratorTask.class);
    private final Console console = System.console();

    @Override
    public void run() {
        if (console == null) {
            logger.error("No console available. Please run this command in a terminal.");
            System.exit(1);
        }

        Properties properties = new Properties();
        File configFile = new File(System.getProperty("user.home") + File.separator + ".transcli.properties");

        if (configFile.exists()) {
            String response = promptRequired("Configuration file already exists at: " + configFile.getAbsolutePath() +
                    "\nDo you want to replace it? (y/N): ");
            if (!response.toLowerCase().startsWith("y")) {
                logger.info("Configuration generation cancelled.");
                return;
            }
        }

        logger.info("DeepL Translations CLI Configuration Generator");
        logger.info("-------------------------------------------");
        logger.info("This will create a configuration file at: {}", configFile.getAbsolutePath());
        logger.info("Press Enter to skip optional fields.");

        // Required: API Key
        String authKey = promptRequired("Enter your DeepL API key (required): ");
        properties.setProperty("authKey", authKey);

        // Optional: Source Language
        String sourceLang = promptOptional("Enter default source language (optional, e.g., DE): ");
        if (!sourceLang.isEmpty()) {
            properties.setProperty("sourceLanguage", sourceLang);
        }

        // Optional: Target Languages
        String targetLangs = promptOptional("Enter default target languages (optional, comma-separated, e.g., EN,FR): ");
        if (!targetLangs.isEmpty()) {
            properties.setProperty("targetLanguages", targetLangs);
        }

        try (FileOutputStream out = new FileOutputStream(configFile)) {
            properties.store(out, "DeepL Translations CLI Configuration - Generated on " + LocalDateTime.now());
            logger.info("Configuration file created successfully at: {}", configFile.getAbsolutePath());
            logger.info("Content:");
            logger.info("----------------------------------------");
            properties.forEach((key, value) -> logger.info("{}={}", key, value));
            logger.info("----------------------------------------");
            logger.info("You can now use the CLI with the -f flag to use this configuration.");
        } catch (IOException e) {
            logger.error("Failed to create configuration file: {}", e.getMessage(), e);
            System.exit(1);
        }
    }

    private String promptRequired(String prompt) {
        String input;
        do {
            input = console.readLine(prompt).trim();
            if (input.isEmpty()) {
                logger.warn("This field is required. Please try again.");
            }
        } while (input.isEmpty());
        return input;
    }

    private String promptOptional(String prompt) {
        return console.readLine(prompt).trim();
    }
}