package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.io.Serializable;

/**
 * Filtro del detalle de lo descontado. Todo opcional: sin nada trae el periodo
 * abierto completo.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FiltroDescuentoDto implements Serializable {

    private Integer mes;
    private Integer anio;
    private String  origen;
    private Long    idVendedor;
}
