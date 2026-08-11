package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.PermisoSabadoDto;
import bo.bosque.com.impexpap.dto.PuenteVacacionDto;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

/**
 * El cruce entre las vacaciones y permisos de RR.HH. ({@code trh_permiso}) y los
 * sábados del rol.
 *
 * <p>SP: {@code p_list_trs_PermisoSabado} para leer, y
 * {@code trs_sp_puenteVacacion} para el puente.
 *
 * <p><b>Los permisos sueltos NO se cargan desde este módulo</b>: se cargan en RR.HH. y
 * acá sólo se leen y se aplican con {@code trs_sp_refrescarPermisos}. La única
 * excepción es el PUENTE, y lo es por una razón concreta: es un alta en lote para
 * decenas de personas que nace de una decisión de empresa, y el circuito de solicitudes
 * está construido para el caso contrario — una persona pidiendo, dos firmas aprobando.
 */
public interface IPermisoSabado {

    /**
     * [L] Todos los permisos que caen en un sábado del rol, con la letra que
     * tiene la celda hoy y la que le correspondería.
     *
     * @param codEmpleado 0 = todos
     */
    List<PermisoSabadoDto> obtenerPermisosDeSabado(long idRol, long codEmpleado);

    /**
     * [D] Sólo los DESFASADOS: los que la grilla todavía no refleja.
     *
     * <p>Deja fuera lo que alguien decidió a mano ({@code origen='M'}) y los
     * feriados, porque ninguno de los dos es un error: en el primero una persona
     * decidió que sí viene, y en el segundo la sucursal no abre igual.
     */
    List<PermisoSabadoDto> obtenerDesfases(long idRol);

    // ══════════════════════════════════════════════════════════════════════
    // PUENTE A CUENTA DE VACACIÓN
    // ══════════════════════════════════════════════════════════════════════

    /**
     * [S] Qué pasaría si se declarara el puente: una fila por persona, con los días
     * que se le descontarían y, para quien se saltea, el motivo.
     *
     * <p><b>No escribe nada.</b> El SP hace RETURN antes de cualquier transacción, así
     * que esto es el dry-run que alimenta el mensaje de confirmación.
     *
     * @param codEmpleadoEjecutor quién lo pide, resuelto del token
     * @param esAdmin             true si el token trae ROLE_ADM
     */
    List<PuenteVacacionDto> simularPuente(PuenteVacacionDto p,
                                          long codEmpleadoEjecutor, boolean esAdmin);

    /**
     * [A] Lo aplica: da de alta el permiso de vacación de todos los que ese sábado
     * tenían que venir y deja sus celdas en 'V'.
     *
     * <p><b>Escribe en {@code trh_permiso}, que es de RR.HH.</b>, y le consume saldo de
     * vacación a cada persona. Sólo ROLE_ADM o quien esté en {@code trs_Rrhh}: un jefe
     * programador no puede, aunque sí pueda corregir la celda de su gente — un puente
     * es una decisión de empresa, no de equipo.
     *
     * <p>Es idempotente en lo que importa: quien ya tenga un permiso que se cruce con
     * ese horario se saltea. {@code p_abm_Permiso} no valida nada, así que sin ese
     * chequeo correrlo dos veces duplicaría el descuento de todos.
     *
     * @param codUsuarioAutorizador el {@code tb_usuario.codUsuario} del que declara
     * @return el mensaje del SP y, en {@code idGenerado}, cuántos permisos se crearon
     */
    RespuestaSp aplicarPuente(PuenteVacacionDto p, long codUsuarioAutorizador,
                              long codEmpleadoEjecutor, boolean esAdmin, long audUsuario);
}
