package bo.bosque.com.impexpap.dao;


import bo.bosque.com.impexpap.dto.EntregaNotificacionDTO;
import bo.bosque.com.impexpap.dto.PedidoPendienteEntregaDTO;
import bo.bosque.com.impexpap.model.EntregaChofer;

import java.util.Date;
import java.util.List;

public interface IEntregaChofer {

    /**
     * Registrar entrega de chofer
     * @param mb
     * @param acc
     * @return
     */
    boolean registrarEntregaChofer( EntregaChofer mb, String acc );

    /**
     * Como {@link #registrarEntregaChofer} pero devuelve el conteo de filas.
     *
     * <p>El booleano colapsa dos cosas distintas: "el SP corrio y no encontro nada que
     * actualizar" (0) y "el SP exploto" (-1). Mientras la ACCION 'B' actualizaba por
     * (docEntry, db) sin exigir que la fila estuviera pendiente daba lo mismo; al agregarle
     * {@code AND ISNULL(fueEntregado,0)=0}, re-marcar una entrega ya marcada pasa a afectar 0
     * filas, y con el booleano eso se traducia en un 500 sobre una entrega bien registrada.
     *
     * @return filas afectadas, o -1 si hubo excepcion
     */
    int registrarEntregaChoferFilas( EntregaChofer mb, String acc );

    /**
     * Listar entregas por empleado
     * @param codEmpleado
     * @return
     */
    List<EntregaChofer> listarEntregasXEmpleado(  int codEmpleado );

    /**
     * Lo MISMO que {@link #listarEntregasXEmpleado(int)} pero SIN sincronizar con SAP
     * (SP {@code p_list_trch_Entregas} ACCIÓN 'Y').
     *
     * <p>La ACCIÓN 'A' que usa el método de arriba hace dos cosas: primero sincroniza
     * {@code trch_Entregas} entera contra SAP por el linked server (~1,4 s medidos, 417 filas de
     * 4 empresas) y recién después filtra los pendientes de UN chofer. La 'Y' es solo la segunda
     * mitad: el mismo {@code SELECT}, las mismas columnas en el mismo orden, sin la sincronización.
     *
     * <p>Quién sincroniza entonces: {@code commons.SincronizacionEntregasService}, como mucho una
     * vez cada N segundos y un solo hilo a la vez.
     *
     * <p><b>NUNCA lanza.</b> Distingue dos situaciones que no son lo mismo:
     * <ul>
     *   <li>{@code null} → la consulta falló (por ejemplo, la ACCIÓN 'Y' todavía no está desplegada
     *       en esta base). El llamador tiene que buscarse la vida — el controlador cae al camino
     *       histórico — porque devolver una lista vacía sería mentirle al chofer diciéndole que no
     *       tiene entregas.</li>
     *   <li>lista vacía → la consulta anduvo y este chofer no tiene pendientes.</li>
     * </ul>
     *
     * @param codEmpleado el chofer
     * @return sus entregas pendientes, lista vacía si no tiene, o {@code null} si la consulta falló
     */
    List<EntregaChofer> listarEntregasXEmpleadoSinSync( int codEmpleado );

    /**
     * Sincroniza {@code trch_Entregas} con SAP (SP {@code p_abm_trch_EntregasSync} ACCIÓN 'S').
     *
     * <p>Es la mitad cara de la vieja ACCIÓN 'A', ahora invocable por separado para poder sacarla
     * del request del chofer. Corre con timeout propio ({@code entregas.sync.timeout-segundos}):
     * es una llamada a otro servidor por linked server y no puede quedarse colgada.
     *
     * <p><b>NUNCA lanza.</b> Un SAP caído devuelve {@code false} y la tabla se queda con lo último
     * que se haya sincronizado, que es un resultado perfectamente utilizable.
     *
     * <p>No lo llames desde un controlador: el que decide cuándo corresponde sincronizar —y que
     * no lo hagan quince hilos a la vez— es {@code commons.SincronizacionEntregasService}.
     *
     * @return {@code true} si el SP terminó sin error
     */
    boolean sincronizarConSap();

    /**
     * ¿Está desplegado en ESTA base el script de sincronización desacoplada?
     *
     * <p>Comprobación POSITIVA contra el catálogo, no una inferencia. Devuelve false si falta
     * cualquiera de las dos piezas del script 29.
     */
    boolean existeSoporteSyncDesacoplada();

    /**
     * Listar entregas por chofer
     * @param fechaEntrega
     * @param codEmpleado
     * @return
     */
    List<EntregaChofer> listarEntregasXChofer(  String fechaEntrega, int codEmpleado );

    /**
     * Listar choferes
     * @return
     */
    List<EntregaChofer> lstChoferes();

    /**
     * Mostrara un extracto de entregas si finalizaron o no
     * @param fechaIni
     * @param fechaFin
     * @return
     */
    List<EntregaChofer> lstChoferesExtracto( Date fechaIni, Date fechaFin, int codSucursal, int codEmpleado);

    /**
     * Listar pedidos pendientes de entrega del SAP
     * @return
     */
    List<PedidoPendienteEntregaDTO> lstPedidosPendientesEntrega( );

    /**
     * Datos de UNA entrega para armar el aviso por WhatsApp (SP p_list_trch_Entregas ACCION 'G').
     *
     * Una entrega es un par (docEntry, db): el documento tiene N filas en trch_Entregas, una por
     * itemCode, y este metodo devuelve una sola fila agregada con cantidadItems = COUNT de esas filas.
     *
     * NUNCA lanza excepcion: si el SP falla o el documento no existe, devuelve null y loguea.
     * Esto es obligatorio, porque se invoca desde el flujo de registro de entrega del chofer y ese
     * flujo no puede romperse ni demorarse por el aviso.
     *
     * @param docEntry docEntry del documento (los marcadores de ruta con docEntry = -1 se excluyen)
     * @param db base de datos / empresa: IPX, ESP, PRODPAP
     * @return el DTO, o null si no se pudo obtener
     */
    EntregaNotificacionDTO obtenerDatosNotificacion( long docEntry, String db );

}
