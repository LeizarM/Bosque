package bo.bosque.com.impexpap.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

/**
 * Fila de {@code p_list_tcpe_Banco 'L'}: la pregunta con su categoría, su respuesta y el resumen de opciones.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TcpeBancoPreguntaDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long codPregunta;

    private Long codClasificacion;

    private String nombreClasificacion;

    private Long codArea;

    private String nombreArea;

    private String pregunta;

    private Integer prioridad;

    private Boolean activo;

    private Long codRespuesta;

    private Boolean esCerrada;

    /** Respuesta modelo de una abierta. */
    private String respuesta;

    private Integer cantOpciones;

    private Integer cantValidas;

    /** Cantidad de exámenes que la usaron. */
    private Integer usadaEnExamenes;

    private Long audUsuario;

    private Date audFecha;

    private String nombreUsuario;
}
