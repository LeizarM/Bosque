package bo.bosque.com.impexpap.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Encoder propio de {@code tcpe_usuario.password2}, aparte del de {@code MainSecurity} para cambiar el costo de los
 * postulantes sin tocar el login de los empleados. Aquel es {@code @Primary}: este se pide con
 * {@code @Qualifier(TcpeSeguridadConfig.ENCODER)}.
 */
@Configuration
public class TcpeSeguridadConfig {

    public static final String ENCODER = "tcpePasswordEncoder";

    /** Costo 14 (~1 s por hash): por eso los lotes se hashean en paralelo. */
    public static final int COSTO_BCRYPT = 14;

    @Bean(name = ENCODER)
    public PasswordEncoder tcpePasswordEncoder() {
        return new BCryptPasswordEncoder(COSTO_BCRYPT);
    }
}
