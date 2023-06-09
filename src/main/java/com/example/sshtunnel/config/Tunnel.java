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
import org.apache.sshd.common.util.io.IoUtils;
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
    private HostAndPort localAddr;
    private HostAndPort remoteAddr;

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
        ClientSession session = null;
        try {
            AttributeRepository context = AttributeRepository.ofKeyValuePair(Tunnel.TUNNEL_ATTRIBUTE_KEY, this);
            session = sshClient.connect(getUser(), getHostAddr().getHost(), getHostAddr().getPort(), context)
                    .addListener(future -> {
                        if (future.isConnected()){
                            future.getSession().setSessionHeartbeat(SessionHeartbeatController.HeartbeatType.IGNORE, getHeartbeatInterval());
                        }
                    })
                    .verify(getConnectTimeout())
                    .getSession();
            session.auth().verify(getAuthTimeout());
            getMode().startPortForwarding(this, session);
            retryCount.set(0);
            if (retryHandler != null) {
                session.addCloseFutureListener(future -> {
                    if (future.isClosed()) {
                        retryHandler.retry(this);
                    }
                });
            }
        } catch (Throwable t) {
            IoUtils.closeQuietly(session);
            log.error("", t);
            if (retryHandler != null) {
                retryHandler.retry(this);
            }
        }
    }


    public void checkConfig() {
        Objects.requireNonNull(mode);
        Objects.requireNonNull(hostAddr);
        Objects.requireNonNull(localAddr);
        Objects.requireNonNull(remoteAddr);
        Objects.requireNonNull(sshClient);
    }

    @AllArgsConstructor
    public enum Mode {
        LOCAL_FORWARD("->") {
            @Override
            public void startPortForwarding(Tunnel tunnel, ClientSession session) throws IOException {
                session.startLocalPortForwarding(new SshdSocketAddress(tunnel.getLocalAddr().getHost(), tunnel.getLocalAddr().getPort()), new SshdSocketAddress(tunnel.getRemoteAddr().getHost(), tunnel.getRemoteAddr().getPort()));
            }
        },
        REMOTE_FORWARD("<-") {
            @Override
            public void startPortForwarding(Tunnel tunnel, ClientSession session) throws IOException {
                session.startRemotePortForwarding(new SshdSocketAddress(tunnel.getRemoteAddr().getHost(), tunnel.getRemoteAddr().getPort()), new SshdSocketAddress(tunnel.getLocalAddr().getHost(), tunnel.getLocalAddr().getPort()));
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
