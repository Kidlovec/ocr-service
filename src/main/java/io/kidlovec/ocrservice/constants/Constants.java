package io.kidlovec.ocrservice.constants;

/**
 * @author kidlovec
 * @date 2020-03-13
 * @since 1.0.0
 */
public interface Constants {
    String LANG_EN = "en";
    String LANG_ZH = "zh-CN";

    String SUFFIX_PNG = "png";
    String SUFFIX_JPG = "jpg";
    String UPLOAD_PATH = "src/main/resources/dst/";
    String TESS_DATA = "//usr/local//share//tessdata///";

    String FILTER_PATTERN = "[^0-9a-zA-Z\u4e00-\u9fa5\\n\\s+“”]+";
}
