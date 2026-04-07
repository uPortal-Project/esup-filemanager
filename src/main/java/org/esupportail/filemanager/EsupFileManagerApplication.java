package org.esupportail.filemanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ImportResource;

@SpringBootApplication(scanBasePackages = "org.esupportail.filemanager")
@ImportResource({"/applicationContext.xml", "/drives.xml"})
public class EsupFileManagerApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(EsupFileManagerApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(EsupFileManagerApplication.class, args);
    }

}
