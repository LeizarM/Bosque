package bo.bosque.com.impexpap.dao;

import java.util.List;

import bo.bosque.com.impexpap.model.AccionCbr;
import bo.bosque.com.impexpap.utils.RespuestaSp;

/**
 * Acceso a <b>tcbr_accion</b>. Procedimientos: {@code p_abm_AccionCbr} (I, U, D y T, esta
 * ultima agregada en tcbr_01) y {@code p_list_AccionCbr} (L, A, B, C, D).
 *
 * <p>Ojo: la rama 'D' de {@code p_list_AccionCbr} <b>escribe</b> (es el traspaso del JSF).
 * Desde Spring el traspaso va por {@link #traspasar(int)}, que usa la rama 'T' del ABM con
 * la misma regla.
 */
public interface IAccionCbr {

    /** Acciones de una garantia. LIST ACCION 'L'. */
    List<AccionCbr> listarPorGarantia(long codGarantia);

    /** Una accion, o {@code null} si no existe. LIST ACCION 'L'. */
    AccionCbr obtener(long codAccion);

    /** Garantias que todavia esperan su traspaso (tienen una sola accion). LIST ACCION 'B'. */
    long traspasosPendientes();

    /** Alta (I) o cambio de observacion (U, lo unico que el procedimiento deja editar). */
    RespuestaSp registrar(AccionCbr accion, String accionAbm);

    /** Baja fisica (D). */
    RespuestaSp eliminar(long codAccion, int audUsuario);

    /** Traspasa todas las pendientes. ABM ACCION 'T'; {@code idGenerado} = cantidad. */
    RespuestaSp traspasar(int audUsuario);
}
