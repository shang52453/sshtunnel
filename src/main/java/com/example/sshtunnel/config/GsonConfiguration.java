package com.example.sshtunnel.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.gson.GsonBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@Slf4j
public class GsonConfiguration {

    @Bean
    public GsonBuilderCustomizer gsonBuilderCustomizer(){
        return builder -> {
            builder.registerTypeAdapter(Duration.class, new DurationAdapter());
        };
    }

}