package bo.bosque.com.impexpap.model;

import java.io.Serializable;

import lombok.*;

/**
 * Una ciudad del catalogo de Ventas, y cuando viene de {@code tven_UsuarioCiudad}
 * tambien el usuario al que esta asignada.
 *
 * <p>Sale de {@code p_list_tven_UsuarioCiudad}: ACCION V (ciudades de venta),
 * U (las de un usuario) y A (todas las asignaciones). Se mapea por nombre de
 * columna con {@code SpHelper.ejecutarListado}.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CiudadVenta implements Serializable {

    private long codUsuario;
    private int codCiudad;
    private String ciudad;

}
