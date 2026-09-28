package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Un porcentaje de utilidad cambiado desde el editor de una familia del asistente, para
 * una lista de precio.
 *
 * <p>En {@code calcularFamilia} reemplaza al de {@code tpr_porcentaje} solo para la vista
 * previa. En {@code guardarFamilia} ademas se registra en {@code tpr_porcentaje} (alta si
 * la familia no tenia porcentaje en esa lista, modificacion si tenia), en la misma
 * transaccion que los precios propuestos: pidio el usuario no tener que ir a
 * «Porcentajes» para cambiarlo y volver.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PorcentajeArmadoDto implements Serializable {

    /** La lista de precio (tpr_clasificacionPrecio). */
    private Long idClasificacion;

    /** Margen sobre el costo en puntos porcentuales: 20 es 20 %. */
    private BigDecimal porcentaje;
}
