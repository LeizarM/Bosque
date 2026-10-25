package bo.bosque.com.impexpap.security;

import bo.bosque.com.impexpap.dao.ILoginDao;
import bo.bosque.com.impexpap.security.jwt.DatosToken;
import bo.bosque.com.impexpap.security.jwt.JwtProvider;
import bo.bosque.com.impexpap.security.jwt.UsuarioToken;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Seguridad del STOMP de todos los módulos: autentica el CONNECT con el JWT de la API (como JwtTokenFilter),
 * rechaza todo SEND (los clientes solo escuchan; si no, cualquiera difundiría datos falsos) y delega cada
 * SUBSCRIBE en las ReglaSuscripcion de los módulos. Un destino sin regla se rechaza.
 */
@Component
public class StompSeguridad implements ChannelInterceptor {

    private static final String PREFIJO_BEARER = "Bearer ";
    private static final List<String> ROLES = Arrays.asList("ROLE_ADM", "ROLE_LIM");

    private final JwtProvider jwt;
    private final ILoginDao logins;
    private final Supplier<List<ReglaSuscripcion>> reglas;

    /** Las reglas se leen de los beans en cada SUBSCRIBE: un módulo nuevo solo registra la suya. */
    @Autowired
    public StompSeguridad(JwtProvider jwt, ILoginDao logins, ObjectProvider<ReglaSuscripcion> reglas) {
        this(jwt, logins, () -> reglas.orderedStream().collect(Collectors.toList()));
    }

    /** Con reglas fijas (pruebas). */
    public StompSeguridad(JwtProvider jwt, ILoginDao logins, List<ReglaSuscripcion> reglas) {
        this(jwt, logins, () -> reglas);
    }

    private StompSeguridad(JwtProvider jwt, ILoginDao logins, Supplier<List<ReglaSuscripcion>> reglas) {
        this.jwt = jwt;
        this.logins = logins;
        this.reglas = reglas;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor acc = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (acc == null || acc.getMessageType() == null) return message;
        switch (acc.getMessageType()) {
            case CONNECT:
                acc.setUser(autenticar(acc.getFirstNativeHeader(HttpHeaders.AUTHORIZATION)));
                break;
            case SUBSCRIBE:
                autorizar(acc.getUser(), acc.getDestination());
                break;
            case MESSAGE:
                throw new AccessDeniedException("El tiempo real solo se escucha: no admite envíos.");
            default:
                break;
        }
        return message;
    }

    /** Mismo armado que JwtTokenFilter: firma, usuario activo y sesión abierta; el postulante no entra. */
    Authentication autenticar(String header) {
        String token = header != null && header.startsWith(PREFIJO_BEARER)
                ? header.substring(PREFIJO_BEARER.length()).trim() : "";
        Claims claims = token.isEmpty() ? null : jwt.leer(token);
        if (claims == null || JwtProvider.esPostulante(claims)) {
            throw new AccessDeniedException("Falta un token válido en el CONNECT.");
        }
        DatosToken datos = jwt.datosDe(claims);
        UsuarioToken usuario = logins.cargarParaToken(claims.getSubject(), datos.getIdSesion());
        if (usuario == null || !usuario.isEnabled() || !usuario.isAccountNonLocked() || usuario.isSesionCerrada()) {
            throw new AccessDeniedException("La sesión del token ya no es válida.");
        }
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
        auth.setDetails(datos);
        if (auth.getAuthorities().stream().noneMatch(a -> ROLES.contains(a.getAuthority()))) {
            throw new AccessDeniedException("El usuario no tiene un rol habilitado.");
        }
        return auth;
    }

    /** Pasa si al menos una regla cubre el destino y todas las que lo cubren lo permiten. */
    void autorizar(Principal usuario, String destino) {
        if (!(usuario instanceof Authentication) || destino == null) {
            throw new AccessDeniedException("Destino no permitido.");
        }
        Authentication auth = (Authentication) usuario;
        List<ReglaSuscripcion> aplicables = reglas.get().stream()
                .filter(r -> r.aplica(destino)).collect(Collectors.toList());
        if (aplicables.isEmpty()) {
            throw new AccessDeniedException("Destino no permitido.");
        }
        if (!aplicables.stream().allMatch(r -> r.permite(auth, destino))) {
            throw new AccessDeniedException("No tiene habilitada la pantalla de este tópico.");
        }
    }
}
