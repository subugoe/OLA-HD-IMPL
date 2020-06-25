package ola.hd.longtermstorage.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.client.LinkDiscoverer;
import org.springframework.hateoas.client.LinkDiscoverers;
import org.springframework.hateoas.mediatype.collectionjson.CollectionJsonLinkDiscoverer;
import org.springframework.plugin.core.SimplePluginRegistry;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.*;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
                .select()
                .apis(RequestHandlerSelectors.basePackage("ola.hd.longtermstorage"))
                .paths(PathSelectors.any())
                .build()
                .apiInfo(API_INFO)
                .protocols(Set.of("http"))
                .securitySchemes(buildSecuritySchemes())
                .useDefaultResponseMessages(false);
    }

    @Bean
    public LinkDiscoverers discoverers() {
        List<LinkDiscoverer> plugins = new ArrayList<>();
        plugins.add(new CollectionJsonLinkDiscoverer());
        return new LinkDiscoverers(SimplePluginRegistry.create(plugins));

    }

    private List<SecurityScheme> buildSecuritySchemes() {
        List<SecurityScheme> schemes = new ArrayList<>();
        schemes.add(new BasicAuth("basicAuth"));
        schemes.add(new ApiKey("bearer", "Authorization", "header"));

        return schemes;
    }
}
