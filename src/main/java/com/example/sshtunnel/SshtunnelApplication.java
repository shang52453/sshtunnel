package com.example.sshtunnel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class SshtunnelApplication {

    public static void main(String[] args) {
		SpringApplication.run(SshtunnelApplication.class, args);
    }
}
