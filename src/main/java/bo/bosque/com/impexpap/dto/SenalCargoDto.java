// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dto\SenalCargoDto.java
package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.util.Date;

/**
 * Una persona que no hizo ninguna de sus últimas N ocurrencias de una tarea:
 * posible cargo desactualizado ({@code p_list_tac_SenalCargo 'L'}, archivo
 * SQL 82). El motivo llega en palabras desde el SP.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class SenalCargoDto {
    private Long codEmpleado;
    private String persona;
    private String cargo;
    private Date cargoDesde;
    private Long codSucursal;
    private String nombreSucursal;

    private Long idTarRuti;
    private String tarea;
    private Integer idFrec;
    private Integer ocurrencias;
    private Integer noAplica;
    private Date desde;
    private Date ultimaHecha;
    private String laHaceOtro;
    private Integer vecesDelOtro;
    private Integer tareasDeLaPersona;
    private String motivo;

    /** "Está bien así": si está callada, hasta cuándo, por qué y quién. */
    private Long idSC;
    private Date calladaHasta;
    private String obsCallada;
    private String calladaPor;
}
