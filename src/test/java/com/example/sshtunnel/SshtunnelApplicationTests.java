package com.example.sshtunnel;

import com.example.sshtunnel.config.TunnelConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SshtunnelApplicationTests {

	@Autowired
	private TunnelConfig config;

	@Test
	void contextLoads() throws Exception {
		System.out.println(config);
	}

}
