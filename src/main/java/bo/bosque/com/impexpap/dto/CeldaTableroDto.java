// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dto\CeldaTableroDto.java
package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.util.Date;

/**
 * Una celda del tablero de Verificar Cierre en un día
 * ({@code p_list_tac_FirmaCierre 'K'}, archivo SQL 81). Solo llegan las celdas
 * de quien mira; el tablero completo, solo si la matriz le asigna todas.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CeldaTableroDto {
    private Date fechaTarea;
    private Date fechaRevisada;
    private String seccion;
    private String nombreSeccion;
    private Long codSucursal;
    private String nombreSucursal;
    /** "A tiempo", "Tarde", "Sin firmar" o "Pendiente": lo calcula el SP. */
    private String estado;

    private Long idFC;
    private Long codEmpleadoFirma;
    private String firmadoPor;
    private Date fechaFirma;
    private Integer filas;
    private Integer filasRevisadas;
    private Integer autorrevisadas;
    private Integer hallazgos;
    private String obs;
    /** Quienes tienen la celda en la matriz, separados por coma. */
    private String responsables;
}
