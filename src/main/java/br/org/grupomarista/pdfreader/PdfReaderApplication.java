package br.org.grupomarista.pdfreader;

import br.org.grupomarista.pdfreader.configs.FileStorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;


@SpringBootApplication
@EnableConfigurationProperties({
        FileStorageProperties.class
})
public class PdfReaderApplication {

    public static void main(String[] args) {

        SpringApplication.run(PdfReaderApplication.class, args);
    }



}

