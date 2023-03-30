package com.example.sshtunnel.runner;

import com.example.sshtunnel.config.ConfigUtils;
import com.example.sshtunnel.config.TunnelConfig;
import com.example.sshtunnel.retry.RetryHandler;
import lombok.AllArgsConstructor;
import org.apache.sshd.client.SshClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class TunnelStartRunner implements CommandLineRunner {

    private SshClient sshClient;
    private TunnelConfig tunnelConfig;
    private RetryHandler retryHandler;

    @Override
    public void run(String... args) {
        ConfigUtils.convertTunnels(tunnelConfig).forEach(tunnel -> {
            tunnel.setSshClient(sshClient);
            tunnel.setRetryHandler(retryHandler);
            tunnel.start();
        });
    }
}
