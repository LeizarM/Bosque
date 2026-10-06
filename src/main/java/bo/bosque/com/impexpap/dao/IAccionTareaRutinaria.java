// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dao\IAccionTareaRutinaria.java
package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.AccionTareaRutinaria;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

public interface IAccionTareaRutinaria {

    /**
     * Registrar, actualizar o eliminar una acción de tarea rutinaria.
     * @param mb   Objeto con los datos
     * @param acc  Acción ('I', 'U', 'D')
     */
    RespuestaSp registrar(AccionTareaRutinaria mb, String acc);

    /**
     * [L] Listado de acciones de tarea rutinaria con filtros opcionales (todos NULL = todas).
     */
    List<AccionTareaRutinaria> listar(AccionTareaRutinaria filtro);

    /**
     * [L] Carga UNA acción de tarea rutinaria por ID exacto (para load-before-update).
     * Solo envía @idATR al SP; todo lo demás queda en DEFAULT NULL.
     */
    AccionTareaRutinaria obtenerPorId(long idATR);
}
