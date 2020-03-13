package io.kidlovec.ocrservice.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * Swagger配置
 *
 * @author kidlovec
 * @date 2020-03-12
 */
@Configuration
@EnableSwagger2
public class SwaggerConfig {

    @Bean
    public Docket createRestApi() {

        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage("io.kidlovec"))
                .paths(PathSelectors.any())
                .build();

    }

    public ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                //页面标题
                .title("ocr api平台")
                //创建人
                .contact(new Contact("Kidlovec", "https://http://kidlovec.github.io/", "kidlovec@gmail.com"))
                //描述
                .description("just for fun")
                //版本号
                .version("0.0.2")
                .build();
    }
}
