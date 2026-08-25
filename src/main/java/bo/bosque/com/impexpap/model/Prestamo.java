package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Prestamo implements Serializable {
    private Integer codEmpresa;
    private String db;
    private String codigoCuenta;
    private String nombreCuenta;
    private Date fechaAsiento;
    private String numAsiento;
    private String concepto;
    private String referencia;
    private Double debe;
    private Double haber;
    private String estadoAsignacion;
    private String estadoPrestamo;
    private Long codPrestamo;
    private Long codEmpleado;
    private String nombreEmpleadoAsignado;
    private Double saldoPendiente;

    // Parametros para asignación masiva
    private Long transIdSAP;
    private Double montoPrestamo;
    private String descripcion;
    private String fecIniPago;
    private Double numCuotas;
    private String xmlEmpleados;
    private String fechaDesembolso;
    private String observacion;
    private Long audUsuarioI;

    // Campos faltantes para el SP p_abm_Prestamo
    private String tipoPago;
    private String tipoEstado;
    private Double cuotaReferencia;
    private Long transTypeSAP;
    private Long lineIdSAP;
    private Long docEntrySAP;
    private Long lineNumSAP;
    private Long docNumSAP;
    private Date audFechaI;

    // Filtros de busqueda
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd")
    private Date fechaDesde;
    
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd")
    private Date fechaHasta;

    // Parametros auxiliares (Paginacion y busqueda)
    private String Search;
    private Integer fila;
    private Integer pagina;
    private Integer tamanoPagina;
    private Integer totalPaginas;
    private Integer totalRegistros;
    private Integer mostrarAnulados;
    private Integer forzar;
}
