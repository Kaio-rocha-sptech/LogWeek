package school.sptech.logweek_api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:contexto;DB_CLOSE_DELAY=-1")
class LogweekApiApplicationTests {

	@Test
	void contextLoads() {
	}

}
