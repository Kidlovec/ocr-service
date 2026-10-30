package io.kidlovec.ocrservice.upload;

import org.junit.Test;
import org.springframework.mock.web.MockMultipartFile;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import static org.junit.Assert.assertEquals;

public class UploadImageTest {
    @Test
    public void decodesImageWithoutUsingClientPath() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 3, BufferedImage.TYPE_INT_RGB), "png", bytes);
        BufferedImage image = UploadImage.read(new MockMultipartFile("file", "../../outside.png", "image/png", bytes.toByteArray()));
        assertEquals(2, image.getWidth());
        assertEquals(3, image.getHeight());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsDisguisedNonImage() throws Exception {
        UploadImage.read(new MockMultipartFile("file", "photo.png", "image/png", "not an image".getBytes("UTF-8")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsEmptyFile() throws Exception {
        UploadImage.read(new MockMultipartFile("file", new byte[0]));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsOversizedFile() throws Exception {
        UploadImage.read(new MockMultipartFile("file", new byte[10 * 1024 * 1024 + 1]));
    }
}
