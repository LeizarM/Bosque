package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

/**
 * Una entrada de la bitácora de cambios manuales — tabla {@code tbio_bioBitacora}.
 *
 * <p>Insert-only: la escriben los propios {@code p_abm_Bio*} (Marcaciones olvidadas
 * y Horarios) después de cada I/U/D/A/E exitoso — nunca se edita ni se borra desde
 * la app. Por eso no tiene un {@code registrar}, sólo lectura vía {@code IBioBitacora}.
 *
 * <p>SP: {@code p_list_BioBitacora} (L). Ver {@code sql/03_bitacora_biometrico.sql}.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class BioBitacora implements Serializable {

    private long idBitacora;
    private String tabla;
    private String idRegistro;
    private String accion;
    private String motivo;
    private long audUsuario;
    private Date audFecha;
}
