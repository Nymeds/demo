package studdy.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		System.out.println("Servidor subiu, só n pode cair !");
		SpringApplication.run(DemoApplication.class, args);
	}

}
