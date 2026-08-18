package bo.bosque.com.impexpap;

import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.tomcat.util.descriptor.web.SecurityCollection;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
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
 * <h3>El conector HTTP</h3>
 * Ver {@link #servletContainer()}.
 */
@SpringBootApplication
@EnableScheduling
public class BosqueApplication {

    /** Puerto del conector en texto plano que solo redirige. Ver {@link #servletContainer()}. */
    @Value("${http.port:8080}")
    private int httpPort;

    /**
     * Puerto HTTPS al que se redirige. Sale de {@code server.port} en vez de
     * estar escrito a mano: la version anterior tenia {@code 8443} fijo, asi que
     * el dia que cambiara el puerto la redireccion habria apuntado a un puerto
     * muerto — y el sintoma habria sido un bucle de 302 sin explicacion.
     */
    @Value("${server.port:8443}")
    private int httpsPort;

    public static void main(String[] args) {
        System.out.println("Iniciando Bosque...");
        SpringApplication.run(BosqueApplication.class, args);
    }

    /**
     * Tomcat con un segundo conector en texto plano que <b>solo redirige</b> a
     * HTTPS. El trabajo lo hace la restriccion {@code CONFIDENTIAL} sobre
     * {@code /*}: Tomcat responde 302 hacia {@code redirectPort} a cualquier
     * peticion que llegue sin cifrar.
     *
     * <h3>Por que es condicional</h3>
     * {@code @ConditionalOnProperty} sobre {@code server.ssl.enabled}: sin TLS
     * este bean <b>no se crea</b> y Spring Boot pone su fabrica por defecto.
     *
     * <p>Sin esa condicion, levantar el perfil {@code prod} con
     * {@code SSL_ENABLED=false} —que es como se prueba la configuracion de
     * produccion en una maquina de desarrollo, sin copiarle el certificado—
     * deja a Tomcat redirigiendo TODO hacia un 8443 que no esta escuchando. La
     * aplicacion arranca perfecto y no responde a nada, que es la peor forma de
     * fallar.
     *
     * <h3>Ojo con la publicacion de puertos</h3>
     * El contenedor hoy publica solo {@code -p 8443:8443}, asi que a este
     * conector no llega nadie desde afuera y la redireccion no se usa nunca.
     * Para que sirva de verdad —que alguien escriba {@code http://} y termine en
     * {@code https://}— hay que publicar tambien {@code http.port} en el
     * {@code podman run}. Se deja igual: no cuesta nada y el dia que se publique
     * el puerto ya funciona.
     */
    @Bean
    @ConditionalOnProperty(name = "server.ssl.enabled", havingValue = "true")
    public ServletWebServerFactory servletContainer() {
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory() {
            @Override
            protected void postProcessContext(Context context) {
                SecurityConstraint securityConstraint = new SecurityConstraint();
                securityConstraint.setUserConstraint("CONFIDENTIAL");
                SecurityCollection collection = new SecurityCollection();
                collection.addPattern("/*");
                securityConstraint.addCollection(collection);
                context.addConstraint(securityConstraint);
            }
        };
        tomcat.addAdditionalTomcatConnectors(redirectConnector());
        return tomcat;
    }

    private Connector redirectConnector() {
        Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
        connector.setScheme("http");
        connector.setPort(httpPort);
        connector.setSecure(false);
        connector.setRedirectPort(httpsPort);
        return connector;
    }
}
