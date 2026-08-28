package bo.bosque.com.impexpap.model;

import java.io.Serializable;
import java.util.Date;

import lombok.*;

/**
 * Union entre grupo y tipo de talonario. Tabla tmto_talonarioPorGrupo.
 *
 * PK compuesta (codGrupo + codTipoRecibo) y sin columna IDENTITY: el SP de ABM
 * devuelve idGenerado = 0 siempre, y este modelo no tiene id propio.
 * Solo admite ACCION 'I' y 'D'; para mover un tipo de grupo es D + I.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class TalonarioPorGrupo implements Serializable {

    private long codGrupo;
    private long codTipoRecibo;
    private long audUsuario;
    private Date audFecha;

    // ---- solo lectura, los llena p_list_tmto_TalonarioPorGrupo ----
    private String datoGrupo;
    private String datoTipoNombre;
    private String sigla;
    private String datoTipo;

}
