package bo.bosque.com.impexpap.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Fila de {@code p_list_tcpe_Resultado 'P'} (contrato 15.1): una pregunta fallada de su categoría, con su puesto.
 * {@code fallos} = {@code sinResponder} + {@code incompletas} + los que eligieron una incorrecta.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TcpeRankingPreguntaDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 1 = la más fallada de su categoría. */
    private Integer puesto;

    private Long codClasificacion;

    private String nombreClasificacion;

    private String nombreArea;

    private Long codPregunta;

    private String pregunta;

    private Boolean esCerrada;

    /** Veces que se calificó en exámenes rendidos. */
    private Integer veces;

    private Integer aciertos;

    private Integer fallos;

    /** No marcó nada (o dejó vacía la abierta). */
    private Integer sinResponder;

    /** Marcó solo una parte de las correctas, sin ninguna incorrecta. */
    private Integer incompletas;

    private BigDecimal porcentajeFallo;

    /** La opción incorrecta más elegida; nula si nadie eligió una incorrecta. */
    private String errorComun;

    private Integer vecesErrorComun;
}
