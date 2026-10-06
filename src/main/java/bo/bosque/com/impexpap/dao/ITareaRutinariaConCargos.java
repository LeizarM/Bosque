// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dao\ITareaRutinariaConCargos.java
package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.RegistrarTareaRutinariaRequest;
import bo.bosque.com.impexpap.utils.RespuestaSp;

public interface ITareaRutinariaConCargos {

    /**
     * Crea una tarea rutinaria y la asigna a uno o más cargos en una sola
     * transacción (p_registrar_tac_tareaRutinariaConCargos). En modo jefe
     * (default), el SP valida que el caller tenga un cargo vigente con
     * nivel &lt;= nivelMaximoJefe y que CADA cargo destino esté en su
     * subárbol — si algo falla, no se inserta nada.
     */
    RespuestaSp registrar(RegistrarTareaRutinariaRequest req);
}
