package bo.bosque.com.impexpap.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * Fila de {@code p_list_tcpe_Lote 'D'}: preguntas utilizables de una categoría.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TcpeLoteDisponibleDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long codClasificacion;

    private Integer disponibles;
}
