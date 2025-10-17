package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.SociosTigo;
import bo.bosque.com.impexpap.model.TigoEjecutado;

import java.util.List;

public interface ITigoEjecutado {

    /**
     * Procecimiento para obtener el TOTAL COBRADO X CUENTA
     */
    List<TigoEjecutado> obtenerTotalCobradoXCuenta(String periodoCobrado);
    /**
     * Procedimiento para insertar anticipos
     */
    boolean generarAnticiposTigo (String periodoCobrado);
    /**
     * obtener resumen cuentas consumo tigo
     */
    List<TigoEjecutado> obtenerResumenCuentas(String periodoCobrado);
    /**
     * obtener resumen DETALLADO X CUENTA TIGO
     */
    List<TigoEjecutado> obtenerDetalleXCuentas(String periodoCobrado);
    /**
     * insertar tigoejecutado
     */
    boolean registrarTigoEjecutado(TigoEjecutado te,String acc);
    /**
     * Procecimiento para obtener tigo ejecutado
     */
    List<TigoEjecutado> obtenerTigoEjecutado(String empresa, String periodoCobrado);
    /**
     * Procecimiento para obtener EL ARBOL DETALLADO
     */
    List<TigoEjecutado> obtenerArbolDetallado(String empresa,String periodoCobrado);

}
