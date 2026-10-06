// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dao\IVerificarCierreOperaciones.java
package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.utils.RespuestaSp;

public interface IVerificarCierreOperaciones {
    /** fueRevisado 1 = visto bueno (ACCION 'V'); 0 = quitarlo (ACCION 'Q', archivo SQL 71). */
    RespuestaSp marcarArqueoRevisado(long idAC, int fueRevisado, long audUsuario);

    RespuestaSp marcarLlegadaVerificada(long idRp, int fueVerificado, long audUsuario);

    RespuestaSp confirmarVerificacion(long idBitTarea, long audUsuario);

    /**
     * Resuelve la sucursal vigente del supervisor dueño de esta ocurrencia
     * (idBitTarea -&gt; tac_bitTareaRuti.codEmpleado -&gt; cargo vigente en
     * trh_empleadoCargo -&gt; tb_cargo_sucursal), el mismo join que ya usa
     * p_list_tac_ArqueoCajaSucursales (ACCION='B'/'H') para saber a quién
     * pertenece un idBitTarea — nunca de la sesión de quien llama (ver
     * memoria "resolve context from record, not session"). Devuelve 0
     * (todas las sucursales) si {@code todasSucursales}=true, si el
     * idBitTarea no existe, o si el empleado no tiene un cargo vigente —
     * el mismo significado de 0/NULL que ya usan p_list_arqueoCajaSucursales
     * y p_list_llegada para "sin filtro de sucursal".
     */
    int resolverCodSucursal(long idBitTarea, boolean todasSucursales);
}
