package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.io.Serializable;

/**
 * Forma del resultset de {@code p_list_clasificacionPrecio} con {@code @ACCION = 'B'}:
 * lista de precios cruzada con su sucursal (INNER JOIN a tb_sucursal), ordenada por
 * estado DESC y vpp.
 *
 * <p>Existe como DTO y no como model porque {@code nombreSucursal} no es una columna de
 * tpr_clasificacionPrecio: si viviera en el model, {@code SpHelper.ejecutarAbm} lo mandaria
 * como parametro inexistente al SP de ABM y el EXEC fallaria.
 *
 * <p>Las demas ramas del listado NO usan este DTO: la rama 'L' devuelve solo columnas de la
 * tabla y se mapea al model {@code ClasificacionPrecio}; las ramas 'C' y 'D' devuelven una
 * sola columna ({@code vpp}) y tambien se mapean al model, del que solo se lee ese campo.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ClasificacionPrecioDto implements Serializable {

    /** tpr_clasificacionPrecio.idClasificacion */
    private Long idClasificacion;

    /** tpr_clasificacionPrecio.codSucursal */
    private Long codSucursal;

    /** tpr_clasificacionPrecio.vpp */
    private Integer vpp;

    /** tpr_clasificacionPrecio.estado: 1 = activo, 0 = desactivado. */
    private Integer estado;

    /** tpr_clasificacionPrecio.nombrePrecio */
    private String nombrePrecio;

    /**
     * Nombre de la sucursal (tb_sucursal.nombre).
     * <p>El SELECT historico devolvia esa columna con el label generico {@code nombre}, que
     * seguia usando el JSF por posicion. El ALTER conserva {@code nombre} y agrega al final
     * {@code suc.nombre AS nombreSucursal}, que es el label que lee este DTO.
     */
    private String nombreSucursal;
}
