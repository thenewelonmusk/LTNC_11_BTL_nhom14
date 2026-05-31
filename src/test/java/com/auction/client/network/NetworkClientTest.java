package com.auction.client.network;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NetworkClientTest {

	@Test
	@DisplayName("Test NetworkClient khởi tạo đúng chuẩn Singleton")
	void testSingletonInstance() {
		NetworkClient instance1 = NetworkClient.getInstance();
		NetworkClient instance2 = NetworkClient.getInstance();

		assertNotNull(instance1, "Instance không được phép null");

		assertSame(instance1, instance2, "NetworkClient phải tuân thủ chặt chẽ pattern Singleton");
	}
}