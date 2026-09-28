package bo.bosque.com.impexpap.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Mapea la tabla <b>tpr_articulo</b> (modulo de Precios).
 *
 * <p>Contiene EXACTAMENTE las 10 columnas de la tabla, ni una mas: este objeto se
 * serializa con Jackson dentro de {@code SpHelper.ejecutarAbm} y cada propiedad viaja
 * como un parametro {@code @nombre} del procedimiento {@code p_abm_articulo}. Un campo
 * que no exista como parametro del procedimiento rompe el EXEC completo, por eso los
 * campos de solo lectura que llegan de los JOIN (precio, disponible, codCiudad,
 * idGrpFamiliaSap...) viven en {@code ArticuloDisponibleDto} y nunca aqui.
 *
 * <p>Los procedimientos asociados son {@code p_abm_articulo} (I/U/D) y
 * {@code p_list_Articulo} (ramas L, A, B, D, E, F, G, H).
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Articulo implements Serializable {

    /**
     * PK de la tabla: varchar(50) y <b>NO es IDENTITY</b>. El codigo lo entrega el
     * usuario o la sincronizacion con SAP, de modo que en la accion 'I' viaja siempre
     * lleno y {@code RespuestaSp.getIdGenerado()} queda en 0 (no hay SCOPE_IDENTITY que
     * devolver). Nunca tratarlo como numerico.
     *
     * <p>Ojo: el parametro del procedimiento {@code p_abm_articulo} esta declarado
     * varchar(100), mas ancho que la columna varchar(50); un codigo de mas de 50
     * caracteres pasa la validacion del procedimiento y explota recien en el INSERT.
     */
    private String codArticulo;

    /** int NULL. FK logica hacia tpr_producto.codigoFamilia (no hay constraint declarada). */
    private Integer codigoFamilia;

    /** varchar(150) NULL. Descripcion del articulo; es el campo por el que busca la rama 'A' con LIKE. */
    private String datoArt;

    /** varchar(150) NULL. Descripcion extendida / alterna del articulo. */
    private String datoArtExt;

    /**
     * float(53) NULL. Se mapea a BigDecimal y no a float/double: es una cantidad con
     * decimales significativos (puede haber fracciones de resma) y el binario de float
     * pierde precision. El wrapper ademas tolera el NULL de la columna.
     */
    private BigDecimal stock;

    /** float(53) NULL. Unidad tecnica de medida usada en el calculo de precios. BigDecimal por la misma razon que stock. */
    private BigDecimal utm;

    /**
     * varchar(15) NULL. Lo alimenta la sincronizacion con SAP, no la pantalla de ABM.
     * El procedimiento {@code p_abm_articulo} original no lo recibia; el script
     * {@code resources/sql/tpr_Articulo.sql} agrega el parametro al final con DEFAULT NULL
     * y, en la actualizacion, solo lo pisa cuando llega con valor.
     */
    private String unidadMedida;

    /**
     * float(53) NULL — <b>no es texto</b>. El model anterior lo declaraba String, lo que
     * convertia el gramaje en cadena y rompia cualquier comparacion numerica. Igual que
     * unidadMedida, lo alimenta SAP y el parametro del ABM es nuevo (DEFAULT NULL).
     */
    private BigDecimal gramajeSap;

    /** bigint NULL. Usuario de auditoria. Long y no int: la columna es bigint. */
    private Long audUsuario;

    /**
     * datetime NULL. Faltaba en el model anterior. El procedimiento la escribe siempre con
     * GETDATE() en las acciones 'I' y 'U' e ignora lo que se le mande, asi que aqui sirve
     * para leer (rama 'L') y como filtro exacto, no para grabar.
     */
    private Date audFecha;
}
