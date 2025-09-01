package bo.bosque.com.impexpap.security;

import bo.bosque.com.impexpap.dao.LoginDaoImpl;
import bo.bosque.com.impexpap.security.jwt.JwtEntryPoint;
import bo.bosque.com.impexpap.security.jwt.JwtTokenFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true)
public class MainSecurity extends WebSecurityConfigurerAdapter {

    private final JwtEntryPoint jwtEntryPoint;
    private final LoginDaoImpl loginImpl;
    private final SecurityFilter securityFilter;

    public MainSecurity(JwtEntryPoint jwtEntryPoint, LoginDaoImpl loginImpl, SecurityFilter securityFilter) {
        this.jwtEntryPoint = jwtEntryPoint;
        this.loginImpl = loginImpl;
        this.securityFilter = securityFilter;
    }

    @Bean
    public JwtTokenFilter jwtTokenFilter() {
        return new JwtTokenFilter();
    }

    @Bean
    public RateLimitFilter customRateLimitFilter() {
        return new RateLimitFilter();
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
                .antMatchers("/auth/**").permitAll()
                .antMatchers("/fichaTrabajador/uploads/img/**").permitAll()
                .antMatchers("/fichaTrabajador/uploads/documentos/**").permitAll()
                .antMatchers("/fichaTrabajador/uploads/pendientes/**").permitAll()
                .antMatchers("/fichaTrabajador/resources/reports/**").permitAll()
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