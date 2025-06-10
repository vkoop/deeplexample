package de.vkoop;

import de.vkoop.commands.TranslateCommand;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@SpringBootApplication
public class AppStarter {

    public static void main(String[] args) {
        System.exit(SpringApplication.exit(SpringApplication.run(AppStarter.class, args)));
    }

    @Component
    @Command(
        subcommands = { TranslateCommand.class, ConfigGeneratorTask.class },
        name = "deeplclient",
        description = "DeepL translation client",
        mixinStandardHelpOptions = true
    )
    public static class AppCommand implements Runnable, CommandLineRunner, ExitCodeGenerator {
        
        private final CommandLine.IFactory factory;
        private int exitCode;
        
        public AppCommand(CommandLine.IFactory factory) {
            this.factory = factory;
        }
        
        @Override
        public void run() {
            // show help

        }
        
        @Override
        public void run(String... args) {
            exitCode = new CommandLine(this, factory).execute(args);
        }
        
        @Override
        public int getExitCode() {
            return exitCode;
        }
    }
}
