package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Mapea 1:1 la tabla <b>tpr_articuloPropuesto</b> (articulos congelados dentro de una
 * propuesta de precios). Nueve columnas, ni una mas.
 *
 * <p><b>Por que no hay campos de display aca:</b> {@code SpHelper.ejecutarAbm} serializa
 * este POJO con Jackson y manda TODOS sus campos como parametros de
 * {@code p_abm_ArticuloProp}. Un campo que no exista como {@code @parametro} en el proc
 * hace fallar el EXEC entero. Los campos que vienen de JOIN (titulo, obs, proveedor,
 * familia, nombre de sucursal, precios calculados, codCad, filaCod...) viven en los DTO
 * {@code ArticuloPropuesto*Dto}, uno por forma de resultset de {@code p_list_ArticuloProp}.
 *
 * <p><b>Todos los tipos son envoltorios (wrappers) a proposito:</b> ocho de las nueve
 * columnas son NULL-ables en la base y {@code BeanPropertyRowMapper} revienta al asignar
 * NULL sobre un primitivo.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ArticuloPropuesto implements Serializable {

    /** bigint NOT NULL IDENTITY, PK. Lo genera la base; en el alta viaja en null. */
    private Long idArticulo;

    /** bigint NULL, FK -> tpr_propuesta.idPropuesta. */
    private Long idPropuesta;

    /** varchar(50) NULL, FK logica -> tpr_articulo.codArticulo. Es texto, no numero. */
    private String codArticulo;

    /** int NULL (no bigint), FK logica -> tpr_producto.codigoFamilia. */
    private Integer codigoFamilia;

    /**
     * varchar(150) NULL. Trampa: el parametro {@code @datoArticulo} de
     * {@code p_abm_ArticuloProp} estaba declarado varchar(100) y truncaba en silencio;
     * el script tpr_ArticuloPropuesto.sql lo lleva a varchar(150).
     */
    private String datoArticulo;

    /** float(53) NULL. BigDecimal y no float/double: es cantidad de inventario. */
    private BigDecimal stock;

    /** float(53) NULL. Unidad tecnica de medida (toneladas por unidad); divide precios. */
    private BigDecimal utm;

    /** bigint NULL. Usuario de auditoria. */
    private Long audUsuario;

    /**
     * datetime NULL. El proc siempre la pisa con GETDATE() en I/U/C/G, asi que lo que
     * se mande aca se ignora en la escritura; sirve para lectura y como filtro en la
     * ACCION 'L'.
     */
    private Date audFecha;
}
