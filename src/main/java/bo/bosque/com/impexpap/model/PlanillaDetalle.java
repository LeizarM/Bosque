package bo.bosque.com.impexpap.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
public class PlanillaDetalle {
    private Long codPlanilla;
    private Long codEmpleado;
    private String ciNumero;
    private String apellidos;
    private String nombres;
    private String nacionalidad;
    private Date fechaNacimiento;
    private String sexo;
    private String cargo;
    private Date fechaIngreso;
    private Date fechaSalida;
    private Float dias_pagados_mes;
    private Float horas_dias_pagadas;
    private Float haberBasico;
    private Float bonoAntiguedad;
    private Float bonoProduccion;
    private Float total;
    private Float AFP;
    private Float saldoPrestamo;
    private Float cuotasDolares;
    private Float cuotasBolivianos;
    private Float multas;
    private Float anticipo;
    private Float otros;
    private Float totalDescuentos;
    private Float liquido;

    // Campos calculados por el SP
    private String nombreCompleto;

    // Totales calculados por el SP
    private Float totalHaberBasico;
    private Float totalBonoAntiguedad;
    private Float totalBonoProduccion;
    private Float sumTotalGanado;
    private Float totalAFP;
    private Float totalSaldoPrestamo;
    private Float totalCuotasDolares;
    private Float totalCuotasBolivianos;
    private Float totalMultas;
    private Float totalAnticipo;
    private Float totalOtros;
    private Float totalTotalDescuentos;
    private Float totalLiquido;

    // Parámetros auxiliares de paginación y búsqueda
    private int fila;
    private int pagina;
    private int tamanoPagina;
    private Integer totalRegistros;
    private Integer totalPaginas;
    private String search;

    // Validación de negocio cruzada
    private Boolean tieneError;
    private String mensajeError;
}
