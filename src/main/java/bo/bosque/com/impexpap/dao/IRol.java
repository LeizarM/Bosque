package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.IntervencionRolDto;
import bo.bosque.com.impexpap.model.Rol;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

/**
 * Cabecera anual del Rol de Turnos de Sábado.
 * SPs: {@code p_abm_trs_Rol} · {@code p_list_trs_Rol}.
 */
public interface IRol {

    /**
     * Registra, actualiza o elimina un rol.
     *
     * <p><b>'I' no inserta una fila suelta:</b> el SP delega en
     * {@code trs_sp_generarRol}, que además arma los ~52 sábados y todos los
     * participantes con su grupo A/B. Sólo hacen falta {@code anio} y el alcance.
     *
     * <p><b>'D' borra en cascada</b> las 6 tablas que cuelgan del rol, en orden de FKs.
     *
     * @param rol Datos del rol
     * @param acc 'I' | 'U' | 'D'
     */
    RespuestaSp registrarRol(Rol rol, String acc);

    /** [L] Lista las cabeceras, con sucursal y contadores de sábados/participantes/celdas. */
    List<Rol> obtenerRoles(long codSucursal, int anio);

    /** [L] Devuelve un rol por su id. */
    Rol obtenerRolPorId(long idRol);

    /**
     * [I] Todo lo que un humano tocó sobre el horario por defecto, de las cuatro
     * fuentes posibles. Es lo que reemplazó a la conformidad: pocas filas = el
     * default está bien calibrado.
     * @param idRol 0 = todos los roles
     */
    List<IntervencionRolDto> obtenerIntervenciones(long idRol);
}
