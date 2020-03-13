package io.kidlovec.ocrservice.model;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * @author kidlovec
 * @date 2020-03-13
 * @since 1.0.0
 */
@Data
public class TranslateInfo {

    @ExcelProperty(index = 0, value = "文件名")
    private String name;
    @ExcelProperty(index = 1, value ="原文")
    private String originContent;
    @ExcelProperty(index = 2, value = "译文")
    private String engContent;
}
