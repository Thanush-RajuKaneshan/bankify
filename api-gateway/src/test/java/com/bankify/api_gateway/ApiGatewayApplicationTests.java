package com.bankify.api_gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest
@AutoConfigureWebTestClient
class ApiGatewayApplicationTests {

	@Autowired
	WebTestClient client;

	@Test
	void contextLoads() {
	}

	@Test
	void webPageNeedsNoToken() {
		for (String path : new String[] {"/", "/app.js", "/style.css"}) {
			client.get().uri(path).exchange().expectStatus().isOk();
		}
	}

}
