package com.easymed.html2pdf;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class Html2pdf {
    public static void main(String[] args) {
        SpringApplication.run(Html2pdf.class, args);
    }
}





