package bo.bosque.com.impexpap.model;

import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Mapea 1:1 la tabla <b>tch_chTransaccionBancaria</b>: los numeros de transaccion bancaria con los que se pago o
 * deposito un cheque.
 *
 * <pre>
 *   codCheque        int          NOT NULL
 *   nroTransaccion   varchar(30)  NOT NULL
 *   codBanco         int          NULL      tch_banco (sin FK)
 *   fechaTransaccion date         NULL
 *   audUsuario       int          NULL
 *   audFecha         datetime     NULL
 * </pre>
 *
 * <p><b>Sin PK, sin FK, sin indices</b>: la clave de hecho es {@code codCheque} + {@code nroTransaccion}.
 * {@code p_abm_ChTransaccionBancaria} 'D' borra todas las filas del par. Tiene triggers {@code dai_} y {@code dad_}
 * (bitacora y la gemela {@code tch_chTransaccionBancariaEliminado}); no hay {@code dau_}.
 *
 * <p>Todos los tipos son wrappers: {@code BeanPropertyRowMapper} revienta al escribir un NULL sobre un primitivo.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ChTransaccionBancaria implements Serializable {

    /** int. Cheque al que pertenece. */
    private Integer codCheque;

    /** varchar(30). El legacy exige mas de 4 caracteres; el servidor ademas rechaza mas de 30. */
    private String nroTransaccion;

    /** int. {@code tch_banco.codBanco}. */
    private Integer codBanco;

    /** date. En JSON va como {@code yyyy-MM-dd}, sin hora ni zona. */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date fechaTransaccion;

    /** int. Sale del token, nunca del cuerpo. */
    private Integer audUsuario;

    /** datetime. La sella el procedimiento con GETDATE(). */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private Date audFecha;
}
