package bo.bosque.com.impexpap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BosqueApplication {

    public static void main(String[] args) {
        System.out.println("Iniciando Bosque...");
        SpringApplication.run(BosqueApplication.class, args);

    }

}
