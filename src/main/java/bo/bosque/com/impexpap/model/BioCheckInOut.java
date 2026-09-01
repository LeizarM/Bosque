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
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class BioCheckInOut implements Serializable {

    private int USERID;
    private Date CHECKTIME;
    private String CHECKTYPE;
    private Integer VERIFYCODE;
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
