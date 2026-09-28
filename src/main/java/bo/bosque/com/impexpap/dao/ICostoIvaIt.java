package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.CostoIvaItDto;
import bo.bosque.com.impexpap.model.CostoIvaIt;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

/**
 * Acceso a dbo.tpr_costoIvaIt (IVA e IT del modulo de Precios).
 * Todo pasa por procedimiento almacenado: {@code p_abm_costoIvaIt} y
 * {@code p_list_costoIvaIt}.
 */
public interface ICostoIvaIt {

    /**
     * Registra, actualiza o elimina la fila de IVA/IT.
     * Invoca {@code p_abm_costoIvaIt} con la ACCION recibida.
     *
     * @param mb  datos de la fila (los campos null en 'U' no se pisan)
     * @param acc 'I' alta (falla si ya existe una fila: la tabla es singleton),
     *            'U' modificacion, 'D' baja (falla si es la ultima fila)
     * @return error, errormsg e idGenerado devueltos por el SP
     */
    RespuestaSp registrarCostoIvaIt(CostoIvaIt mb, String acc);

    /**
     * Todas las filas de tpr_costoIvaIt, de la mas nueva a la mas vieja.
     * Invoca {@code p_list_costoIvaIt} ACCION 'L' sin filtros.
     * En una base sana devuelve exactamente una fila; mas de una es la senal
     * de que el invariante de fila unica se rompio.
     */
    List<CostoIvaIt> listarCostoIvaIt();

    /**
     * Una fila por PK. Invoca {@code p_list_costoIvaIt} ACCION 'L' filtrando
     * por @idCII.
     *
     * @param idCII PK buscada
     * @return la fila, o null si no existe
     */
    CostoIvaIt obtenerPorId(int idCII);

    /**
     * Filas asociadas a una propuesta. Invoca {@code p_list_costoIvaIt}
     * ACCION 'L' filtrando por @idPropuesta.
     *
     * @param idPropuesta id de la propuesta
     */
    List<CostoIvaIt> listarPorPropuesta(long idPropuesta);

    /**
     * La fila de IVA/IT vigente, de forma determinista (mayor idCII), con
     * {@code totalIvaIt = iva + it} ya sumado por el SP.
     * Invoca {@code p_list_costoIvaIt} ACCION 'V'.
     *
     * @return la fila vigente, o null si la tabla esta vacia
     */
    CostoIvaItDto obtenerVigente();
}
