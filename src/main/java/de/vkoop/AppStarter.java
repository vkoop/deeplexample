package de.vkoop;

import picocli.CommandLine;

public class AppStarter {
    public static void main(String[] args) {
        new CommandLine(new TranslationTask()).execute(args);
    }
}
