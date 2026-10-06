package bo.bosque.com.impexpap.dto;

import java.io.Serializable;
import java.util.Date;

import lombok.*;

/**
 * Filtro de las lecturas del modulo de garantias. Todo es opcional: lo que llega en null
 * no filtra (queda en el DEFAULT NULL del procedimiento).
 *
 * <ul>
 *   <li>{@code buscar} — texto libre: codigo o nombre del cliente (resumen y buscador SAP).</li>
 *   <li>{@code estado} — VIGENTE, CADUCADO o CERRADO.</li>
 *   <li>{@code tipoGarantia} — garantias que tengan al menos un detalle de ese tipo.</li>
 *   <li>{@code vencDesde}/{@code vencHasta} — rango sobre fechaExpiracion.</li>
 *   <li>{@code regDesde}/{@code regHasta} — rango sobre la fecha de registro (accion REG).</li>
 * </ul>
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class FiltroGarantiaDto implements Serializable {

    private String buscar;
    private String codClienteSAP;
    private String estado;
    private String tipoGarantia;
    private Date vencDesde;
    private Date vencHasta;
    private Date regDesde;
    private Date regHasta;
}
