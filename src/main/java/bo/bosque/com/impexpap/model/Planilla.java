package bo.bosque.com.impexpap.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
public class Planilla {
    private Long codPlanilla;
    private Integer codEmpresa;
    private Integer codSeguro;
    private Date fechaEjecucion;
    private Date fechaPeriodo;
    private String estado;
    private Long audUsuarioI;

    // Campos calculados por el SP (JOINs)
    private String empresa;
    private String caja;
    private Float totalLiquido;

    // Parámetros auxiliares de paginación y filtros
    private int fila;
    private int pagina;
    private int tamanoPagina;
    private Integer totalPaginas;
    private Integer totalRegistros;
    private Integer filtroMes;
    private Integer filtroAnio;
}
