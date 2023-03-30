package com.example.sshtunnel.config;

import com.google.common.net.HostAndPort;
import org.apache.sshd.common.config.keys.FilePasswordProvider;
import org.apache.sshd.common.keyprovider.KeyIdentityProvider;
import org.apache.sshd.common.util.io.resource.PathResource;
import org.apache.sshd.common.util.security.SecurityUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class ConfigUtils {

    public static List<KeyPair> parseKeyPair(TunnelConfig config) {
        return Optional.of(config.getKeyFiles()).map(keyFiles -> keyFiles.stream().map(keyFile -> {
                    PathResource pathResource = new PathResource(Paths.get(keyFile));
                    try {
                        return SecurityUtils.loadKeyPairIdentities(null, pathResource, pathResource.openInputStream(), FilePasswordProvider.EMPTY);
                    } catch (IOException | GeneralSecurityException e) {
                        throw new RuntimeException(e);
                    }
                })
                .map(Iterable::iterator)
                .map(KeyIdentityProvider::exhaustCurrentIdentities)
                .collect(Collectors.toList())).orElse(Collections.emptyList());
    }

    public static List<Tunnel> convertTunnels(TunnelConfig config) {
        return Optional.of(config.getTunnels())
                .map(tunnels -> tunnels.stream().map(tunnel -> {
                    Tunnel tunnel1 = new Tunnel();
                    String[] split = tunnel.getTunnel().split("\\s+");
                    tunnel1.setBindAddr(HostAndPort.fromString(split[0]));
                    tunnel1.setMode(Tunnel.Mode.find(split[1]));
                    tunnel1.setDialAddr(HostAndPort.fromString(split[2]));
                    Optional.ofNullable(tunnel.getConnectTimeout()).ifPresent(tunnel1::setConnectTimeout);
                    Optional.ofNullable(tunnel.getAuthTimeout()).ifPresent(tunnel1::setAuthTimeout);
                    Optional.ofNullable(tunnel.getRetryInterval()).ifPresent(tunnel1::setRetryInterval);
                    Optional.ofNullable(tunnel.getHeartbeatInterval()).ifPresent(tunnel1::setHeartbeatInterval);
                    Optional.ofNullable(tunnel.getRetryMax()).ifPresent(tunnel1::setRetryMax);
                    try {
                        URI uri = new URI("ssh://" + tunnel.getServer());
                        tunnel1.setUser(uri.getRawUserInfo());
                        tunnel1.setHostAddr(HostAndPort.fromParts(uri.getHost(), (uri.getPort() <= 0 ? 22 : uri.getPort())));
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                    return tunnel1;
                }).collect(Collectors.toList())).orElse(Collections.emptyList());
    }
}
