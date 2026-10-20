package bo.bosque.com.impexpap.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * Fila de {@code p_list_tcpe_Resultado 'S'}: una por opción (las abiertas con la opción nula). Para los PDF.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TcpeSolucionarioDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long codExamen;

    private Long codPreguntaH;

    private Integer orden;

    private String pregunta;

    private Boolean esCerrada;

    private Long codRsptaOpH;

    private String opcionRspta;

    private Boolean esValido;

    /** La respuesta modelo de una abierta (v10); nula en las cerradas o si no tiene. */
    private String respuestaModelo;
}
