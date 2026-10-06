package bo.bosque.com.impexpap.dao;

import java.util.List;

import bo.bosque.com.impexpap.dto.AccionChequeDto;
import bo.bosque.com.impexpap.model.ChAccion;
import bo.bosque.com.impexpap.utils.RespuestaSp;

/** Acceso a {@code tch_accion} por {@code p_list_Accion} y {@code p_abm_Accion}. */
public interface IChAccion {

    /** {@code p_list_Accion} 'L': todo el historial del cheque, por fecha. {@code descripcion} se completa en Java. */
    List<AccionChequeDto> listarPorCheque(int codCheque);

    /** Una accion por su codigo, o {@code null}. */
    ChAccion obtener(int codAccion);

    /** {@code p_list_Accion} 'F': cuantas acciones tiene el cheque ({@code contRegPorChks}). */
    int contarPorCheque(int codCheque);

    /**
     * {@code p_list_Accion} 'G': cuantas acciones {@code TRASP} tiene el cheque. El legacy
     * ({@code verifTraspCheq}) exige que sea <b>exactamente 1</b>.
     */
    int traspasosDelCheque(int codCheque);

    /**
     * {@code p_list_Accion} 'H': si hay tantas devoluciones como entregas a cobranza
     * ({@code verifCheqDevuelto}, que exige que el SP devuelva 1).
     */
    boolean devolucionesIgualAEntregas(int codCheque);

    /**
     * {@code p_list_Accion} 'C': cheques de la sucursal con una sola accion, es decir, los que todavia
     * no se traspasaron. <b>Recibe la sucursal en {@code @codAccion}</b>.
     */
    int pendientesDeTraspaso(long codSucursal);

    /** {@code p_abm_Accion} 'I'. Devuelve el id en {@code idGenerado}. */
    RespuestaSp registrar(ChAccion accion);

    /** {@code p_abm_Accion} 'D'. */
    RespuestaSp eliminar(int codAccion, int audUsuario);

    /**
     * {@code p_abm_Accion} 'A': copia una accion existente a otro cheque (misma fecha, estado,
     * responsable y observacion). Es el "Dar Custodia" del administrador.
     */
    RespuestaSp copiar(int codAccionOrigen, int codChequeDestino, int audUsuario);

    /**
     * {@code p_list_Accion} 'E': traspaso masivo. Inserta {@code TRASP} en <b>todos</b> los cheques de
     * la sucursal que tienen exactamente una accion. {@code idGenerado} = cuantos cheques traspaso.
     */
    RespuestaSp traspasar(long codSucursal, int audUsuario);
}
