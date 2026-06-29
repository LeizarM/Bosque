package bo.bosque.com.impexpap.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
public class Bono {
    private Integer codBono;
    private Date fechaCreacion;
    private String descripcion;
    private String estado;
    private Date fechaEjecucion;
    private String tipoBono;
    private Float montoTotal;
    private int audUsuarioI;

    //parametros auxiliares
    private int fila;
    private int pagina;
    private int tamanoPagina;
    private Integer totalPaginas;
    private Integer totalRegistros;
    private Integer filtroMes;
    private Integer filtroAnio;
}