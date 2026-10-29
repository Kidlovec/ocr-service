package io.kidlovec.ocrservice;

import io.kidlovec.ocrservice.controller.OcrController;
import io.kidlovec.ocrservice.exception.CommonExceptionAdvice;
import org.junit.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class LocalExcelAccessTest {
    @Test
    public void localFileAccessIsForbiddenByDefault() throws Exception {
        MockMvcBuilders.standaloneSetup(new OcrController())
                .setControllerAdvice(new CommonExceptionAdvice()).build()
                .perform(get("/translate-local/").param("path", "/does-not-exist.xlsx"))
                .andExpect(status().isForbidden());
    }
}
