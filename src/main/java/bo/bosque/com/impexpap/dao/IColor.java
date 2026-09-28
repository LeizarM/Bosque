package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Color;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

/**
 * Contrato de acceso al catalogo tpr_color.
 * Todo pasa por procedimientos almacenados: p_abm_color y p_list_color.
 */
public interface IColor {

    /**
     * Registra, actualiza o da de baja un color (p_abm_color).
     *
     * @param mb  datos del color; para 'U' y 'D' el idColor es obligatorio
     * @param acc accion: 'I' insertar, 'U' actualizar, 'D' eliminar
     * @return RespuestaSp con error, errormsg y el idGenerado del alta
     */
    RespuestaSp registrarColor(Color mb, String acc);

    /**
     * Lista colores sin importar el estado (p_list_color, ACCION 'L').
     *
     * @param idColor id a buscar; 0 devuelve todos
     */
    List<Color> obtenerColores(long idColor);

    /**
     * Lista solo los colores activos, para combos (p_list_color, ACCION 'A').
     * Devuelve unicamente idColor y color.
     */
    List<Color> obtenerColoresActivos();
}
