package bo.bosque.com.impexpap.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

/**
 * Fila de {@code p_list_tcpe_PreguntaSemana 'R'} (gestión): quién respondió, su cargo actual de trabajo, qué
 * escribió y cuándo.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TcpeRsptaSemanaDetalleDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long codRsptaSemana;

    private Long codUsuario;

    private String login;

    private String nombreCompleto;

    private String cargo;

    private String respuesta;

    private Date fechaRespuesta;
}
