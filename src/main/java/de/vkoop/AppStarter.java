package de.vkoop;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
    subcommands = {TranslateCommand.class, ConfigGeneratorTask.class},
    name = "deeplclient",
    description = "DeepL translation client"
)
public class AppStarter {
    public static void main(String[] args) {
        int exitCode = new CommandLine(new AppStarter()).execute(args);
        System.exit(exitCode);
    }
}
