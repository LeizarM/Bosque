package bo.bosque.com.impexpap.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Fila de {@code p_list_tcpe_Resultado 'C'}: informe por categoría y rango de fechas.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TcpeInformeDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long codRolExamen;

    private String titulo;

    private String nombreArea;

    private String nombreClasificacion;

    private String modalidad;

    @FechaDiaJson
    private LocalDate fecha;

    private Integer examenes;

    private Integer rendidos;

    private Integer calificados;

    private Double promedio;

    private Integer aprobados;

    private Integer reprobados;

    /** Nota mínima del lote con la que se cuentan aprobados y reprobados. */
    private BigDecimal notaAprobacion;

    /** No iniciaron dentro de la ventana; ya cuentan en {@code reprobados}. */
    private Integer ausentes;
}
