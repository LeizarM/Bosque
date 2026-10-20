package bo.bosque.com.impexpap.dto;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * Filtros de {@code p_list_tcpe_Lote} ({@code L}) y la categoría de {@code D} (disponibles). Nulo = sin filtro.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TcpeLoteFiltroDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long codRolExamen;

    private Long codArea;

    private Long codClasificacion;

    /** E u O. */
    private String modalidad;
}
