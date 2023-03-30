package com.example.sshtunnel.config;

import lombok.Data;

import java.time.Duration;
import java.util.List;

@Data
public class TunnelConfig {
    private List<String> keyFiles;
    private List<Tunnel> tunnels;

    @Data
    public static class Tunnel{
        private String tunnel;
        private String server;
        private Duration connectTimeout;
        private Duration authTimeout;
        private Duration retryInterval;
        private Duration heartbeatInterval;
        private Integer retryMax;
    }
}
