package bo.bosque.com.impexpap.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Resultset de las ramas de venta de <b>p_list_Articulo</b>: 'D', 'E', 'F', 'G' y 'H'.
 *
 * <p>Esas cinco ramas cruzan tpr_articulo con tablas que NO son del modulo de precios
 * (tped_almSapXArticulo, tped_almacenSAP, tped_precioArtSAP) y por eso ninguna de sus
 * columnas de precio o disponibilidad puede vivir en el model {@code Articulo}: si
 * estuvieran ahi, {@code SpHelper.ejecutarAbm} las mandaria como parametros inexistentes
 * a {@code p_abm_articulo} y el EXEC fallaria.
 *
 * <p>Las cinco ramas comparten la misma forma una vez aplicado el script
 * {@code resources/sql/tpr_Articulo.sql}, que pone alias a las columnas que el legacy
 * devolvia sin nombre (los {@code null} y los {@code 0} literales de F, G y H). Las
 * columnas que una rama no calcula llegan en null o en 0:
 *
 * <ul>
 *   <li><b>D</b> — disponibles por grupo de familia y lista de precio: sin idGrpFamiliaSap.</li>
 *   <li><b>E</b> — disponibles por ciudad: listaPrecio y precio llegan en 0 y moneda en null.</li>
 *   <li><b>F</b> — precios de un articulo: solo codArticulo, listaPrecio, precio y moneda traen dato real.</li>
 *   <li><b>G</b> — precios lado vendedor: sin gramajeSap y con codCiudad en 0.</li>
 *   <li><b>H</b> — precios de vendedores externos (excluye la lista minima): misma forma que G.</li>
 * </ul>
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ArticuloDisponibleDto implements Serializable {

    /** tpr_articulo.codArticulo. Unica columna con dato real en las cinco ramas. */
    private String codArticulo;

    /** tpr_articulo.datoArt. En la rama 'F' el legacy la devolvia sin alias; el script le pone nombre y llega en null. */
    private String datoArt;

    /** tped_precioArtSAP.listaPrecio. En las ramas 'D' y 'E' es el literal 0, no una lista real. */
    private Integer listaPrecio;

    /** tped_precioArtSAP.precio. BigDecimal porque es dinero. En la rama 'E' es el literal 0. */
    private BigDecimal precio;

    /** tped_precioArtSAP.moneda. En la rama 'E' llega null por diseno del procedimiento. */
    private String moneda;

    /**
     * <b>El nombre esta mal escrito a proposito.</b> El alias del procedimiento es
     * {@code diponible} (sin la 's') en las ramas D, E, G y H, y el script SQL lo conserva
     * asi para no romper el JSF que todavia consume esas ramas.
     * {@code BeanPropertyRowMapper} mapea por nombre literal: si este campo se llamara
     * {@code disponible} llegaria siempre null <b>sin lanzar ninguna excepcion</b>. No
     * renombrar aqui sin renombrar antes el alias en el procedimiento.
     *
     * <p>Es la suma de {@code tped_almSapXArticulo.disponible} con CAST a INT, por eso Integer.
     */
    private Integer diponible;

    /** tpr_articulo.gramajeSap. Llega con dato real solo en 'D' y 'E'; en 'F', 'G' y 'H' es null. */
    private BigDecimal gramajeSap;

    /** tpr_articulo.unidadMedida. En 'G' y 'H' viene con ISNULL(..., 'Sin unidad de Medidad en el SAP'); en 'F' llega null. */
    private String unidadMedida;

    /** tped_almacenSAP.codCiudad. En 'F', 'G' y 'H' es el literal 0, no la ciudad consultada. */
    private Integer codCiudad;

    /** tpr_producto.idGrpFamiliaSap. Solo lo devuelven las ramas 'G' y 'H'; en 'D', 'E' y 'F' llega null. */
    private Integer idGrpFamiliaSap;
}
