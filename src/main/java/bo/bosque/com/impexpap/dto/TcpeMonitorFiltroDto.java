package bo.bosque.com.impexpap.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * Cuerpo de {@code /examenes/monitor/listar} ({@code codRolExamen}) y {@code /espejo} ({@code codExamen}). Sin el
 * que pide cada acción, el SP no devuelve filas.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TcpeMonitorFiltroDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long codRolExamen;

    private Long codExamen;
}
