package de.vkoop;

import picocli.CommandLine;

public class App {
    public static void main(String[] args) {
        int exitCode = new CommandLine(new TranslationTask()).execute(args);
        System.exit(exitCode);
    }
}
