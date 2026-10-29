package io.kidlovec.ocrservice.translate;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URLEncoder;

/**
 * @author kidlovec
 * @date 2020-03-13
 * @since 1.0.0
 */
@Slf4j
@Component
public class GoogleApi {
    private static final String PATH = "/gettk.js";

    static ScriptEngine engine = null;

    private Browser browser = null;

    static {
        ScriptEngineManager maneger = new ScriptEngineManager();
        engine = maneger.getEngineByName("javascript");
        FileInputStream fileInputStream = null;
        Reader scriptReader = null;

        try {
            scriptReader = new InputStreamReader(GoogleApi.class.getResourceAsStream(PATH), "utf-8");
            engine.eval(scriptReader);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (scriptReader != null) {
                try {
                    scriptReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public GoogleApi() {
        this.browser = new Browser();
    }

    public GoogleApi(String ip, Integer port) {
        this.browser = new Browser();
        this.browser.setProxy(ip, port);
    }

    public String getTKK() throws Exception {
        browser.setUrl("https://translate.google.cn/");

        try {
            log.debug(" path : {}", browser.getUrl());
            String result = browser.executeGet();
            if (StringUtils.isNotBlank(result)) {
                if (result.indexOf("tkk") > -1) {
                    String matchString = RegularUtil.findMatchString(result, "tkk:.*?',");
                    String tkk = matchString.substring(5, matchString.length() - 2);
                    return tkk;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("获取 tkk 出错");
        }

        return null;
    }

    public static String getTK(String word, String tkk) {
        String result = null;

        try {
            if (engine instanceof Invocable) {
                Invocable invocable = (Invocable) engine;
                result = (String) invocable.invokeFunction("tk", new Object[]{word, tkk});
            }
        } catch (Exception e) {
            throw new RuntimeException("获取 tk 出错");
        }

        return result;
    }

    public String translate(String word, String from, String to) throws Exception {
        if (StringUtils.isBlank(word)) {
            return null;
        }

        String tkk = getTKK();

        if (StringUtils.isBlank(tkk)) {
            throw new RuntimeException("无法获取 tkk");
        }

        String tk = getTK(word, tkk);

        try {
            word = URLEncoder.encode(word, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }

        StringBuffer buffer = new StringBuffer("https://translate.google.cn/translate_a/single?client=t");

        if (StringUtils.isBlank(from)) {
            from = "auto";
        }

        buffer.append("&sl=" + from);
        buffer.append("&tl=" + to);
        buffer.append("&hl=zh-CN&dt=at&dt=bd&dt=ex&dt=ld&dt=md&dt=qca&dt=rw&dt=rm&dt=ss&dt=t&ie=UTF-8&oe=UTF-8&source=btn&kc=0");
        buffer.append("&tk=" + tk);
        buffer.append("&q=" + word);
        browser.setUrl(buffer.toString());

        try {
            log.debug(" path : {}", browser.getUrl());
            String result = browser.executeGet();
            return parseTranslation(result);
        } catch (Exception e) {
            throw new RuntimeException("结果集解析出错");
        }
    }

    // Tree parsing does not enable polymorphic type resolution.
    static String parseTranslation(String result) throws IOException {
        JsonNode root = new ObjectMapper().readTree(result);
        if (root == null || !root.isArray() || !root.path(0).isArray()) {
            throw new IOException("Invalid translation response");
        }
        StringBuilder translated = new StringBuilder();
        for (JsonNode segment : root.get(0)) {
            if (!segment.isArray() || !segment.path(0).isTextual()) {
                throw new IOException("Invalid translation segment");
            }
            translated.append(segment.get(0).textValue());
        }
        return translated.toString();
    }

    /**
     * 自动检测源语言
     *
     * @param word 要翻译的词
     * @param to   翻译的目标语言, 参考谷歌接口
     * @return
     * @throws Exception
     */
    public String translate(String word, String to) throws Exception {
        return translate(word, null, to);
    }
}
