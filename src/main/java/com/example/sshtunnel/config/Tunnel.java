package com.example.sshtunnel.config;

import com.example.sshtunnel.retry.RetryHandler;
import com.google.common.net.HostAndPort;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.AttributeRepository;
import org.apache.sshd.common.session.SessionHeartbeatController;
import org.apache.sshd.common.util.net.SshdSocketAddress;

import java.io.IOException;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

@Data
@Slf4j
public class Tunnel {
    public final static AttributeRepository.AttributeKey<Tunnel> TUNNEL_ATTRIBUTE_KEY = new AttributeRepository.AttributeKey<>();
    private Mode mode;
    private String user;
    private HostAndPort hostAddr;
    private HostAndPort bindAddr;
    private HostAndPort dialAddr;

    private Duration connectTimeout = Duration.ofSeconds(10);

    private Duration authTimeout = Duration.ofSeconds(10);

    private Duration retryInterval = Duration.ofSeconds(10);

    private Duration heartbeatInterval = Duration.ofSeconds(10);

    private Integer retryMax = 30;

    private final AtomicInteger retryCount = new AtomicInteger(0);

    private SshClient sshClient;

    private RetryHandler retryHandler;


    public void start() {
        this.checkConfig();
        try {
            AttributeRepository context = AttributeRepository.ofKeyValuePair(Tunnel.TUNNEL_ATTRIBUTE_KEY, this);
            ClientSession session = sshClient.connect(getUser(), getHostAddr().getHost(), getHostAddr().getPort(), context)
                    .addListener(future -> {
                        if (future.isConnected()){
                            future.getSession().setSessionHeartbeat(SessionHeartbeatController.HeartbeatType.IGNORE, getHeartbeatInterval());
                        }
                    })
                    .verify(getConnectTimeout())
                    .getSession();
            session.auth().verify(getAuthTimeout());
            if (retryHandler != null) {
                session.addCloseFutureListener(future -> {
                    if (future.isClosed()) {
                        retryHandler.retry(Tunnel.this);
                    }
                });
            }
            retryCount.set(0);
            getMode().startPortForwarding(this, session);
        } catch (Throwable t) {
            log.error("", t);
            if (retryHandler != null) {
                retryHandler.retry(this);
            }
        }
    }


    public void checkConfig() {
        Objects.requireNonNull(mode);
        Objects.requireNonNull(hostAddr);
        Objects.requireNonNull(bindAddr);
        Objects.requireNonNull(dialAddr);
        Objects.requireNonNull(sshClient);
    }

    @AllArgsConstructor
    public enum Mode {
        LOCAL_FORWARD("->") {
            @Override
            public void startPortForwarding(Tunnel tunnel, ClientSession session) throws IOException {
                session.startLocalPortForwarding(new SshdSocketAddress(tunnel.getBindAddr().getHost(), tunnel.getBindAddr().getPort()), new SshdSocketAddress(tunnel.getDialAddr().getHost(), tunnel.getDialAddr().getPort()));
            }
        },
        REMOTE_FORWARD("<-") {
            @Override
            public void startPortForwarding(Tunnel tunnel, ClientSession session) throws IOException {
                session.startRemotePortForwarding(new SshdSocketAddress(tunnel.getDialAddr().getHost(), tunnel.getDialAddr().getPort()), new SshdSocketAddress(tunnel.getBindAddr().getHost(), tunnel.getBindAddr().getPort()));
            }
        };

        private final String value;

        public abstract void startPortForwarding(Tunnel tunnel, ClientSession session) throws IOException;

        public static Mode find(String s) {
            for (Mode mode : Mode.values()) {
                if (mode.value.equals(s)) {
                    return mode;
                }
            }
            throw new IllegalArgumentException("invalid mode syntax");
        }

    }
}
