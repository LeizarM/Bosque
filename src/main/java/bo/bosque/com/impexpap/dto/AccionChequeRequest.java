package bo.bosque.com.impexpap.dto;

import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Cuerpo de las tres acciones que se registran desde el detalle de un cheque:
 * {@code /cheque/accion/devolver}, {@code /cheque/accion/cerrar} y {@code /cheque/accion/fecha-cobro}.
 *
 * <p>No todos los campos aplican a todas:
 * <ul>
 *   <li>{@code fecha}: dia de la accion; no puede ser anterior a hoy (el calendario del legacy lo
 *       impide). Vacio = hoy.</li>
 *   <li>{@code estado}: <b>cerrar</b> ({@code COB} con verificacion; {@code CEF}, {@code CCH},
 *       {@code PAP}, {@code DPR} sin ella) y <b>fecha-cobro</b> ({@code VEN} o {@code ADE}).
 *       Devolver siempre es {@code DEV}.</li>
 *   <li>{@code nroSap}: obligatorio al cerrar (2 a 40 caracteres).</li>
 *   <li>{@code nuevaFechaCobro}: solo en fecha-cobro.</li>
 *   <li>{@code conVerificacion}: solo en cerrar; elige entre {@code COB} y los otros cuatro.</li>
 *   <li>{@code observacion}: 2 a 200 caracteres.</li>
 * </ul>
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class AccionChequeRequest implements Serializable {

    private Integer codCheque;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date fecha;

    private String estado;
    private String nroSap;
    private String observacion;
    private Boolean conVerificacion;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date nuevaFechaCobro;
}
