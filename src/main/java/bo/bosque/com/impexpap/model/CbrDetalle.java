package bo.bosque.com.impexpap.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import lombok.*;

/**
 * Mapea 1:1 la tabla <b>tcbr_cbrDetalle</b>: los documentos o bienes que componen una
 * garantia (pagare, inmueble, vehiculo...).
 *
 * <pre>
 *   codDetalle        bigint        NOT NULL IDENTITY PK
 *   codGarantia       bigint        NOT NULL FK -&gt; tcbr_garantia (NO ACTION)
 *   fecha             datetime      NOT NULL  (el alta graba GETDATE() e ignora el valor enviado)
 *   tipoGarantia      varchar(5)    NOT NULL  (catalogo v_tipos grupo 28)
 *   detalle           varchar(300)  NULL
 *   montoGarantiaParc decimal(15,4) NULL
 *   audUsuario        int           NOT NULL
 *   audFecha          datetime      NOT NULL
 * </pre>
 *
 * <p>Despues del alta solo se puede cambiar {@code montoGarantiaParc}: la rama 'U' de
 * {@code p_abm_CbrDetalle} no toca fecha, tipo ni detalle.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CbrDetalle implements Serializable {

    /** bigint IDENTITY. 0 o null = alta. */
    private Long codDetalle;

    /** bigint. Garantia a la que pertenece. */
    private Long codGarantia;

    /** datetime. Fecha de carga; la pone el procedimiento. */
    private Date fecha;

    /** varchar(5). Codigo del catalogo v_tipos grupo 28 (PAG, INM, VEH...). */
    private String tipoGarantia;

    /** varchar(300) NULL. Descripcion libre del documento o bien. */
    private String detalle;

    /** decimal(15,4) NULL. Parte del monto de la garantia que cubre este detalle. */
    private BigDecimal montoGarantiaParc;

    /** int. Sale del token, nunca del cuerpo. */
    private Integer audUsuario;

    /** datetime. La sella el procedimiento con GETDATE(). */
    private Date audFecha;
}
