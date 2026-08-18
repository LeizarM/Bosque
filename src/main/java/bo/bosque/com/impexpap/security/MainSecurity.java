package bo.bosque.com.impexpap.security;

import bo.bosque.com.impexpap.dao.LoginDaoImpl;
import bo.bosque.com.impexpap.security.jwt.JwtEntryPoint;
import bo.bosque.com.impexpap.security.jwt.JwtTokenFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuración de seguridad.
 *
 * <p><b>prePostEnabled = true</b> — sin este flag, {@code securedEnabled} sólo activa
 * {@code @Secured}, y las ~50 anotaciones {@code @PreAuthorize} del proyecto quedaban
 * <b>inertes</b>: Spring ni las miraba. Los endpoints anotados con {@code @PreAuthorize}
 * eran alcanzables por cualquier usuario autenticado, sin importar su rol.
 *
 * <p>Es seguro activarlo porque las dos familias usan los MISMOS nombres de rol
 * ({@code ROLE_ADM}, {@code ROLE_LIM}), y son exactamente los que concede
 * {@code p_list_Usuario @ACCION='B'} con {@code 'ROLE_'+UPPER(tipoUsuario)}. Los 134
 * usuarios de la base son {@code adm} o {@code lim}, o sea que todos entran en uno de
 * los dos. {@code hasAnyRole('ROLE_ADM')} no duplica el prefijo: Spring lo antepone
 * sólo si el nombre no lo trae ya.
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true)
public class MainSecurity extends WebSecurityConfigurerAdapter {

    private final JwtEntryPoint jwtEntryPoint;
    private final LoginDaoImpl loginImpl;
    private final SecurityFilter securityFilter;
    private final ClienteIp clienteIp;

    public MainSecurity(JwtEntryPoint jwtEntryPoint, LoginDaoImpl loginImpl,
                        SecurityFilter securityFilter, ClienteIp clienteIp) {
        this.jwtEntryPoint = jwtEntryPoint;
        this.loginImpl = loginImpl;
        this.securityFilter = securityFilter;
        this.clienteIp = clienteIp;
    }

    @Bean
    public JwtTokenFilter jwtTokenFilter() {
        return new JwtTokenFilter();
    }

    @Bean
    public RateLimitFilter customRateLimitFilter() {
        // Se le pasa ClienteIp para que el cupo sea por IP REAL. Con la version
        // anterior bastaba con mandar un X-Forwarded-For distinto en cada intento
        // para saltarse el limite de 5 por minuto del login.
        return new RateLimitFilter(clienteIp);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(this.loginImpl).passwordEncoder(passwordEncoder());
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // Configuración de seguridad
        http.cors().and().csrf().disable()
                .authorizeRequests()
                .antMatchers("/actuator/prometheus").permitAll()  // Sin ** al final
                .antMatchers("/actuator/**").permitAll()          // Permitir todos los endpoints de actuator
                .antMatchers("/auth/**").permitAll()
                /* /pagos-extranjeros/** estaba en permitAll: los 60 endpoints del
                   modulo TPEX se alcanzaban SIN token. Se saca y cae en
                   anyRequest().authenticated(); el @PreAuthorize de clase de
                   PagosExtranjerosController ya limita por rol ahora que
                   prePostEnabled esta activo.
                   La app Flutter no se ve afectada: su interceptor adjunta el
                   Bearer a todo menos al login, incluida la descarga del voucher. */
                .antMatchers("/fichaTrabajador/uploads/img/**").permitAll()
                .antMatchers("/fichaTrabajador/uploads/documentos/**").permitAll()
                .antMatchers("/fichaTrabajador/uploads/pendientes/**").permitAll()
                .antMatchers("/fichaTrabajador/resources/reports/**").permitAll()
                .antMatchers("/tigo/uploads/facturasTigo/**").permitAll()
                /* Webhook de openWA. Tiene que ser permitAll porque openWA no puede
                   mandar un JWT: se autentica con la firma HMAC-SHA256 del body contra
                   openwa.webhook.secret, que verifica WhatsAppWebhookController. Va ANTES
                   de anyRequest().authenticated(); puesto despues no tendria efecto y el
                   sintoma seria un 401 del JwtEntryPoint que parece un problema de openWA.
                   Solo esta ruta exacta, no /whatsapp/**. */
                .antMatchers(HttpMethod.POST, "/whatsapp/webhook").permitAll()
                .antMatchers("/rrhh/eliminarFoto").authenticated()
                .anyRequest().authenticated()
                .and()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .exceptionHandling().authenticationEntryPoint(jwtEntryPoint);

        // Añadir filtros en orden de ejecución
        // 1. Filtro de seguridad para detectar ataques
        http.addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class);
        // 2. Filtro de rate limiting
        http.addFilterBefore(customRateLimitFilter(), UsernamePasswordAuthenticationFilter.class);
        // 3. Filtro JWT
        http.addFilterBefore(jwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);
    }
}