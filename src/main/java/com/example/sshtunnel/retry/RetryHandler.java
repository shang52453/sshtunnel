package com.example.sshtunnel.retry;

import com.example.sshtunnel.config.Tunnel;

public interface RetryHandler {

    void retry(Tunnel tunnel);
}
