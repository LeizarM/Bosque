package bo.bosque.com.impexpap.model;

import java.io.Serializable;
import java.util.Date;

import lombok.*;

/**
 * Mapea 1:1 la tabla <b>tcbr_accion</b>: la historia de eventos de una garantia.
 *
 * <pre>
 *   codAccion   bigint       NOT NULL IDENTITY PK
 *   codGarantia bigint       NOT NULL FK -&gt; tcbr_garantia (NO ACTION)
 *   fecha       datetime     NOT NULL
 *   estado      varchar(50)  NOT NULL  (catalogo v_tipos grupo 29)
 *   observacion varchar(300) NULL
 *   audUsuario  int          NOT NULL
 *   audFecha    datetime     NOT NULL
 * </pre>
 *
 * <p>Estados (grupo 29) y quien los crea:
 * <ul>
 *   <li><b>REG</b> — registrado. Lo crea {@code p_abm_GarantiaCbr} junto con la garantia.</li>
 *   <li><b>TRASP</b> — traspaso a custodia. Lo crea el traspaso masivo
 *       ({@code p_abm_AccionCbr} ACCION 'T'). Una garantia es "pendiente de traspaso"
 *       mientras tenga UNA sola accion.</li>
 *   <li><b>EXT</b> — extension de la fecha de expiracion.</li>
 *   <li><b>NOT</b> — nota libre. La unica que no exige traspaso previo.</li>
 *   <li><b>CER</b> — cierre. Con ella la garantia pasa a CERRADO y ya no se modifica.</li>
 * </ul>
 * REG y TRASP no se cargan a mano: el procedimiento los rechaza en la ACCION 'I'.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class AccionCbr implements Serializable {

    /** bigint IDENTITY. 0 o null = alta. */
    private Long codAccion;

    /** bigint. Garantia a la que pertenece. */
    private Long codGarantia;

    /** datetime. Fecha del evento; si llega null el procedimiento graba GETDATE(). */
    private Date fecha;

    /** varchar(50). Codigo del catalogo v_tipos grupo 29. */
    private String estado;

    /** varchar(300) NULL. Lo unico que se puede editar despues del alta. */
    private String observacion;

    /** int. Sale del token, nunca del cuerpo. */
    private Integer audUsuario;

    /** datetime. La sella el procedimiento con GETDATE(). */
    private Date audFecha;
}
