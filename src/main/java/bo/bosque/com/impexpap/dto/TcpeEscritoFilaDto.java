package bo.bosque.com.impexpap.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Fila de {@code RptTcpeExamenEscrito.jrxml}: una opción (o la abierta, con la opción nula) con la cabecera de su
 * examen repetida. El reporte agrupa por examen (una hoja nueva cada uno) y por pregunta. Uso interno.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TcpeEscritoFilaDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long codExamen;

    private Long numero;

    private String tituloLote;

    private String nombreClasificacion;

    private String nombreArea;

    private Date fecha;

    private Integer duracionMinutos;

    private BigDecimal notaAprobacion;

    private String nombreCompleto;

    private String ci;

    private Long codPreguntaH;

    /** Número de la pregunta dentro del examen. */
    private Integer orden;

    private String pregunta;

    private Boolean esCerrada;

    /** a, b, c... de la opción; nula en una abierta. */
    private String letra;

    private String opcionRspta;

    private Boolean esValido;

    /** Solo en el solucionario: la respuesta modelo de una abierta. */
    private String respuestaModelo;
}
