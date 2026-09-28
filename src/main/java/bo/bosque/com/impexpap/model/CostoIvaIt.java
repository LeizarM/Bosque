package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Mapea 1:1 la tabla <b>dbo.tpr_costoIvaIt</b> (modulo de Precios).
 *
 * <p>Es la tabla de los dos impuestos que entran en la formula de precio:
 * IVA e IT. En produccion tiene <b>una sola fila</b> (iva = 15.476, it = 3.57)
 * y el resto de los procedimientos del modulo la lee con
 * {@code (SELECT TOP 1 iva FROM tpr_costoIvaIt)} — sin ORDER BY, o sea que una
 * segunda fila volveria no determinista el calculo de TODOS los precios.
 * Por eso {@code p_abm_costoIvaIt} protege el invariante de fila unica y
 * {@code p_list_costoIvaIt} tiene la accion 'V' (vigente), que si ordena.
 *
 * <p>Los campos son EXACTAMENTE las columnas de la tabla: este POJO viaja a
 * {@code SpHelper.ejecutarAbm}, que serializa todos sus campos como parametros
 * del SP. Un campo de mas aca es un {@code EXEC} que falla. Los datos de
 * presentacion (por ejemplo iva + it ya sumados) van en {@code CostoIvaItDto}.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CostoIvaIt implements Serializable {

    /** PK IDENTITY. En la BD es {@code int}, no bigint como el resto del modulo. */
    private Integer idCII;

    /**
     * FK logica a tpr_propuesta (sin constraint en la BD). Hoy nadie la usa
     * para filtrar: las subconsultas del modulo ignoran esta columna y leen la
     * unica fila. Queda porque la columna existe.
     */
    private Long idPropuesta;

    /**
     * Porcentaje de IVA. En la BD es {@code float(53)}: se mapea a BigDecimal
     * a proposito, porque float binario no representa exacto valores como
     * 15.476 y el error se arrastra al precio final.
     */
    private BigDecimal iva;

    /** Porcentaje de IT. Mismo caso que {@link #iva}: float(53) en la BD. */
    private BigDecimal it;

    /** Usuario de auditoria. bigint NULL en la BD -> wrapper, nunca primitivo. */
    private Long audUsuario;

    /** Fecha de auditoria. Si llega null, el SP graba GETDATE(). */
    private Date audFecha;
}
