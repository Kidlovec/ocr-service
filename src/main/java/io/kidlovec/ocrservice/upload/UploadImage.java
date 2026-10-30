package io.kidlovec.ocrservice.upload;

import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

public final class UploadImage {
    private UploadImage() { }

    public static BufferedImage read(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty() || file.getSize() > 10L * 1024 * 1024) {
            throw new IllegalArgumentException("Image must be non-empty and at most 10 MB");
        }
        try (InputStream input = file.getInputStream();
             ImageInputStream imageInput = ImageIO.createImageInputStream(input)) {
            if (imageInput == null) {
                throw new IllegalArgumentException("Invalid image");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) {
                throw new IllegalArgumentException("Only PNG and JPEG images are supported");
            }
            ImageReader reader = readers.next();
            try {
                String format = reader.getFormatName();
                if (!"png".equalsIgnoreCase(format) && !"jpeg".equalsIgnoreCase(format)) {
                    throw new IllegalArgumentException("Only PNG and JPEG images are supported");
                }
                reader.setInput(imageInput, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > 25000000L) {
                    throw new IllegalArgumentException("Image exceeds 25 million pixels");
                }
                return reader.read(0);
            } finally {
                reader.dispose();
            }
        }
    }
}
