package dev.jbaby.shadwick.examples;

import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.WicketFilter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/** Runs the showcase on Spring Boot's embedded server: {@code ./mvnw spring-boot:run -pl shadwick-examples}. */
@SpringBootApplication
public class ShadwickExamplesApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShadwickExamplesApplication.class, args);
    }

    @Bean
    WebApplication wicketApp() {
        return new ExamplesApplication();
    }

    @Bean
    WicketFilter wicketFilter(WebApplication wicketApp) {
        WicketFilter filter = new WicketFilter(wicketApp);
        filter.setFilterPath("/");
        return filter;
    }
}
