package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Tipo;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

/**
 * Contrato de acceso al catalogo <b>tpr_tipo</b> del modulo de precios.
 * Todo pasa por procedimientos almacenados: {@code p_abm_tipo} y
 * {@code p_list_tipo}. Nunca RAW SQL.
 */
public interface ITipo {

    /**
     * Registra, actualiza o elimina un tipo ({@code p_abm_tipo}).
     *
     * @param mb  datos del tipo; para 'U' y 'D' el idTipo es obligatorio
     * @param acc accion: 'I' insertar, 'U' actualizar, 'D' eliminar
     * @return RespuestaSp con error, errormsg y el idGenerado del alta
     */
    RespuestaSp registrarTipo(Tipo mb, String acc);

    /**
     * Lista tipos sin importar el estado ({@code p_list_tipo}, ACCION 'L').
     *
     * @param idTipo id a buscar; 0 devuelve todos
     */
    List<Tipo> obtenerTipos(long idTipo);

    /**
     * Lista solo los tipos activos, para combos ({@code p_list_tipo}, ACCION 'A').
     * El resultset trae unicamente idTipo y tipo.
     */
    List<Tipo> obtenerTiposActivos();
}
