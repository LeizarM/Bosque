package bo.bosque.com.impexpap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Arranque de la aplicacion.
 *
 * <h3>Por que ya no lleva {@code exclude = JdbcRepositoriesAutoConfiguration.class}</h3>
 * La version que corria en el servidor tenia esa exclusion, y por un motivo
 * real: con {@code spring-boot-starter-data-jdbc} en el pom, Spring Data JDBC
 * detecta SQL Server, carga su {@code SqlServerDialect}, y ese dialecto
 * referencia {@code microsoft.sql.DateTimeOffset} — una clase que vive DENTRO
 * de mssql-jdbc. Al pasar a jTDS la clase desaparece y el contexto muere al
 * arrancar con {@code TypeNotPresentException}.
 *
 * <p>El pom unificado ataca la misma causa un escalon mas abajo: reemplaza
 * {@code spring-boot-starter-data-jdbc} por {@code spring-boot-starter-jdbc}.
 * Este proyecto usa {@code JdbcTemplate} a secas y no tiene ni un repositorio de
 * Spring Data —"Found 0 JDBC repository interfaces" en cada arranque—, asi que
 * la dependencia sobraba entera.
 *
 * <p><b>Las dos soluciones juntas no compilan:</b> sin la dependencia, la clase
 * que nombra el {@code exclude} no existe. Se elige sacar la dependencia;
 * excluir la autoconfiguracion deja igual el jar cargado de codigo que nadie
 * usa, y encima obliga a acordarse de la exclusion para siempre.
 *
 * <h3>Sin TLS ni conector de redireccion</h3>
 * El HTTPS lo atiende el Caddy del servidor, que renueva el certificado solo
 * (ver application-prod.properties y LEEME-SECRETS.md): Tomcat escucha solo
 * HTTP, con la fabrica por defecto de Spring Boot. Por eso se quito el segundo
 * conector que redirigia de 8080 a HTTPS; ademas, ese puerto nunca se publico.
 */
@SpringBootApplication
@EnableScheduling
public class BosqueApplication {

    public static void main(String[] args) {
        System.out.println("Iniciando Bosque...");
        SpringApplication.run(BosqueApplication.class, args);
    }
}
