package bo.bosque.com.impexpap.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * Cuerpo de {@code /examenes/banco/duplicados} ({@code p_list_tcpe_Banco 'D'}). Sin categoría o sin texto, el SP no
 * devuelve filas.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TcpeBancoDuplicadosFiltroDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long codClasificacion;

    /** Lo que se está escribiendo en el editor. */
    private String texto;
}
