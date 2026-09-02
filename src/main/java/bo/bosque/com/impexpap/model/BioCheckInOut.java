package bo.bosque.com.impexpap.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.io.Serializable;
import java.util.Date;

/**
 * Marcación cruda del reloj biométrico — tabla {@code tbio_bioCHECKINOUT}.
 *
 * <p>SPs: {@code p_abm_BioCHECKINOUT} (I/U/D) · {@code p_list_BioCHECKINOUT} (L).
 *
 * <p>Sin columnas de auditoría: esta tabla no la escribe la app, la puebla el software
 * del fabricante del reloj (o el proceso de importación disparado por
 * {@code p_abm_BioCHECKINOUT ACCION='B'} — ver {@link bo.bosque.com.impexpap.dao.BioCheckInOutDao}).
 * Nombres de columna tal cual vienen del dispositivo (mayúsculas incluidas) para no
 * introducir un mapeo que se pueda desalinear con el SP.
 *
 * <p><b>{@code @JsonProperty} en USERID/CHECKTIME/CHECKTYPE/VERIFYCODE/SENSORID
 * agregado 2026-09-01</b> — bug real confirmado en {@link BioCheckInOutAdicional}
 * (mismo problema, mismo origen): sin la anotación, Jackson serializa
 * {@code getUSERID()} como {@code "userid"} (todo minúscula), no
 * {@code "USERID"}, al revés de lo que dice el comentario de arriba sobre
 * "nombres tal cual vienen del dispositivo". Acá nunca se notó en vivo
 * porque nada en el frontend consume {@code /biometrico/marcaciones/listar}
 * directamente — esta clase sólo se usa server-side dentro de
 * {@code calcularReporte}, que lee los valores vía getters de Java, no JSON
 * — pero el defecto es idéntico y hubiera reventado el día que alguien
 * conectara ese endpoint a una pantalla.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class BioCheckInOut implements Serializable {

    @JsonProperty("USERID")
    private int USERID;

    @JsonProperty("CHECKTIME")
    private Date CHECKTIME;

    @JsonProperty("CHECKTYPE")
    private String CHECKTYPE;

    @JsonProperty("VERIFYCODE")
    private Integer VERIFYCODE;

    @JsonProperty("SENSORID")
    private String SENSORID;

    @JsonProperty("Memoinfo")
    private String Memoinfo;

    @JsonProperty("WorkCode")
    private String WorkCode;

    private String sn;

    @JsonProperty("UserExtFmt")
    private Integer UserExtFmt;

    private String fechaString;
}
