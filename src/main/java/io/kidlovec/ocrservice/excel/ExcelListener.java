package io.kidlovec.ocrservice.excel;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import io.kidlovec.ocrservice.model.TranslateInfo;
import io.kidlovec.ocrservice.translate.GoogleApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author kidlovec
 * @date 2020-03-13
 * @since 1.0.0
 */
@Slf4j
@Component
public class ExcelListener extends AnalysisEventListener<TranslateInfo> {

    @Autowired
    private GoogleApi translator;
//    private List<TranslateInfo> data = new ArrayList<>();

    @Override
    public void doAfterAllAnalysed(AnalysisContext arg0) {

        //解析结束后销毁资源，（疑惑，clear()好像不能测地回收内存。）
//        data.clear();
    }

    @Override
    public void invoke(TranslateInfo object, AnalysisContext context) {
//        log.debug("解析到一条数据:{}", JSON.toJSONString(object));

        // 数据存储到list，供批量处理，或者自己后续的业务处理
//        data.add(object);
        // 根据自己的是业务而处理（就是用service层保存到数据库了）
        doSomething(object);
    }

    public void doSomething(TranslateInfo object) {

        final String text = object.getOriginContent();

        if (text != null && !"".equals(text)) {
            try {
                final String translated = translator.translate(text, "zh-CN", "en");
                log.debug("翻译: {} -> {}", text, translated);
                object.setEngContent(translated);
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }


        //入库操作
    }
}