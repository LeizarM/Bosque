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
 * Mapea 1:1 la tabla <b>tch_chPostergacion</b>: las postergaciones de cobro que se le concedieron a un cheque.
 *
 * <pre>
 *   codPostergacion int          NOT NULL IDENTITY (la tabla no tiene PK declarada)
 *   codCheque       int          NOT NULL
 *   fecha           date         NOT NULL
 *   observacion     varchar(200) NULL
 *   nombreArchivo   varchar(50)  NULL
 *   audUsuario      int          NOT NULL
 *   audFecha        datetime     NOT NULL
 * </pre>
 *
 * <p>{@code nombreArchivo} <b>siempre queda vacio</b>: {@code p_abm_ChPostergacion} 'I' escribe {@code ''} y el legacy
 * no lo actualiza al subir el PDF. El documento vive en una carpeta del servidor como {@code <codPostergacion>.pdf};
 * la existencia se comprueba mirando el archivo. Sin triggers.
 *
 * <p>Todos los tipos son wrappers: {@code BeanPropertyRowMapper} revienta al escribir un NULL sobre un primitivo.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ChPostergacion implements Serializable {

    /** int IDENTITY. 0 o null = alta. */
    private Integer codPostergacion;

    /** int. Cheque al que pertenece. */
    private Integer codCheque;

    /** date. Dia de la postergacion. En JSON va como {@code yyyy-MM-dd}. */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date fecha;

    /** varchar(200). El legacy exige mas de 2 caracteres; el servidor ademas rechaza mas de 200. */
    private String observacion;

    /** varchar(50). Siempre vacio: ver el javadoc de la clase. */
    private String nombreArchivo;

    /** int. Sale del token, nunca del cuerpo. */
    private Integer audUsuario;

    /** datetime. La sella el procedimiento con GETDATE(). */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private Date audFecha;
}
