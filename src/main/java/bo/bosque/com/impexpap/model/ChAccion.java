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
 * Mapea 1:1 la tabla <b>tch_accion</b>: la historia de un cheque.
 *
 * <pre>
 *   codAccion   int          NOT NULL IDENTITY PK
 *   codCheque   int          NOT NULL   (sin FK a tch_cheque: lo comprueba p_abm_Accion)
 *   fecha       datetime     NOT NULL
 *   estado      varchar(50)  NOT NULL   v_tipos grupo 22
 *   codEmpleado int          NULL       0 en las acciones manuales, NULL en REC y TRASP, &gt; 0 en CUS
 *   nroSAP      varchar(50)  NULL       obligatorio al cerrar el cheque
 *   observacion varchar(200) NULL
 *   audUsuario  int          NOT NULL
 *   audFecha    datetime     NOT NULL
 * </pre>
 *
 * <p>Estados y quien los crea (grupo 22):
 * <ul>
 *   <li><b>REC</b> recibido: lo crea {@code p_abm_Cheque} 'I' junto con el cheque.</li>
 *   <li><b>TRASP</b> traspaso a cobranza: el traspaso masivo ({@code p_list_Accion} 'E').</li>
 *   <li><b>CUS</b> a cobranza: "A Custodio" / "Dar Custodia". Siempre con empleado.</li>
 *   <li><b>DEV</b> devuelto; <b>VEN</b> / <b>ADE</b> nueva fecha de cobro.</li>
 *   <li>Cierres: <b>COB</b> cobrado (con verificacion); <b>CEF</b>, <b>CCH</b>, <b>PAP</b>,
 *       <b>DPR</b> (sin verificacion).</li>
 * </ul>
 *
 * <p>{@code codEmpleado}: el legacy escribe <b>0</b>, no NULL, en toda accion manual (DEV, VEN,
 * COB, ...): 0 nulos y miles de ceros en {@code BOSQUE2PRUEBA}. REC y TRASP los crea el SP con
 * NULL. Quien inserte una accion manual manda 0 si no hay empleado, para no distinguir filas
 * del legacy de filas nuevas.
 *
 * <p>Todos los tipos son wrappers: {@code BeanPropertyRowMapper} revienta al escribir un NULL
 * sobre un primitivo.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ChAccion implements Serializable {

    /** int IDENTITY. 0 o null = alta. */
    private Integer codAccion;

    /** int. Cheque al que pertenece. */
    private Integer codCheque;

    /** datetime. En JSON va como {@code yyyy-MM-dd'T'HH:mm:ss}. */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private Date fecha;

    /** varchar(50). Codigo del catalogo v_tipos grupo 22. */
    private String estado;

    /** int NULL. */
    private Integer codEmpleado;

    /** varchar(50) NULL. */
    private String nroSAP;

    /** varchar(200) NULL. */
    private String observacion;

    /** int. Sale del token, nunca del cuerpo. */
    private Integer audUsuario;

    /** datetime. La sella el procedimiento con GETDATE(). */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private Date audFecha;
}
