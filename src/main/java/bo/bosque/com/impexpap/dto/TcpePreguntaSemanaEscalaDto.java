package bo.bosque.com.impexpap.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * Fila de {@code p_list_tcpe_PreguntaSemana 'E'} (gestión, v13): un valor de la escala, también si nadie lo eligió,
 * con cuántos lo eligieron y su porcentaje. {@code total} y {@code promedio} se repiten en cada fila.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TcpePreguntaSemanaEscalaDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer valor;

    /** La etiqueta de un extremo ("Muy mal", "Excelente"); nula en los valores del medio. */
    private String etiqueta;

    private Integer cantidad;

    /** De 0 a 100, con un decimal. */
    private Double porcentaje;

    private Integer total;

    private Double promedio;
}
