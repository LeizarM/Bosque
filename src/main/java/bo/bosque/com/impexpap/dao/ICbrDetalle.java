package bo.bosque.com.impexpap.dao;

import java.util.List;

import bo.bosque.com.impexpap.dto.DetalleReciboRptDto;
import bo.bosque.com.impexpap.model.CbrDetalle;
import bo.bosque.com.impexpap.utils.RespuestaSp;

/**
 * Acceso a <b>tcbr_cbrDetalle</b>. Procedimientos: {@code p_abm_CbrDetalle} (I, U, D) y
 * {@code p_list_CbrDetalle} (L, A).
 */
public interface ICbrDetalle {

    /** Detalles de una garantia. LIST ACCION 'L'. */
    List<CbrDetalle> listarPorGarantia(long codGarantia);

    /** Un detalle, o {@code null} si no existe. LIST ACCION 'L'. */
    CbrDetalle obtener(long codDetalle);

    /** Detalles con la descripcion del tipo, para el subreporte del recibo. LIST ACCION 'A'. */
    List<DetalleReciboRptDto> listarParaRecibo(long codGarantia);

    /** Alta (I) o cambio de monto (U, lo unico que el procedimiento deja editar). */
    RespuestaSp registrar(CbrDetalle detalle, String accion);

    /** Baja fisica (D). El trigger dad_cbrDetalle copia la fila a la papelera. */
    RespuestaSp eliminar(long codDetalle, int audUsuario);
}
