package io.kidlovec.ocrservice.translate;

/**
 * @author kidlovec
 * @date 2020-03-13
 * @since 1.0.0
 */
public class StringUtils {
    public static boolean isBlank(String string) {
        if (string == null || "".equals(string.trim())) {
            return true;
        }

        return false;
    }

    public static boolean isNotBlank(String string) {
        return !isBlank(string);
    }
}
