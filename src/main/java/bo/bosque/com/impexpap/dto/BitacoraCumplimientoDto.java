// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dto\BitacoraCumplimientoDto.java
package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.util.Date;

/**
 * Una fila de la bitácora de cumplimiento
 * ({@code p_list_tac_BitTareaRuti @ACCION='B'}, archivo SQL 55).
 *
 * <p>Los nombres de los campos son los alias del SELECT: los llena
 * {@code BeanPropertyRowMapper} y, para el PDF, Jasper por reflexión desde los
 * getters. Cambiar uno sin cambiar el otro deja una columna vacía sin error.
 *
 * <p><b>{@code cumplimiento} no es {@code fueRealizado}.</b> En seis años hubo
 * cinco respuestas "No": la gente no marca que no hizo algo, no lo responde.
 * Por eso una pendiente con fecha pasada cuenta como no realizada.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class BitacoraCumplimientoDto {

    private long idBitTarea;
    private Date fechaPresentacion;
    private Date fechaCompletado;
    private Long idTarRuti;
    private String nombreTareaRutinaria;
    private Long idFrec;
    private String descripcionFrecuencia;
    private Long codEmpleado;
    private String nombreEmpleado;

    /** El cargo que la persona tenía en la fecha de la tarea, no el de hoy. */
    private Long codCargo;
    private String descripcionCargo;

    /** La sucursal de ese cargo, en esa fecha. */
    private Long codSucursal;
    private String nombreSucursal;

    private Integer fueRealizado;

    /**
     * R realizada · N no realizada (respondió "No", o venció sin responder) ·
     * A no aplica · P en plazo (pendiente cuya fecha todavía no pasó).
     */
    private String cumplimiento;

    private String obs;

    /** Quién respondió. Vacío en las que nadie respondió. */
    private String nombreRespondio;

    /**
     * El estado en palabras, para el PDF y para quien lea el JSON a mano.
     * Sin setter: no viene de la base.
     */
    public String getEstadoTexto() {
        if (cumplimiento == null) return "";
        switch (cumplimiento) {
            case "R": return "Realizada";
            case "N": return "No realizada";
            case "A": return "No aplica";
            case "P": return "En plazo";
            default:  return cumplimiento;
        }
    }
}
