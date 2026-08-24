package bo.bosque.com.impexpap.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import lombok.*;

/**
 * Tramo de comision segun los dias que tardo el cliente en pagar
 * (tabla tcom_comisionPorRango). Lo usa la rama K de p_list_paraPagar.
 * <p>
 * OJO con la escala: aca comision esta en BASE 1, o sea 0.008 es 0,8%. En
 * tcom_grupo el porcentaje esta en puntos porcentuales, donde 0.7 es 0,7%.
 * comisionVisual trae el valor ya convertido para mostrar.
 * <p>
 * Un rango negativo representa el pago anticipado.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ComisionPorRango implements Serializable {

    private long       idCFR;
    private BigDecimal comision;        // base 1
    private BigDecimal comisionVisual;  // solo lectura, en puntos porcentuales
    private int        min;             // dia inicial del tramo
    private int        max;             // dia final del tramo
    private String     tipo;            // Contado o Credito
    private int        esInterno;
    private long       audUsuario;
    private Date       audFecha;
}
