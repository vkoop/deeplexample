package de.vkoop;

import java.io.IOException;
import java.net.URISyntaxException;

public class App {
    public static void main(String[] args) throws URISyntaxException, IOException, InterruptedException {
        if(args.length != 2){
            System.out.println("Cli usage: <AUTH_TOKEN> <TEXT>");
            System.exit(1);
        }

        final String authKey = args[0];
        final String text = args[1];
        final TranslateClient translateClient = new TranslateClient(authKey);

        final TranslateClient.Response response = translateClient.translate(text, TranslateClient.SourceLanguages.DE, TranslateClient.TargetLanguages.EN_US);
        System.out.println(response.translations.get(0).text);
    }
}
