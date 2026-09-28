package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Forma del resultset de p_list_costoIncre en las ramas <b>'B'</b> y <b>'D'</b>:
 * el costo de tpr_costoIncre con el nombre de la sucursal traido por JOIN a
 * tb_sucursal.
 *
 * <p>Un solo DTO sirve para las dos ramas porque el mapeo es POR NOMBRE
 * (BeanPropertyRowMapper) y la forma de 'D' es un superconjunto de la de 'B':
 * <ul>
 *   <li>'B' devuelve nombre, codSucursal, valor -> idIncre queda en null.</li>
 *   <li>'D' devuelve idIncre, codSucursal, nombre, valor (todos los campos).</li>
 * </ul>
 *
 * <p>No se renombro la columna 'nombre' a 'nombreSucursal' para no cambiar la
 * forma del resultset que ya consume el JSF.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CostoIncreSucursalDto implements Serializable {

    /** tpr_costoIncre.idIncre. Solo viene en la rama 'D'; en la 'B' llega null. */
    private Long idIncre;

    /** Codigo de la sucursal (tb_sucursal.codSucursal). */
    private Long codSucursal;

    /** tb_sucursal.nombre. Columna de display, por eso vive aqui y no en el model. */
    private String nombre;

    /** tpr_costoIncre.valor: float(53) en la BD, BigDecimal aqui (es dinero). */
    private BigDecimal valor;
}
