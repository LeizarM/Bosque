package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Forma del resultset de p_list_costoIncre en la rama <b>'C'</b>: el catalogo de
 * sucursales con su ciudad, para armar la grilla donde el usuario carga los
 * costos de flete de una propuesta nueva.
 *
 * <p>ATENCION: esta rama NO lee tpr_costoIncre. Es un UNION de tres SELECT sobre
 * tb_sucursal + trh_ciudad y la columna <b>costo siempre vale 0</b> (literal en el
 * SQL): es la fila en blanco que el formulario va a completar, no un costo
 * guardado. Para leer costos reales usar la rama 'D'
 * ({@link CostoIncreSucursalDto}).
 *
 * <p>La columna se llama 'costo' y no 'valor' en el SP; se respeta el nombre para
 * no cambiar la forma del resultset que ya consume el JSF.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CostoIncreCiudadDto implements Serializable {

    /** tb_sucursal.codSucursal. */
    private Long codSucursal;

    /** trh_ciudad.codCiudad. */
    private Long codCiudad;

    /** tb_sucursal.nombre. */
    private String nombre;

    /** trh_ciudad.ciudad. */
    private String ciudad;

    /** Literal 0 en el SP: placeholder para que el front cargue el costo. */
    private BigDecimal costo;
}
