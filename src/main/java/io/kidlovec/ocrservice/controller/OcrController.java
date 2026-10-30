package io.kidlovec.ocrservice.controller;

import com.alibaba.excel.EasyExcel;
import io.kidlovec.ocrservice.constants.OcrLang;
import io.kidlovec.ocrservice.excel.ExcelListener;
import io.kidlovec.ocrservice.model.TranslateInfo;
import io.kidlovec.ocrservice.translate.GoogleApi;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.PatternSyntaxException;

import io.kidlovec.ocrservice.upload.UploadImage;

import static io.kidlovec.ocrservice.constants.Constants.*;

/**
 * @author kidlovec
 * @date 2020-02-18
 * @since 1.0.0
 */
@CrossOrigin
@Slf4j
@RestController
public class OcrController {


    @org.springframework.beans.factory.annotation.Value("${ocr.local-excel.enabled:false}")
    private boolean localExcelEnabled;

    @Autowired
    private GoogleApi translator;
    @Autowired
    private ExcelListener excelListener;

    @GetMapping("/")
    public String index() {
        return "index";
    }


    @PostMapping(value = "/uploads", consumes = "multipart/*", headers = "content-type=multipart/form-data")
    @ApiOperation(value = "上传图片文件-识别文字")
    public String multiFileUpload(
            @RequestParam("files") @ApiParam(value = "文件-可以上传多个，只支持png/jpg", name = "files", required = true) List<MultipartFile> files
            , @RequestParam(value = "lang", required = false, defaultValue = "") @ApiParam(value = "eng", name =
            "lang") String lang
    ) throws Exception {


        if (!OcrLang.getLangType(lang)) {

            throw new IllegalArgumentException("The " + lang + " data is not existed!");

        }

        if (files == null || files.isEmpty() || files.size() > 10) {
            throw new IllegalArgumentException("Upload between 1 and 10 images");
        }
        Tesseract tesseract = getTesseract(lang);

        StringBuilder sb = new StringBuilder();

        for (MultipartFile file : files) {
            String text = getText(file, tesseract);
            final String format = String.format("%s : %s \t %s %s",
                    file.getOriginalFilename(),
                    text,
                    translator.translate(text, LANG_ZH, LANG_EN),
                    System.lineSeparator());

            System.out.println(format);
            sb.append(format);
        }

        return sb.toString();
    }

    @PostMapping(value = "/upload")
    @ApiOperation(value = "上传图片文件-识别文字")
    public String singleFileUpload(
            @RequestParam("file") @ApiParam(value = "文件，只支持png/jpg", name = "file", required = true) MultipartFile file
            , @RequestParam(value = "lang", required = false, defaultValue = "chi_sim") @ApiParam(value = "chi_sim", name = "lang") String lang
    ) throws Exception {
        if (!OcrLang.getLangType(lang)) {
            throw new IllegalArgumentException("The " + lang + " lang is not existed!");
        }

        log.info(" uploading file: {}", file.getOriginalFilename());

        long st = System.currentTimeMillis();
        final String text = getText(file, getTesseract(lang));

        log.warn(" ocr completed in ({} ms)", System.currentTimeMillis() - st);
        return text;
    }


    @GetMapping("/translate")
    @ApiOperation(value = "翻译")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "msg", value = "要翻译的文字", required = true, paramType = "query"),
            @ApiImplicitParam(name = "langFrom", value = "源语言", example = "ja/zh-CN/en", required = true,
                    defaultValue = "zh-CN", paramType
                    = "query"),
            @ApiImplicitParam(name = "langTo", value = "目标语言", example = "ja/zh-CN/en", required = true,
                    defaultValue = "en", paramType =
                    "query"),
    })
    public String getTranslate(@RequestParam("msg") String msg,
                               @RequestParam(value = "langFrom", required = false, defaultValue = LANG_ZH) String langFrom,
                               @RequestParam(value = "langTo", required = false, defaultValue = LANG_EN) String langTo
    ) throws Exception {
        log.debug("translate param : {}, {} -> {}", msg, langFrom, langTo);
        String translate = translator.translate(stringFilterBeforeTranslate(msg), langFrom, langTo);
        return translate;
    }


    @GetMapping("/translate-local/")
    @ApiOperation(value = "翻译-excel 读取 Excel 的 内容 并翻译到 格式参考 sample.xlsx ")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "path", value = "文件所在位置-全路径", dataType = "File", required = true, paramType =
                    "query"),
    })
    public String readExcel(@RequestParam("path") String path) {
        if (!localExcelEnabled) {
            throw new SecurityException("Local Excel access is disabled");
        }

        final List<TranslateInfo> objects =
                EasyExcel.read(path, TranslateInfo.class, excelListener).sheet("Sheet1").doReadSync();
        EasyExcel.write(path, TranslateInfo.class).sheet("Sheet1").doWrite(objects);

        return "success";
    }

    private String getText(MultipartFile file, Tesseract tesseract) throws Exception {
        // Never use the client-supplied filename as a filesystem path.
        BufferedImage image = UploadImage.read(file);
        Path temporary = Files.createTempFile("ocr-", ".png");
        try {
            BufferedImage rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = rgb.createGraphics();
            try {
                graphics.drawImage(image, 0, 0, Color.WHITE, null);
            } finally {
                graphics.dispose();
            }
            if (!ImageIO.write(rgb, "png", temporary.toFile())) {
                throw new IOException("PNG encoder unavailable");
            }
            String text = tesseract.doOCR(temporary.toFile());
            return stringFilterBeforeTranslate(filterForUnrelatedContents(text));
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    /**
     * after ocr there may be some unrelated content
     * you may need to filter these contents
     * TODO
     *
     * @param text
     * @return
     */
    private String filterForUnrelatedContents(String text) {

        // eg: filter the content before "2016"
//        return text.substring(text.indexOf("2016"));
        return text;
    }

    /**
     * generate the tesseract client
     * with some configuration
     *
     * @return
     */
    private Tesseract getTesseract(String lang) {
        Tesseract tesseract = new Tesseract();
        tesseract.setTessVariable("user_defined_dpi", "300");
        tesseract.setDatapath(TESS_DATA);

//        tesseract.setLanguage("chi_simmm");//中文识别
//        tesseract.setLanguage("chi_simm");//中文识别
//        tesseract.setLanguage("chi_tra");//繁体中文识别

        tesseract.setLanguage("eng");
        tesseract.setLanguage("enm");
        // 日文识别
        tesseract.setLanguage("jpn");
        // 中文识别
        tesseract.setLanguage("chi_sim");

        if (lang != null && !"".equals(lang.trim())) {

            tesseract.setLanguage(lang);
        }
        return tesseract;
    }

    /**
     * 过滤特殊字符
     *
     * @param str
     * @return
     * @throws PatternSyntaxException
     */
    private String stringFilterBeforeTranslate(String str) throws PatternSyntaxException {

//        str.replaceAll(FILTER_PATTERN, " ").trim();

        return str;
    }

}

