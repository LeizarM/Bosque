// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dao\ITraspasoMovCaja.java
package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.TraspasoDelDiaDto;
import bo.bosque.com.impexpap.dto.VerificarTraspasoRequest;
import bo.bosque.com.impexpap.model.TraspasoMovCaja;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

public interface ITraspasoMovCaja {

    /**
     * Registrar, actualizar o eliminar un traspaso de movimiento de caja.
     * @param mb   Objeto con los datos
     * @param acc  Acción ('I', 'U', 'D')
     */
    RespuestaSp registrar(TraspasoMovCaja mb, String acc);

    /**
     * [L] Listado de traspasos de movimiento de caja con filtros opcionales (todos NULL = todas).
     */
    List<TraspasoMovCaja> listar(TraspasoMovCaja filtro);

    /**
     * [L] Carga UN traspaso de movimiento de caja por ID exacto (para load-before-update).
     * Solo envía @idTrasp al SP; todo lo demás queda en DEFAULT NULL.
     */
    TraspasoMovCaja obtenerPorId(long idTrasp);

    // ==================== TAREA 289 - VERIFICAR TRASPASO ENTRE SISTEMAS ====

    /**
     * [A] Los traspasos de UN dia, para la tarea 289.
     *
     * <p>No es un SELECT sobre la tabla: el SP pregunta a SAP por OPENQUERY y
     * cruza contra lo que ya se verifico. Devuelve las filas de SAP con su
     * estado, y ademas las que estan guardadas en Bosque y SAP ya no devuelve
     * (marcadas con {@code soloEnBosque}).
     */
    List<TraspasoDelDiaDto> listarDelDia(java.util.Date fecha);

    /**
     * [V] Marca UN traspaso como verificado o como que no cuadra.
     *
     * <p>Es un upsert: si el traspaso todavia no tiene fila en
     * {@code tac_traspasoMovCaja} la inserta, porque en este flujo la fila se
     * escribe recien al verificar.
     */
    RespuestaSp verificar(VerificarTraspasoRequest req, long audUsuario);

    /**
     * [S] Cierra el dia como "sin novedad" cuando no hubo ningun traspaso.
     *
     * <p>El SP <b>no le cree al cliente</b>: vuelve a preguntarle a SAP y
     * rechaza (error 24) si hay traspasos en esa fecha. Si la aplicacion
     * pudiera declarar por su cuenta que no habia nada, el registro no valdria
     * nada.
     */
    RespuestaSp sinNovedad(long idBitTarRuti, java.util.Date fecha, long audUsuario);
}
