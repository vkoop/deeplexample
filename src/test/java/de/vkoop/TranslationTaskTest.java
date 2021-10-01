package de.vkoop;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import picocli.CommandLine;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class TranslationTaskTest {

    final String authkey = "fakekey";
    String sourceLanguage = "DE";
    List<String> targetLanguages = List.of("EN", "FR");
    String text = "Hallo, wie gehts es dir?";

    String[] defaultArgs = {
            "-k=" + authkey,
            "-s=" + sourceLanguage,
            "-t=" + String.join(",", targetLanguages),
            text
    };

    @Mock
    private TranslateClient client;

    @Test
    public void testParameterBinding() {
        final TranslationTask translationTask = CommandLine.populateCommand(new TranslationTask(), defaultArgs);

        Assertions.assertEquals(authkey, translationTask.authKey);
        Assertions.assertEquals(text, translationTask.text);
        Assertions.assertEquals(sourceLanguage, translationTask.sourceLanguage);
        Assertions.assertEquals(targetLanguages, translationTask.targetLanguages);
    }

    @Test
    public void integrationTest(){
        final TranslationTask translationTask = CommandLine.populateCommand(new TranslationTask(), defaultArgs);
        translationTask.setTranslateClient(client);
        translationTask.run();

        Mockito.verify(client).translate(Mockito.eq(text), Mockito.eq(sourceLanguage), Mockito.eq("EN"));
        Mockito.verify(client).translate(Mockito.eq(text), Mockito.eq(sourceLanguage), Mockito.eq("FR"));
    }

}
