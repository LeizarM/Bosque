package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.math.BigDecimal;

/**
 * Forma del resultset de <b>p_list_precioPropuesta ACCION 'A'</b>: detalle de los precios
 * propuestos de UNA propuesta, con los datos de producto/articulo/sucursal resueltos por JOIN.
 *
 * <p>Filtra por {@code @idPropuesta} y ordena por codArticulo, vpp.
 *
 * <p>Los nombres de los campos son exactamente los alias del SELECT (BeanPropertyRowMapper
 * mapea por nombre de columna, no por posicion). No agregar ni renombrar campos sin tocar
 * tambien el SP.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PrecioPropuestaADto {

    /** tpr_precioPropuesta.idPrecioPropuesto (PK de la fila editable). */
    private Long idPrecioPropuesto;

    /** varchar(15) en tpr_precioPropuesta (ver PrecioPropuesta#codigoFamilia). */
    private String codigoFamilia;

    /** tpr_producto.idPresentacion. */
    private Long idPresentacion;

    /** tpr_articulo.codArticulo. */
    private String codArticulo;

    /** tpr_articulo.datoArt: descripcion del articulo. */
    private String datoArt;

    /** tpr_proveedorExtSap.proveedorExtSap. */
    private String proveedorExtSap;

    /** tpr_grupoFamiliaSap.grpFam. */
    private String grpFam;

    /** tb_sucursal.nombre (tabla fuera del prefijo tpr_). */
    private String nombre;

    /**
     * Alias de {@code tpr_clasificacionPrecio.vpp}. Es el vpp BUENO, el derivado por JOIN;
     * no el denormalizado de tpr_precioPropuesta.vpp, que esta corrupto.
     */
    private Integer numListPrecio;

    /** Precio vigente. */
    private BigDecimal precioActual;

    /**
     * Texto armado en el SP con la formula del calculo, del estilo
     * {@code "(costoSug + porcentaje % + iva+it %) + costoIncre ="}. Es display puro:
     * sale de tres subconsultas escalares (tpr_costoSug, tpr_costoIvaIt, tpr_costoIncre)
     * y si alguna no tiene fila el CONCAT devuelve NULL entero.
     */
    private String operacion;

    /** Precio propuesto (editable en pantalla). */
    private BigDecimal precioPropuesto;
}
