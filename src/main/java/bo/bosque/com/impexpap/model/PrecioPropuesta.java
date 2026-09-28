package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Detalle de precios de una propuesta de reprecio: tabla <b>tpr_precioPropuesta</b>.
 *
 * <p>Una fila = un precio (tpr_precio) incluido en una propuesta (tpr_propuesta),
 * con su valor actual, su valor propuesto y el porcentaje aplicado.
 *
 * <p>SPs: <b>p_abm_precioPropuesta</b> (I, U, D, B, C, E) y
 * <b>p_list_precioPropuesta</b> (L, A, B, C, D).
 *
 * <p><b>Contiene EXACTAMENTE las 13 columnas de la tabla</b> y nada mas: SpHelper
 * serializa el POJO con Jackson y manda cada campo como parametro del SP, asi que
 * cualquier campo de display (los que vienen por JOIN) va en un DTO aparte, nunca aca.
 * Ver {@code PrecioPropuestaADto}, {@code PrecioPropuestaBDto} y
 * {@code PrecioPropuestaDDto}.
 *
 * <p><b>OJO con el nombre del id.</b> La columna se llama {@code idPrecioPropuesto}
 * (terminada en "o") pero el parametro del ABM legacy se llama {@code @idPrePropuesto}
 * y el del listado {@code @idPrecioPropuesta}. El campo respeta el nombre de la COLUMNA
 * porque asi lo mapea BeanPropertyRowMapper en la rama L. Para que el ABM reciba el id,
 * el script {@code src/main/resources/sql/tpr_PrecioPropuesta.sql} agrega al proc el
 * parametro alias {@code @idPrecioPropuesto} (al final y con DEFAULT). <b>Sin ese ALTER,
 * las acciones U, D y B no reciben el id y no modifican ninguna fila.</b>
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PrecioPropuesta implements Serializable {

    /** PK, bigint IDENTITY. Columna real: {@code idPrecioPropuesto} (no "...Propuesta"). */
    private Long idPrecioPropuesto;

    /** FK a tpr_propuesta.idPropuesta. */
    private Long idPropuesta;

    /** FK a tpr_precio.idPrecio. */
    private Long idPrecio;

    /**
     * <b>TRAMPA DE TIPO:</b> aqui es {@code varchar(15)}; en TODAS las demas tablas del
     * modulo (tpr_producto, tpr_precio, tpr_porcentaje, tpr_costoSug, tpr_articulo...)
     * {@code codigoFamilia} es {@code int}. Se mapea a String para reflejar la columna real
     * y no perder valores no numericos ni ceros a la izquierda.
     *
     * <p>Consecuencia: al comparar contra otras tablas la base hace conversion implicita
     * varchar -> int (el int tiene mayor precedencia) y revienta con error 245 si alguna
     * fila no es numerica. El script SQL de esta entidad normaliza esas comparaciones.
     */
    private String codigoFamilia;

    /** Precio vigente al momento de armar la propuesta. float(53) en BD: se usa BigDecimal para no perder centavos. */
    private BigDecimal precioActual;

    /** Precio que se propone. float(53) en BD. */
    private BigDecimal precioPropuesto;

    /** Porcentaje aplicado sobre el costo sugerido. float(53) en BD. */
    private BigDecimal porcentaje;

    /**
     * <b>DENORMALIZADO Y CORRUPTO EN LA BASE.</b> Se conserva porque el proc
     * p_abm_precioPropuesta lo recibe e inserta, pero <b>no sirve para mostrar</b>:
     * el dato bueno es {@code tpr_clasificacionPrecio.codSucursal}, alcanzable por
     * JOIN tpr_precio -> tpr_clasificacionPrecio. No filtrar ni reportar por este campo.
     */
    private Long codSucursal;

    /**
     * <b>DENORMALIZADO Y CORRUPTO EN LA BASE.</b> Codigo de lista de precios del SAP.
     * Mismo caso que {@link #codSucursal}: para mostrar, derivarlo por JOIN desde
     * {@code tpr_clasificacionPrecio.listNum}.
     */
    private Long listNum;

    /**
     * <b>DENORMALIZADO.</b> Copia del nombre de la lista de precios. El dato bueno esta en
     * {@code tpr_clasificacionPrecio.nombrePrecio}; esta copia puede estar desfasada.
     */
    private String nombrePrecio;

    /**
     * <b>DENORMALIZADO Y CORRUPTO: mal cargado en 4013 filas.</b> Se conserva porque el
     * proc lo recibe, pero para mostrar hay que derivarlo por JOIN desde
     * {@code tpr_clasificacionPrecio.vpp} (que es lo que hacen las ramas A y D del
     * p_list_). Nunca confiar en este valor.
     */
    private Integer vpp;

    /** Usuario que grabo la fila (tb_usuario). */
    private Long audUsuario;

    /**
     * Fecha de auditoria. El proc la pisa siempre con GETDATE() en I y en U, asi que lo que
     * se mande desde Java se ignora; el campo existe para poder leerla en la rama L.
     */
    private Date audFecha;

}
