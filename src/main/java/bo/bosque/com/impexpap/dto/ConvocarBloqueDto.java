package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.io.Serializable;

/**
 * Payload de {@code trs_sp_convocar} — convocar o excusar en BLOQUE para el sábado
 * de un evento.
 *
 * <p>Distinto de {@code p_abm_trs_Convocatoria}, que trabaja de a UNA persona. Acá se
 * elige el conjunto con <b>uno</b> de estos tres criterios:
 * <ul>
 *   <li>{@code codEmpleado} — una persona (equivale al ABM).</li>
 *   <li>{@code codEmpleadoJefe} — todo su subárbol del organigrama. Es el caso real
 *       del inventario: "que venga Contabilidad entera".</li>
 *   <li>{@code codCargo} — todos los que tienen ese cargo.</li>
 * </ul>
 *
 * <p>Sólo funciona en sábados con {@code alcanceEvento IS NOT NULL}: primero se declara
 * el evento ({@code p_abm_trs_Sabado @ACCION='U'}) y recién después se convoca. Las
 * ausencias de un sábado normal no van por acá — van por {@code trs_Programacion}.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ConvocarBloqueDto implements Serializable {

    /** Obligatorio. Tiene que ser un sábado con evento declarado. */
    private long idSabado;

    /** CONVOCADO (viene aunque no le toque) | EXCUSADO (no viene aunque le toque). */
    private String tipo;

    // Criterio de seleccion: mandar UNO de los tres.
    private Long codEmpleado;
    private Long codEmpleadoJefe;
    private Long codCargo;

    /** Se copia a la celda y gana sobre el motivo del evento. */
    private String motivo;

    /** Quién tomó la decisión. Queda en trs_Convocatoria. */
    private Long codEmpleadoAutoriza;

    private Long audUsuario;
}
