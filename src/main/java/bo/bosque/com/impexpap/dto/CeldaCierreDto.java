// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dto\CeldaCierreDto.java
package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.util.Date;

/**
 * Una celda de Verificar Cierre que le toca a quien abrió su tarea 39: una
 * sección de una sucursal (o de toda la empresa) y el día que revisa
 * ({@code p_list_tac_FirmaCierre 'M'}, archivo SQL 81). Qué celdas hay lo decide
 * la matriz en SQL.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CeldaCierreDto {
    /** A arqueos, F caja fuerte, T traspasos AXA, C cheques, R tareas del día. */
    private String seccion;
    private String nombreSeccion;
    /** Archivo 86: qué revisar, si tiene panel propio y si se marca fila por fila. */
    private String queRevisar;
    private Integer propia;
    private Integer marcaPorFila;
    /** Null = toda la empresa. */
    private Long codSucursal;
    private String nombreSucursal;
    private Date fechaRevisada;
    private Integer puedeAutorrevisar;

    private Long idFC;
    private Integer firmada;
    private String firmadoPor;
    private Date fechaFirma;
    private Integer filas;
    private Integer filasRevisadas;
    private Integer autorrevisadas;
    private Integer hallazgos;
    private String obs;

    /** Sin firma: 1 si lleva el botón Firmar. Lo decide el SP. */
    private Integer conBoton;
    /** Sin firma: en qué está la celda, en palabras del SP. */
    private String detalle;
}
