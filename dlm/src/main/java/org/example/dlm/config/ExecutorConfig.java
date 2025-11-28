package org.example.dlm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ExecutorConfig {

    @Bean(name = "downloadExecutorService", destroyMethod = "shutdown")
    public ExecutorService downloadExecutorService() {
        int cores = Math.max(2, Runtime.getRuntime().availableProcessors());
        return Executors.newFixedThreadPool(cores);
    }
}
