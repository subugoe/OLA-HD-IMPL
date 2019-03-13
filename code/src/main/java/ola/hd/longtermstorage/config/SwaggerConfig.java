package ola.hd.longtermstorage.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.ArrayList;

@Configuration
@EnableSwagger2
public class SwaggerConfig {

    private static final Contact API_CONTACT = new Contact("Triet Doan",
            "https://www.gwdg.de/",
            "triet.doan@gwdg.de");
    private static final ApiInfo API_INFO = new ApiInfo("OLA-HD Long-term Archive",
            "This is the documentation for the API of the Long-term Archive System",
            "1.0",
            "urn:tos",
            API_CONTACT,
            "Apache 2.0",
            "http://www.apache.org/licenses/LICENSE-2.0", new ArrayList<>());

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(API_INFO);
    }
}
