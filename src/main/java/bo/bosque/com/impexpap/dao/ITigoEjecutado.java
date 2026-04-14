package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.CambiosTigo;
import bo.bosque.com.impexpap.model.SociosTigo;
import bo.bosque.com.impexpap.model.TigoEjecutado;
import bo.bosque.com.impexpap.utils.RespuestaSp;

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
    List<TigoEjecutado> obtenerTigoEjecutado(String empresa, String periodoCobrado,String search);
    /**
     * Procecimiento para obtener EL ARBOL DETALLADO
     */
    List<TigoEjecutado> obtenerArbolDetallado(String empresa,String periodoCobrado,String search);

    /**
     * Procedimiento para ejecutar por lotes
     * @param te
     * @return
     */
    boolean actualizarEmpresaLote(TigoEjecutado te);
    /**
     * NUEVO — Ejecutar periodo completo (ACCION='E')
     * Unifica ACCION='B' (anticipos) + ACCION='G' (tTigo_ejecutado)
     * en una sola transaccion con validaciones y mensajes estructurados.
     *
     * @param te Objeto con periodoCobrado y audUsuarioI obligatorios
     * @return RespuestaSp con error, errormsg e idGenerado (total registros procesados)
     */
    RespuestaSp ejecutarPeriodo(TigoEjecutado te);
    /**
     * PARA EL DROPDOWN DE EMPRESAS : INCLUYE TODAS LAS EMPRESAS EXCEPTO PAPIRUS UNIFICADO A SOCIOS TOMANDO EL NOMBRECOMPLETO COMO EMPRESA.
     */
    List <TigoEjecutado> listarEmpresas();



}
