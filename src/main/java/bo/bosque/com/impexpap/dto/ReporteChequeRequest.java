package bo.bosque.com.impexpap.dto;

import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Lo que mandan los dialogos de reportes de cheques. Una sola forma para todos: cada reporte lee los campos que
 * necesita y ignora el resto.
 *
 * <p>{@code codEmpresa} y {@code codSucursal} son la empresa y la sucursal de la pantalla (los dos combos del
 * legacy); el servidor comprueba que la empresa sea una de {@code /cheque/empresas} y que el usuario pueda ver
 * la sucursal. Las fechas viajan como {@code yyyy-MM-dd}.
 */
@Getter
@Setter
@NoArgsConstructor
public class ReporteChequeRequest implements Serializable {

    private Integer codEmpresa;
    private Long codSucursal;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date fechaDesde;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date fechaHasta;

    /** Un solo dia (reporte de custodio). */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date fecha;

    /** {@code PEN} / {@code CER}; ausente o {@code "-"} = todos (reporte de cobranzas). */
    private String estado;

    /** Codigo del cliente; ausente o {@code "-"} = todos (reporte de cobranzas). */
    private String codCliente;

    /** El cobrador; ausente o 0 = todos (reporte de custodio). */
    private Integer codEmpleado;

    /** La accion {@code TRASP} del traspaso a reimprimir. */
    private Long codAccion;
}
