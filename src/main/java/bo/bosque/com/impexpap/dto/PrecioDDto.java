package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.math.BigDecimal;

/**
 * Forma del resultset de <b>p_list_precio @ACCION = 'D'</b> y de
 * <b>@ACCION = 'E'</b>: las dos ramas devuelven las mismas 19 columnas, en el
 * mismo orden. Solo cambia el filtro (E excluye los precios ya cargados en una
 * propuesta) y el tipo de JOIN sobre grupoFamiliaSap/proveedorExtSap.
 *
 * <p>Es la grilla de reprecio: producto + sucursal + lista + precio actual +
 * porcentaje + iva/it, todo lo que la pantalla necesita para calcular el precio
 * propuesto.
 *
 * <p><b>Bug del legacy corregido en el SP:</b> el SELECT repetia
 * {@code pre.precio} dos veces (indices 16 y 17). Con BeanPropertyRowMapper la
 * segunda pisaba a la primera y el dato "precio nuevo" que el codigo viejo leia
 * por indice 17 quedaba igual al actual. El script tpr_Precio.sql le pone alias
 * {@code precioNew} a la segunda, conservando la cantidad y el orden de columnas
 * para no romper a los lectores por indice del JSF.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PrecioDDto {

    /** tpr_producto.codigoFamilia (int). */
    private Integer codigoFamilia;

    /** Id crudo de la presentacion, no el texto. */
    private Long idPresentacion;

    /** tpr_proveedorExtSap.proveedorExtSap. En la rama A se llama "proveedor". */
    private String proveedorExtSap;

    /** tpr_grupoFamiliaSap.grpFam. En la rama A se llama "grupoFam". */
    private String grpFam;

    /** tpr_producto.formato: varchar(50). */
    private String formato;

    /** tpr_producto.gramaje: varchar(50), no numerico. */
    private String gramaje;

    /** tpr_color.color. */
    private String color;

    /** tb_sucursal.codSucursal (bigint). */
    private Long codSucursal;

    /** Nombre de la SUCURSAL, no del precio. */
    private String nombre;

    /** tpr_clasificacionPrecio.idClasificacion. */
    private Long idClasificacion;

    /** tpr_clasificacionPrecio.nombrePrecio, sin concatenar (a diferencia de la rama C). */
    private String nombrePrecio;

    /** tpr_precio.idPrecio. */
    private Long idPrecio;

    /** tpr_clasificacionPrecio.vpp (int). Es el numero de lista de venta. */
    private Integer vpp;

    /** ISNULL(tpr_porcentaje.porcen, 0). Puntos porcentuales, float(53) en la base. */
    private BigDecimal porcentaje;

    /** tpr_clasificacionPrecio.listNum (bigint). Numero de lista en SAP. */
    private Long listNum;

    /** Precio vigente: tpr_precio.precio. */
    private BigDecimal precio;

    /**
     * Segunda aparicion de {@code pre.precio} en el SELECT, ahora con alias
     * propio. Hoy trae el MISMO valor que {@link #precio}: la pantalla lo usa
     * como valor inicial editable del precio nuevo.
     */
    private BigDecimal precioNew;

    /** SELECT TOP 1 iva FROM tpr_costoIvaIt — subconsulta escalar, sin filtro por propuesta. */
    private BigDecimal iva;

    /** SELECT TOP 1 it FROM tpr_costoIvaIt — subconsulta escalar, sin filtro por propuesta. */
    private BigDecimal it;
}
