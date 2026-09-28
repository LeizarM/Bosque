package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.ArticuloDisponibleDto;
import bo.bosque.com.impexpap.model.Articulo;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

/**
 * Contrato de acceso a datos de <b>tpr_articulo</b>.
 *
 * <p>Un metodo por cada ACCION real de los procedimientos {@code p_abm_articulo}
 * (I, U, D) y {@code p_list_Articulo} (L, A, B, D, E, F, G, H). Todo pasa por
 * procedimientos almacenados via {@code SpHelper}; no hay SQL crudo.
 */
public interface IArticulo {

    /**
     * {@code p_abm_articulo} con ACCION 'I', 'U' o 'D'.
     *
     * @param articulo datos del articulo; codArticulo es obligatorio en las tres acciones
     *                 porque es la PK y no es IDENTITY
     * @param acc      "I" para insertar, "U" para actualizar, "D" para eliminar
     * @return RespuestaSp con error/errormsg; idGenerado siempre 0 (la PK no es IDENTITY)
     */
    RespuestaSp registrarArticulo(Articulo articulo, String acc);

    /**
     * {@code p_list_Articulo} ACCION 'L': listado plano de la tabla con filtro exacto por codigo.
     *
     * @param codArticulo codigo exacto a buscar; null devuelve todos los articulos
     * @return filas de tpr_articulo (sin unidadMedida ni gramajeSap: esa rama no los selecciona)
     */
    List<Articulo> obtenerArticulos(String codArticulo);

    /**
     * {@code p_list_Articulo} ACCION 'A': busqueda por coincidencia parcial, ordenada por descripcion.
     *
     * @param codigoFamilia familia a la que pertenece el articulo; null = todas
     * @param datoArt       texto a buscar dentro de la descripcion (LIKE %texto%); null = sin filtro
     * @param datoArtExt    texto a buscar dentro de la descripcion extendida (LIKE %texto%); null = sin filtro
     * @return filas de tpr_articulo sin datos de auditoria
     */
    List<Articulo> buscarArticulos(Integer codigoFamilia, String datoArt, String datoArtExt);

    /**
     * {@code p_list_Articulo} ACCION 'B': catalogo para el buscador de la vista.
     *
     * <p>Es un UNION ALL entre tpr_articulo y tinv_Producto, de modo que devuelve codigos
     * de dos origenes distintos; solo vienen llenos codArticulo y datoArt.
     *
     * @return pares codigo/descripcion ordenados por descripcion
     */
    List<Articulo> obtenerArticulosParaBusqueda();

    /**
     * {@code p_list_Articulo} ACCION 'D': articulos con stock y precio para el lado cliente,
     * filtrados por grupo de familia SAP, lista de precio y ciudad.
     *
     * @param idGrpFamiliaSap grupo de familia SAP; <b>0 significa todos los grupos</b>
     * @param listaPrecio     lista de precio del usuario (filtro exacto, obligatorio)
     * @param codCiudad       ciudad del usuario (filtro exacto, obligatorio)
     * @return articulos disponibles; el campo idGrpFamiliaSap del DTO llega null en esta rama
     */
    List<ArticuloDisponibleDto> obtenerDisponiblesPorGrupoFamilia(int idGrpFamiliaSap, int listaPrecio, int codCiudad);

    /**
     * {@code p_list_Articulo} ACCION 'E': articulos disponibles de una ciudad, sin precio.
     *
     * @param codCiudad ciudad del usuario (filtro exacto, obligatorio)
     * @return articulos con disponibilidad mayor a 1; precio y listaPrecio llegan en 0 y moneda en null
     */
    List<ArticuloDisponibleDto> obtenerDisponiblesPorCiudad(int codCiudad);

    /**
     * {@code p_list_Articulo} ACCION 'F': todas las listas de precio de UN articulo en una ciudad.
     *
     * @param codArticulo articulo a consultar (obligatorio: la rama filtra por igualdad)
     * @param codCiudad   ciudad a consultar (obligatorio)
     * @return una fila por lista de precio; solo codArticulo, listaPrecio, precio y moneda traen dato real
     */
    List<ArticuloDisponibleDto> obtenerPreciosPorArticulo(String codArticulo, int codCiudad);

    /**
     * {@code p_list_Articulo} ACCION 'G': precios y disponibilidad de todos los articulos de
     * una ciudad para el lado vendedor.
     *
     * @param codCiudad ciudad del vendedor (obligatorio)
     * @return articulos con disponibilidad mayor o igual a 1 y precio mayor a 0
     */
    List<ArticuloDisponibleDto> obtenerPreciosVendedor(int codCiudad);

    /**
     * {@code p_list_Articulo} ACCION 'H': precios de UN articulo para vendedores externos,
     * excluyendo la lista de precio minima de la ciudad.
     *
     * @param codArticulo articulo a consultar (obligatorio)
     * @param codCiudad   ciudad del vendedor; el rango de listas permitido depende de ella
     *                    (1 -> 2..4, 4 -> 6..8, 5 -> 10..12) y cualquier otra ciudad no devuelve filas
     * @return listas de precio habilitadas para el vendedor externo
     */
    List<ArticuloDisponibleDto> obtenerPreciosVendedorExterno(String codArticulo, int codCiudad);
}
