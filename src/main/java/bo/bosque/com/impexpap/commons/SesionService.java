package bo.bosque.com.impexpap.commons;

import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.dao.ISesion;
import bo.bosque.com.impexpap.model.Sesion;
import bo.bosque.com.impexpap.security.jwt.DatosToken;
import bo.bosque.com.impexpap.security.jwt.JwtProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Sesiones de la app (trec_sesion): se registran al entrar, la app manda una señal cada 2 minutos y se cierran al
 * salir, por un administrador, por bloqueo o baja, o al cambiar la contraseña. Ningún fallo de este registro
 * impide entrar ni trabajar: sin sql/07 el login sigue como antes, solo que sin sesiones.
 */
@Slf4j
@Service
public class SesionService {

    public static final String MOTIVO_SALIO = "S";
    public static final String MOTIVO_ADMIN = "A";
    public static final String MOTIVO_BLOQUEO = "B";
    public static final String MOTIVO_BAJA = "I";
    public static final String MOTIVO_CLAVE = "R";

    public static final String MSG_CERRADA_ADMIN = "Un administrador cerró tu sesión. Vuelve a iniciar sesión.";
    public static final String MSG_CLAVE_CAMBIADA = "Tu contraseña cambió. Vuelve a iniciar sesión con la nueva.";
    public static final String MSG_CERRADA = "Tu sesión se cerró. Vuelve a iniciar sesión.";
    static final String MSG_PROPIA = "Esa es tu sesión actual. Para cerrarla usa «Cerrar sesión».";
    static final String MSG_FECHAS = "Elige la fecha desde y la fecha hasta.";
    static final String MSG_ORDEN = "La fecha hasta no puede ser anterior a la fecha desde.";
    static final String MSG_RANGO = "Elige un rango de hasta 31 días.";
    static final int DIAS_MAX_HISTORIAL = 31;

    private final ISesion dao;
    private final JwtProvider jwt;

    public SesionService(ISesion dao, JwtProvider jwt) {
        this.dao = dao;
        this.jwt = jwt;
    }

    /** El id para el claim {@code sid}, o 0 si no se pudo registrar: el login no depende de esto. */
    public long iniciar(long codUsuario, String plataforma, String versionApp, String ip) {
        try {
            int minutos = (jwt.getExpiracionSegundos() + 59) / 60;
            return dao.iniciar(codUsuario, plataforma, versionApp, ip, minutos);
        } catch (RuntimeException e) {
            log.warn("No se pudo registrar la sesión del usuario {} (¿falta sql/07?): {}", codUsuario, e.getMessage());
            return 0;
        }
    }

    /** Señal del chequeo periódico. Si falla no pasa nada: la próxima la repone. */
    public void senal(DatosToken datos, String plataforma, String versionApp, String ip) {
        if (datos == null || datos.getIdSesion() <= 0) return;
        try {
            dao.senal(datos.getIdSesion(), datos.getCodUsuario(), plataforma, versionApp, ip);
        } catch (RuntimeException e) {
            log.debug("Señal de la sesión {} sin registrar: {}", datos.getIdSesion(), e.getMessage());
        }
    }

    /** «Cerrar sesión» en la app. */
    public void salir(DatosToken datos) {
        if (datos == null || datos.getIdSesion() <= 0) return;
        try {
            dao.cerrar(datos.getIdSesion(), datos.getCodUsuario(), MOTIVO_SALIO, null);
        } catch (RuntimeException e) {
            log.debug("Salida de la sesión {} sin registrar: {}", datos.getIdSesion(), e.getMessage());
        }
    }

    /** Desde la pantalla Sesiones. Los rechazos del SP (ya cerrada, no existe) llegan como SpBusinessException. */
    public void cerrarPorAdministrador(long idSesion, DatosToken administrador) {
        if (idSesion == administrador.getIdSesion()) throw new SpBusinessException(MSG_PROPIA);
        dao.cerrar(idSesion, 0, MOTIVO_ADMIN, (long) administrador.getCodUsuario());
    }

    /** Bloqueo, baja o cambio de contraseña. Si falla se registra y no corta la operación que lo pidió. */
    public void cerrarTodas(long codUsuario, String motivo, Long audUsuario) {
        cerrarTodas(codUsuario, motivo, audUsuario, 0);
    }

    /** Igual, pero deja abierta {@code exceptoIdSesion}: la sesión desde la que alguien cambia su propia clave. */
    public void cerrarTodas(long codUsuario, String motivo, Long audUsuario, long exceptoIdSesion) {
        try {
            long cerradas = dao.cerrarTodas(codUsuario, motivo, audUsuario, exceptoIdSesion);
            if (cerradas > 0) log.info("Se cerraron {} sesiones del usuario {} (motivo {})", cerradas, codUsuario, motivo);
        } catch (RuntimeException e) {
            log.warn("No se pudieron cerrar las sesiones del usuario {} (motivo {}): {}", codUsuario, motivo, e.getMessage());
        }
    }

    /**
     * El texto del 401 de una sesión que ya no vale. Lo decide p_list_Usuario 'B' con el sid del token (06): sin
     * motivo es que la sesión no existe o es de otro usuario (p. ej. si trec_sesion se recreó y volvió a numerar).
     */
    public static String mensajeDeCierre(String motivo) {
        if (MOTIVO_ADMIN.equals(motivo)) return MSG_CERRADA_ADMIN;
        if (MOTIVO_CLAVE.equals(motivo)) return MSG_CLAVE_CAMBIADA;
        return MSG_CERRADA;
    }

    /** Las abiertas con el token vigente; la del administrador que consulta queda marcada. */
    public List<Sesion> abiertas(DatosToken quien) {
        List<Sesion> filas = dao.abiertas();
        for (Sesion s : filas) s.setEsActual(quien != null && quien.getIdSesion() > 0 && s.getIdSesion() == quien.getIdSesion());
        return filas;
    }

    /** {@code desde} y {@code hasta} son días completos (llegan a las 00:00): el SP recibe [desde, hasta + 1 día). */
    public List<Sesion> historial(Date desde, Date hasta, Long codUsuario) {
        if (desde == null || hasta == null) throw new SpBusinessException(MSG_FECHAS);
        if (hasta.before(desde)) throw new SpBusinessException(MSG_ORDEN);
        long dias = TimeUnit.MILLISECONDS.toDays(hasta.getTime() - desde.getTime()) + 1;
        if (dias > DIAS_MAX_HISTORIAL) throw new SpBusinessException(MSG_RANGO);
        Date hastaExclusivo = new Date(hasta.getTime() + TimeUnit.DAYS.toMillis(1));
        return dao.historial(desde, hastaExclusivo, codUsuario != null && codUsuario > 0 ? codUsuario : null);
    }
}
