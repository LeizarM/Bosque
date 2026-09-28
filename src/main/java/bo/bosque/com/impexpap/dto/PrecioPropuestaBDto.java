package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.math.BigDecimal;

/**
 * Forma del resultset de <b>p_list_precioPropuesta ACCION 'B'</b>: comparativo horizontal
 * (PIVOT) de una propuesta. Cada fila es un producto y trae una columna por lista de precios.
 * El UNION devuelve dos bloques de filas: primero los precios ACTUALES y despues los
 * PROPUESTOS; se distinguen por {@link #detalleA}.
 *
 * <p><b>Por que los campos se llaman precio1, precio2... y no p1, p2:</b> los alias del PIVOT
 * son {@code [Precio 1]}, {@code [Precio 2]}, ... y BeanPropertyRowMapper normaliza el nombre
 * de columna quitando los espacios y bajando a minusculas, asi que "Precio 1" cae en la
 * propiedad {@code precio1}. La lista de listas NO es correlativa: 1,2,3,4,10,11,12,13,20,21,22,23.
 *
 * <p>Si se agregan mas listas de precios al PIVOT en el SP, hay que agregar aqui el campo
 * {@code precioNN} correspondiente o esa columna se pierde en silencio.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PrecioPropuestaBDto {

    /** tpr_propuesta.idPropuesta. */
    private Long idPropuesta;

    /** tpr_propuesta.titulo. */
    private String titulo;

    /**
     * Descripcion concatenada del producto: codigoFamilia + presentacion + proveedor +
     * grupo familia + formato + gramaje + color. Alias "Producto" en el SP.
     */
    private String producto;

    /**
     * Etiqueta del bloque: "Precios Actuales: " o "Precios Propuestos: ".
     * El segundo SELECT del UNION llama a su columna {@code DetalleP}, pero en un UNION
     * mandan los nombres del PRIMER SELECT, asi que la columna real es {@code DetalleA}
     * para las dos mitades. Por eso hay un solo campo y no dos.
     */
    private String detalleA;

    private BigDecimal precio1;
    private BigDecimal precio2;
    private BigDecimal precio3;
    private BigDecimal precio4;
    private BigDecimal precio10;
    private BigDecimal precio11;
    private BigDecimal precio12;
    private BigDecimal precio13;
    private BigDecimal precio20;
    private BigDecimal precio21;
    private BigDecimal precio22;
    private BigDecimal precio23;
}
