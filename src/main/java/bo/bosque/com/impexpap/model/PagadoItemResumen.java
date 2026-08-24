package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;

/**
 * Los items de un periodo agrupados por motivo (ACCION R de
 * {@code p_list_tcom_PagadoItem}).
 * <p>
 * Es la vista que contesta la pregunta que el detalle no contesta de un vistazo:
 * de todo lo que se pago, cuanto no descontó y por que. El SP ordena por monto
 * descendente, asi que la primera fila es siempre el motivo que mas plata mueve.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PagadoItemResumen implements Serializable {

    /** DESCONTO, VENDEDOR_EXENTO, SIN_FAMILIA, FAMILIA_SIN_POLITICA o
     *  FUERA_DE_VIGENCIA. DESCONTO es el unico que no es una exclusion. */
    private String  motivo;
    private Integer items;
    private Double  montoBS;
    private Double  descuento;
}
