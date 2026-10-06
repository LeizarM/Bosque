package bo.bosque.com.impexpap.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import bo.bosque.com.impexpap.dto.FechaDiaJson;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Mapea 1:1 la tabla <b>tch_verificacionDeposito</b>: la verificacion de un cheque depositado contra el banco
 * (pantalla Verificar Cheques, vista 77).
 *
 * <pre>
 *   codvd       int           NOT NULL IDENTITY PK
 *   codCheque   bigint        NULL      (sin FK a tch_cheque; el cheque es int)
 *   codBanco    bigint        NULL      banco donde se verifico (no es forzosamente el del cheque)
 *   fechaBanco  date          NULL      dia de la verificacion (sin hora)
 *   observacion varchar(50)   NULL
 *   estado      varchar(1)    NULL      v_tipos grupo 36: Y = Valido, N = Anulado
 *   audUsuario  int           NULL
 *   audFecha    datetime      NULL      (datetime, no datetimeoffset: se lee como fecha y hora normal)
 * </pre>
 *
 * <p>"Cancelar" no borra: pone {@code estado = 'N'}. Un cheque tiene como maximo una verificacion {@code Y}; la rama
 * {@code K} de {@code p_list_Cheque} las cuenta para habilitar "Cerrar con verificacion".
 *
 * <p>Todos los tipos son wrappers: {@code BeanPropertyRowMapper} revienta al escribir un NULL sobre un primitivo.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ChVerificacionDeposito implements Serializable {

    /** int IDENTITY. 0 o null = alta. */
    private Integer codvd;

    /** bigint NULL. */
    private Long codCheque;

    /** bigint NULL. */
    private Long codBanco;

    /** date NULL. En JSON va como {@code yyyy-MM-dd}. */
    @FechaDiaJson
    private LocalDate fechaBanco;

    /** varchar(50) NULL. */
    private String observacion;

    /** varchar(1) NULL. {@code Y} valida, {@code N} anulada. */
    private String estado;

    /** int. Sale del token, nunca del cuerpo. */
    private Integer audUsuario;

    /** datetime. La sella el procedimiento con GETDATE(). */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private Date audFecha;
}
