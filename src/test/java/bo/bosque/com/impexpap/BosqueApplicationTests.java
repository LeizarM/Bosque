package bo.bosque.com.impexpap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Arranque del contexto completo.
 *
 * <h3>Esto no es un test unitario: necesita la base de verdad</h3>
 * Levantar el contexto <b>abre una conexion</b>. No es opcional ni perezoso:
 * {@code spring-boot-starter-data-jdbc} activa
 * {@code JdbcRepositoriesAutoConfiguration}, que para construir
 * {@code jdbcCustomConversions} le pregunta el dialecto al motor. Sin base
 * alcanzable, el contexto no arranca.
 *
 * <p>Eso siempre fue asi. Lo que lo disimulaba es que la contrasena de
 * {@code sa} estaba puesta como valor por defecto en
 * {@code application.properties}: el test se conectaba a la base de PRODUCCION
 * en cada {@code mvn test} y nadie tenia por que enterarse. Al sacar ese default
 * —que es el punto del hallazgo C2— la dependencia quedo a la vista.
 *
 * <h3>Por que se saltea en vez de fallar</h3>
 * Sin {@code DB_PASSWORD} en el entorno, JUnit lo marca como <i>skipped</i> y el
 * resto de la suite —102 tests que no tocan la base— corre igual. La alternativa
 * seria volver a escribir credenciales reales en el repositorio, que es
 * exactamente lo que se vino a sacar.
 *
 * <p>Para correrlo de verdad, con las variables ya exportadas (ver
 * LEEME-SECRETS.md):
 * <pre>
 *   mvn test -Dtest=BosqueApplicationTests
 * </pre>
 *
 * <p>El {@code jwt.secret} sigue siendo de juguete —64 bytes de ceros, el minimo
 * que exige {@code JwtProvider}— porque para levantar el contexto no hace falta
 * que sea el real, y asi no hace falta tener el secreto de produccion a mano
 * para correr el test. La base si tiene que ser real: es lo unico que este test
 * comprueba.
 */
@SpringBootTest(properties = {
        "JWT_SECRET=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=="
})
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+",
        disabledReason = "Levanta el contexto completo y eso abre una conexion real. "
                       + "Exportar DB_USERNAME y DB_PASSWORD para correrlo.")
class BosqueApplicationTests {

    @Test
    void contextLoads() {
    }

}
