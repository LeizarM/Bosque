package bo.bosque.com.impexpap.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;

@Data
@NoArgsConstructor
public class Indemnizacion {
    private Integer codIndemnizacion;
    private Integer codEmpleado;
    private Double montoIndemnizacion;
    private Date fechaIndemnizacion;
    private String descripcion;
    private Integer audUsuarioI;
    private Integer estado;

    // Campos de apoyo para traer nombre del empleado
    private Empleado empleado = new Empleado();

    // Parametros auxiliares para paginacion/filtro
    private String buscar;
    private String fechaActual; // yyyy-MM-dd
    private Integer codEmpresa;
    private Integer codArea;
    
    // Columnas extra devueltas por el SP para el reporte/grilla
    private Integer nro;
    private String nombreCompleto;
    private String empresa;
    private String apellidos;
    private String ciNumero;
    private String fechaIngreso;
    private String periodo;
    private String fechaSalida;
    private Integer diasactual;
    private String antiguedad;
    private Double totalGanadoFecha;
    private Double tercerMesAnterior;
    private Double segundoMesAnterior;
    private Double primerMesAnterior;
    private Double promedioMeses;
    private Double indemnizacionMesAnterior;
    private Integer dias30;
    private Double indemnizacion;
    private Double indemnizacionActual;
    private Integer diasAguinaldo;
    private Double aguinaldo;
    private Double diasVacacion;
    private Double pagoFiniquitoQuinq;
    private Double indemnizacionFinal;
    private String caja;
    private String sucursal;
    private String area;
    private String cargo;

    // Metadatos Paginacion
    private Integer pagina;
    private Integer tamanoPagina;
    private Integer totalPaginas;
    private Integer totalRegistros;
}
