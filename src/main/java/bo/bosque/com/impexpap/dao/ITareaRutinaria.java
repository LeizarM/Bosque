// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dao\ITareaRutinaria.java
package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TareaRutinaria;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

public interface ITareaRutinaria {

    /**
     * Registrar, actualizar o eliminar una tarea rutinaria.
     * @param mb   Objeto con los datos
     * @param acc  Acción ('I', 'U', 'D')
     */
    RespuestaSp registrar(TareaRutinaria mb, String acc);

    /**
     * [L] Listado de tareas rutinarias con filtros opcionales (todos NULL = todas).
     */
    List<TareaRutinaria> listar(TareaRutinaria filtro);

    /**
     * [L] Carga UNA tarea rutinaria por ID exacto (para load-before-update).
     * Solo envía @idTarRuti al SP; todo lo demás queda en DEFAULT NULL.
     */
    TareaRutinaria obtenerPorId(long idTarRuti);
}
