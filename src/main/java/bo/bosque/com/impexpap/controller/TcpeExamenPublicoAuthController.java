package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.commons.TcpeClaveService;
import bo.bosque.com.impexpap.dao.ITcpeAcceso;
import bo.bosque.com.impexpap.dao.ITcpeRendir;
import bo.bosque.com.impexpap.dto.TcpeAccesoDto;
import bo.bosque.com.impexpap.dto.TcpeLoginPostulanteDto;
import bo.bosque.com.impexpap.dto.TcpeSesionPostulanteDto;
import bo.bosque.com.impexpap.security.jwt.JwtProvider;
import bo.bosque.com.impexpap.utils.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Date;

/**
 * Login público del postulante: valida con {@code Acceso 'L'}, cierra sus vencidos y ausentes ({@code Rendir 'V'}) y,
 * si SQL lo deja ingresar ({@code puedeIngresar}), emite un token {@code ROLE_POSTULANTE}. Una clave legacy en texto
 * plano se rehashea solo si el ingreso es válido. El límite por IP lo pone {@code RateLimitFilter}.
 */
@RestController
@CrossOrigin(originPatterns = "${tcpe.cors.origenes:*}", methods = {RequestMethod.POST})
@RequestMapping("/examen-publico/auth")
public class TcpeExamenPublicoAuthController {

    private static final Logger log = LoggerFactory.getLogger(TcpeExamenPublicoAuthController.class);

    static final String MSG_CREDENCIALES = "Usuario o contraseña incorrectos.";

    /** {@code tcpe_tipoUsuario} de los postulantes externos (el que les da {@code p_abm_tcpe_GenerarExamen}). */
    static final long TIPO_EXTERNO = 2L;

    private final ITcpeAcceso acceso;
    private final ITcpeRendir rendir;
    private final TcpeClaveService claves;
    private final JwtProvider jwt;

    public TcpeExamenPublicoAuthController(ITcpeAcceso acceso, ITcpeRendir rendir, TcpeClaveService claves,
                                           JwtProvider jwt) {
        this.acceso = acceso;
        this.rendir = rendir;
        this.claves = claves;
        this.jwt = jwt;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<?>> login(@Valid @RequestBody TcpeLoginPostulanteDto dto) {
        String login = dto.getLogin().trim();
        TcpeAccesoDto u = acceso.porLogin(login);

        // Mismo 401 para login inexistente, de otro tipo y clave errada: entre esos casos no se distingue.
        // p_list_tcpe_Acceso 'L' no filtra por tipo: solo los externos (tipo 2) entran por aquí.
        if (u == null || u.getCodUsuario() == null || !Long.valueOf(TIPO_EXTERNO).equals(u.getCodTipoUsuario())
                || !claves.coincide(dto.getPassword2(), u.getPassword2())) {
            log.warn("Login de examen rechazado para '{}'", login);
            return TcpeRespuestas.error(HttpStatus.UNAUTHORIZED, MSG_CREDENCIALES);
        }
        // Solo sus vencidos y ausentes (detalles v4). No cambia puedeIngresar: VENCIDO y lo entregado también habilitan.
        entregarVencidos(u.getCodUsuario());
        // Contrato 7.4: si ingresa (activo, examen habilitante) y con qué mensaje lo decide SQL; nulo no habilita.
        // Se informa recién con la clave correcta, para no revelar qué logins existen.
        if (!Boolean.TRUE.equals(u.getPuedeIngresar())) {
            log.info("Login de examen no habilitado para '{}'", login);
            return TcpeRespuestas.error(HttpStatus.UNAUTHORIZED, u.getMensajeIngreso());
        }
        if (!TcpeClaveService.esHash(u.getPassword2())) {
            migrarClave(u.getCodUsuario(), dto.getPassword2());
        }

        Date expira = jwt.expiracionDesdeAhora();
        String token = jwt.generarTokenPostulante(u.getCodUsuario(), u.getLogin(), expira);
        return TcpeRespuestas.ok(new TcpeSesionPostulanteDto(token, expira, u.getCodUsuario(), u.getLogin(),
                Boolean.TRUE.equals(u.getDatosCompletos()), u.getNombres()));
    }

    /** {@code Rendir 'V'} con {@code @codUsuario}; si falla, se registra y el login sigue (el SP hace COMMIT). */
    private void entregarVencidos(long codUsuario) {
        try {
            rendir.entregarVencidosDe(codUsuario);
        } catch (RuntimeException e) {
            log.error("No se pudieron cerrar los exámenes vencidos antes del login: {}", e.getMessage(), e);
        }
    }

    private void migrarClave(long codUsuario, String plano) {
        try {
            acceso.guardarHash(codUsuario, claves.hash(plano), null);
        } catch (RuntimeException e) {
            log.error("No se pudo migrar a BCrypt la clave del tcpe_usuario {}: {}", codUsuario, e.getMessage());
        }
    }
}
