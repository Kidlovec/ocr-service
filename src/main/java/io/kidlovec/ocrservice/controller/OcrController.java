package io.kidlovec.ocrservice.controller;

import com.alibaba.excel.EasyExcel;
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
import javax.validation.constraints.NotNull;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.PatternSyntaxException;

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
    public String multiFileUpload(@RequestParam("files") @ApiParam(value = "文件-可以上传多个，只支持png/jpg",name = "files", required = true) List<MultipartFile> files) throws Exception {
        Tesseract tesseract = getTesseract();

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
    public String singleFileUpload(@RequestParam("file") @ApiParam(value = "文件，只支持png/jpg",name = "file", required = true) MultipartFile file) throws Exception {
        return getText(file, getTesseract());
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
        log.debug("translate param :{}, {} -> {}", msg, langFrom, langTo);
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

        final List<TranslateInfo> objects =
                EasyExcel.read(path, TranslateInfo.class, excelListener).sheet("Sheet1").doReadSync();
        EasyExcel.write(path, TranslateInfo.class).sheet("Sheet1").doWrite(objects);

        return "success";
    }

    private String getText(MultipartFile file, Tesseract tesseract) throws Exception {
        byte[] bytes = file.getBytes();
        final String uploadPathName = UPLOAD_PATH + file.getOriginalFilename();
        Path path = Paths.get(uploadPathName);
        Files.write(path, bytes);
        String text;
        if (file != null && checkPng(file.getOriginalFilename())) {

            final String jpgName = generateJpgName(file.getOriginalFilename());
            String newFilePath = UPLOAD_PATH + jpgName;

            final File file1 = png2Jpg(uploadPathName, newFilePath);
            text = tesseract.doOCR(file1);

        } else {

            File convFile = convert(file);
            text = tesseract.doOCR(convFile);
        }

        text = filterForUnrelatedContents(text);

        return stringFilterBeforeTranslate(text);
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
    private Tesseract getTesseract() {
        Tesseract tesseract = new Tesseract();
        tesseract.setTessVariable("user_defined_dpi", "300");
        tesseract.setDatapath(TESS_DATA);

//        tesseract.setLanguage("chi_simmm");//中文识别
//        tesseract.setLanguage("chi_simm");//中文识别
        tesseract.setLanguage("chi_sim");//中文识别
//        tesseract.setLanguage("chi_tra");//繁体中文识别
        return tesseract;
    }

    private static boolean checkPng(String fileName) {
        return SUFFIX_PNG.equals(fileName.substring(fileName.lastIndexOf(".") + 1));
    }

    public static File convert(@NotNull MultipartFile file) throws IOException {
        File convFile = new File(file.getOriginalFilename());
        convFile.createNewFile();
        FileOutputStream fos = new FileOutputStream(convFile);
        fos.write(file.getBytes());
        fos.close();
        return convFile;
    }

    public static File png2Jpg(String filePath, String dstPath) {

        BufferedImage bufferedImage;

        try {

            //read image file
            bufferedImage = ImageIO.read(new File(filePath));

            // create a blank, RGB, same width and height, and a white background
            BufferedImage newBufferedImage = new BufferedImage(bufferedImage.getWidth(),
                    bufferedImage.getHeight(), BufferedImage.TYPE_INT_RGB);

            //TYPE_INT_RGB:创建一个RBG图像，24位深度，成功将32位图转化成24位

            newBufferedImage.createGraphics().drawImage(bufferedImage, 0, 0, Color.WHITE, null);
            final File file = new File(dstPath);
            // write to jpeg file
            ImageIO.write(newBufferedImage, SUFFIX_JPG, file);

            return file;
        } catch (IOException e) {

            e.printStackTrace();

        }

        return null;

    }

    public static String generateJpgName(String fileName) {
        final int i = fileName.lastIndexOf(".");
        final String substring = fileName.substring(0, i) + "." + SUFFIX_JPG;
        System.out.println(substring);
        return substring;
    }

    /**
     * 过滤特殊字符
     *
     * @param str
     * @return
     * @throws PatternSyntaxException
     */
    private String stringFilterBeforeTranslate(String str) throws PatternSyntaxException {

        return str.replaceAll(FILTER_PATTERN, " ").trim();
    }

}

