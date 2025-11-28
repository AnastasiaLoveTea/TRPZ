package org.example.dlm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DlmApplication {
    public static void main(String[] args) {
        SpringApplication.run(DlmApplication.class, args);
    }
}
