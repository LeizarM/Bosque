package bo.bosque.com.impexpap.dto;

import bo.bosque.com.impexpap.model.ChAccion;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Una accion del historial de un cheque, con el nombre de su estado.
 *
 * <p>Las columnas vienen de {@code p_list_Accion} 'L' (todas las acciones del cheque, orden por
 * fecha); {@code descripcion} se completa en Java con el catalogo v_tipos grupo 22, porque la rama
 * 'A' que la trae por JOIN descarta las acciones con un estado fuera del catalogo (hay filas con
 * estado {@code PEN}) y el legacy las muestra.
 */
@Getter
@Setter
@ToString(callSuper = true)
@NoArgsConstructor
public class AccionChequeDto extends ChAccion {

    /** Nombre del estado ("RECIBIDO", "A COBRANZA"...). Vacio si el codigo no esta en el catalogo. */
    private String descripcion;

    /** Numero de fila, 1..n. Lo pone el DAO. */
    private Integer nro;
}
