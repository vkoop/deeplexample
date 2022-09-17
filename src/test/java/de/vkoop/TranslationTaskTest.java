package de.vkoop;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import picocli.CommandLine;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

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
            "--text=" + text
    };

    @Mock
    private TranslateClient client;

    @Test
    public void testParameterBinding() {
        final TranslationTask translationTask = CommandLine.populateCommand(new TranslationTask(), defaultArgs);

        assertEquals(authkey, translationTask.authKey);
        assertEquals(Optional.of(text), translationTask.text);
        assertEquals(sourceLanguage, translationTask.sourceLanguage);
        assertEquals(targetLanguages, translationTask.targetLanguages);
    }

    @Test
    public void integrationTest(){
        final TranslationTask translationTask = CommandLine.populateCommand(new TranslationTask(), defaultArgs);
        translationTask.setTranslateClient(client);
        translationTask.run();

        verify(client).translate(eq(text), eq(sourceLanguage), eq("EN"));
        verify(client).translate(eq(text), eq(sourceLanguage), eq("FR"));
    }

}
