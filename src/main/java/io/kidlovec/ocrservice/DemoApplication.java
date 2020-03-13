package io.kidlovec.ocrservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoApplication {
    //
    public static void main(String[] args) {

        SpringApplication.run(DemoApplication.class, args);
    }


//    public static void main(String[] args) throws Exception {
//        // 普通方式初始化
//        GoogleApi googleApi = new GoogleApi();
//        // 通过代理
////        GoogleApi googleApi = new GoogleApi("122.224.227.202", 3128);
//        String result = googleApi.translate("hello world",  "zh");
//        System.out.println(result);
//    }
}

