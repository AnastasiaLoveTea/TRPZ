package org.example.dlm.config;

import org.example.dlm.command.CommandBus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {
    @Bean public CommandBus commandBus() { return new CommandBus(); }
}
