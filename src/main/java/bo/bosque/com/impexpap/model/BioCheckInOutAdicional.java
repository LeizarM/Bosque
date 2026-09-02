package bo.bosque.com.impexpap.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.io.Serializable;
import java.util.Date;

/**
 * Marcación adicional/corregida (olvidada) — tabla {@code tbio_bioCHECKINOUTAdicinal}.
 *
 * <p>SPs: {@code p_abm_BioCHECKINOUTAdicinal} (I/U/D) · {@code p_list_BioCHECKINOUTAdicinal} (L).
 * Nombre de tabla/SP con el typo original ("Adicinal") — se conserva tal cual porque
 * es el nombre real en la base.
 *
 * <p><b>Bug real, confirmado 2026-09-01</b> (usuario: "¿por qué no puedo eliminar
 * estas marcaciones olvidadas?"): sin {@code @JsonProperty}, Jackson NO respeta
 * la decapitalización de {@code java.beans.Introspector} para getters/setters
 * con dos o más mayúsculas iniciales — {@code getUSERID()} serializa como
 * {@code "userid"} (todo minúscula), no {@code "USERID"}. El frontend
 * (`BioCheckInOutAdicionalModel.fromJson`) esperaba las claves en mayúsculas,
 * tal como el SP las nombra — mismatch total, `checkTime` llegaba siempre
 * `null`. Confirmado con dos tests aislados usando el mismo
 * {@code Jackson2ObjectMapperBuilder} que arma Spring Boot para
 * {@code @RequestBody} (no un {@code ObjectMapper} genérico): serializa en
 * minúsculas Y deserializa ignorando en silencio las claves en mayúsculas
 * entrantes (sin tirar error — {@code FAIL_ON_UNKNOWN_PROPERTIES} está
 * deshabilitado por defecto en Spring Boot), dejando los campos en su
 * default (0/null). Mismo bug que {@link BioCheckInOut} ya corrigió para
 * {@code Memoinfo}/{@code WorkCode}/{@code UserExtFmt} — nunca se aplicó acá.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class BioCheckInOutAdicional implements Serializable {

    @JsonProperty("USERID")
    private int USERID;

    @JsonProperty("CHECKTIME")
    private Date CHECKTIME;

    @JsonProperty("CODEMPLEADO")
    private int CODEMPLEADO;

    private String fechaString;

    // ── solo lectura ──────────────────────────────────────────────────────
    private Date audFecha;
}
