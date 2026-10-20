package bo.bosque.com.impexpap.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

/**
 * Fila de {@code p_list_tcpe_Monitor 'D'} (contrato 8.4): lote con exámenes en curso o con la ventana abierta, para
 * el selector del panel "En vivo".
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TcpeMonitorLoteDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long codRolExamen;

    private String titulo;

    private String nombreClasificacion;

    private String modalidad;

    private Date inicioVentana;

    private Date finVentana;

    private Integer examenes;

    private Integer sinIniciar;

    private Integer enCurso;

    private Integer entregados;
}
