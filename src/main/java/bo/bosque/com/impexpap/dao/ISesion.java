package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Sesion;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.Date;
import java.util.List;

/** Sesiones de la app (trec_sesion). Los errores de negocio del SP llegan como SpBusinessException. */
public interface ISesion {

    /** @return el idSesion generado */
    long iniciar(long codUsuario, String plataforma, String versionApp, String ip, int minutosVigencia);

    void senal(long idSesion, long codUsuario, String plataforma, String versionApp, String ip);

    /** Una sesión; {@code codUsuario} es el dueño salvo con motivo A, donde {@code audUsuario} es el administrador. */
    RespuestaSp cerrar(long idSesion, long codUsuario, String motivo, Long audUsuario);

    /** Todas las abiertas del usuario salvo {@code exceptoIdSesion} (0 = ninguna). @return cuántas cerró */
    long cerrarTodas(long codUsuario, String motivo, Long audUsuario, long exceptoIdSesion);

    List<Sesion> abiertas();

    List<Sesion> historial(Date desde, Date hasta, Long codUsuario);
}
