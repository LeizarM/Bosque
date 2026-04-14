package bo.bosque.com.impexpap.dao;
import bo.bosque.com.impexpap.model.FacturaTigo;
import bo.bosque.com.impexpap.model.TigoEjecutado;

import java.util.List;

public interface IFacturaTigo {
    /**
     * Procedimiento que REGISTRA EL DETALLE DEUDA TIGO
     * @param ft
     * @return
     */
    boolean registrarFacturaTigo (FacturaTigo ft, String acc);
    /**
     * Procecimiento para obtener el detalle deuda TIGO
     * @param
     * @return
     */
    List<FacturaTigo> obtenerDetalleDeudaTigo( );
    /**
     * OBTIENE LISTA DE PERIODOCOBRADO NO EJECUTADO
     * @return
     */
    List <FacturaTigo> listarPeriodoFactura();


}
