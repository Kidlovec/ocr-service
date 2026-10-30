package io.kidlovec.ocrservice.translate;

import org.junit.Test;
import java.io.IOException;
import static org.junit.Assert.assertEquals;

public class GoogleApiTest {
    @Test
    public void joinsTranslatedSegments() throws Exception {
        assertEquals("你好世界", GoogleApi.parseTranslation("[[[\"你好\",\"hello\"],[\"世界\",\"world\"]],null,\"en\"]"));
    }

    @Test(expected = IOException.class)
    public void rejectsObjectInsteadOfTranslationArray() throws Exception {
        GoogleApi.parseTranslation("{\"@type\":\"java.util.HashMap\",\"value\":\"unexpected\"}");
    }

    @Test(expected = IOException.class)
    public void rejectsMalformedSegment() throws Exception {
        GoogleApi.parseTranslation("[[[42]]]");
    }
}
