package com.example.sshtunnel.config;

import com.example.sshtunnel.retry.RetryHandler;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSessionImpl;
import org.apache.sshd.client.session.SessionFactory;
import org.apache.sshd.common.io.IoSession;
import org.apache.sshd.common.session.SessionHeartbeatController;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j
public class SshTunnelConfiguration {

    @Bean
    public TunnelConfig tunnelConfig(ApplicationArguments arguments, Gson gson) {
        Resource resource;
        if (arguments.getSourceArgs().length > 0) {
            resource = new PathResource(arguments.getSourceArgs()[0]);
        } else {
            resource = new PathResource("config.json");
        }
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(resource.getInputStream()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return gson.fromJson(reader, TunnelConfig.class);
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public SshClient sshClient(TunnelConfig tunnelConfig) {
        SshClient sshClient = SshClient.setUpDefaultClient();
        ConfigUtils.parseKeyPair(tunnelConfig).forEach(sshClient::addPublicKeyIdentity);
        sshClient.setSessionFactory(new SessionFactory(sshClient){
            @Override
            protected ClientSessionImpl doCreateSession(IoSession ioSession) throws Exception {
                ClientSessionImpl session = super.doCreateSession(ioSession);
                Tunnel tunnel = session.getConnectionContext().getAttribute(Tunnel.TUNNEL_ATTRIBUTE_KEY);
                session.setSessionHeartbeat(SessionHeartbeatController.HeartbeatType.IGNORE, tunnel.getHeartbeatInterval());
                return session;
            }
        });
        return sshClient;
    }

    @Bean
    public RetryHandler retryHandler(ThreadPoolTaskScheduler scheduler){
        return tunnel -> {
            if (tunnel.getRetryCount().getAndIncrement() < tunnel.getRetryMax()) {
                Duration retryInterval = tunnel.getRetryInterval();
                scheduler.getScheduledExecutor().schedule(() -> {
                    log.info("{} retrying...", tunnel);
                    tunnel.start();
                }, retryInterval.toNanos(), TimeUnit.NANOSECONDS);
            } else {
                log.info("{} end retry", tunnel);
            }
        };
    }

}
