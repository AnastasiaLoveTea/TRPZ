package org.example.dlm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.example.dlm.domain.*;
import org.example.dlm.repo.*;
import org.springframework.boot.*;
import org.springframework.context.annotation.Bean;

import java.time.Instant;
import java.util.UUID;

@SpringBootApplication
public class DlmApplication {

    public static void main(String[] args) { SpringApplication.run(DlmApplication.class, args); }

    @Bean
    CommandLineRunner init(UserRepo users, DownloadRepo downloads) {
        return args -> {
            var u = new User();
            u.setUsername("demo");
            u.setPasswordHash("{noop}demo");
            users.save(u);

            var d = new Download();
            d.setId(UUID.randomUUID());
            d.setUrl("https://example.com/file.iso");
            d.setFileName("file.iso");
            d.setSavePath("/tmp/file.iso");
            d.setStatus(DownloadStatus.QUEUED);
            d.setCreatedAt(Instant.now());
            d.setUpdatedAt(Instant.now());
            d.setOwner(u);
            downloads.save(d);
        };
    }
}
