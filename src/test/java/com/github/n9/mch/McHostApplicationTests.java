package com.github.n9.mch;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"mch.minecraft.auto-update-paper=false",
		"spring.datasource.url=jdbc:h2:mem:mch-test;DB_CLOSE_DELAY=-1"
})
class McHostApplicationTests {

	@Test
	void contextLoads() {
	}

}
