package bo.bosque.com.impexpap.dto;

import java.util.ArrayList;
import java.util.List;

import bo.bosque.com.impexpap.model.CbrDetalle;
import bo.bosque.com.impexpap.model.GarantiaCbr;
import lombok.*;

/**
 * Cuerpo de {@code /garantias/registrar}: la garantia, la observacion de su accion REG y los
 * detalles a agregar.
 *
 * <ul>
 *   <li>{@code codGarantia} 0/null = <b>alta</b>: garantia + REG + detalles, en una
 *       transaccion.</li>
 *   <li>{@code codGarantia} &gt; 0 = <b>edicion</b>, como el boton "Editar" del legacy: solo
 *       cambia {@code recFirmas} y {@code nroProtesta} y agrega los detalles nuevos. Montos,
 *       fechas y cliente se cambian por {@code /garantias/actualizar}.</li>
 * </ul>
 * {@code montoGarantiaCalc} y {@code audUsuario} se ignoran si llegan: los pone el backend.
 */
@Getter
@Setter
@ToString(callSuper = true)
@NoArgsConstructor
public class GarantiaRegistroDto extends GarantiaCbr {

    /** Observacion de la accion REG. Solo se usa en el alta. */
    private String observacion;

    /** Detalles a agregar. En la edicion, solo los nuevos: los existentes no se reenvian. */
    private List<CbrDetalle> detalles = new ArrayList<>();

    public boolean esAlta() {
        return getCodGarantia() == null || getCodGarantia() == 0L;
    }
}
