// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dao\ITarRuXCargo.java
package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TarRuXCargo;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ITarRuXCargo {

    /**
     * Registrar, actualizar o eliminar una asignación de cargo a tarea rutinaria.
     * @param mb   Objeto con los datos
     * @param acc  Acción ('I', 'U', 'D')
     */
    RespuestaSp registrar(TarRuXCargo mb, String acc);

    /**
     * [L] Listado de asignaciones de cargo a tarea rutinaria con filtros opcionales (todos NULL = todas).
     */
    List<TarRuXCargo> listar(TarRuXCargo filtro);

    /**
     * [L] Carga UNA asignación de cargo a tarea rutinaria por ID exacto (para load-before-update).
     * Solo envía @idTarXCargo al SP; todo lo demás queda en DEFAULT NULL.
     */
    TarRuXCargo obtenerPorId(long idTarXCargo);

    /**
     * [C] Admin "Tareas Rutinarias por Cargo" (reemplaza dlgTarFunXCargo de
     * WizardEstOrg.java): asignaciones de UN cargo, enriquecidas con
     * descripción/frecuencia/fechaPartida/idATR de la tarea — forma
     * dinámica (no un DTO fijo) porque es un JOIN de despliegue, no el CRUD
     * estándar de la tabla.
     */
    List<Map<String, Object>> listarPorCargoConDetalle(long codCargo);

    /**
     * [T] Traspasos pendientes por cambio de cargo. Una fila por
     * (empleado que cambió de cargo × tarea que tenía su cargo anterior), con
     * {@code yaEstaEnCargoNuevo} indicando si el cargo nuevo ya la tiene y
     * {@code personasEnCargoNuevo} a cuánta gente le caería si se copia.
     * <p>
     * Existe porque el generador solo mira el cargo MÁS RECIENTE del empleado:
     * el día que RR.HH. carga el cargo nuevo, las tareas del anterior dejan de
     * generarse y nadie se entera. Esto las hace visibles para que alguien
     * decida, en vez de perderlas en silencio.
     *
     * @param desde desde qué fecha mirar los cambios de cargo; {@code null} =
     *              últimos 90 días (lo resuelve el proc).
     */
    List<Map<String, Object>> listarTraspasosPendientes(LocalDate desde);

    /**
     * [T] Copia UNA tarea del cargo anterior al cargo nuevo.
     * <p>
     * Idempotente y con guardas: si el cargo destino ya la tiene no duplica; si
     * alguna vez la tuvo y se dio de baja reactiva esa misma fila; y se niega a
     * copiar a un cargo sin ninguna persona activa (error 24). Por eso NO se
     * reusa {@code registrar(..., "I")}, que inserta siempre y dejaría pares
     * (tarea, cargo) duplicados con dos clicks.
     */
    RespuestaSp traspasarTarea(long idTarRuti, long codCargoDestino, long audUsuario);
}
