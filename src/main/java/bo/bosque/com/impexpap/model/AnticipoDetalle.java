package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.util.Date;


@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class AnticipoDetalle implements Serializable {
    private Long codAntDetalle;
    private Long codAnticipo;
    private Integer codEmpleado;
    private String nombreCompleto;
    private Float monto;
    private Integer codAutorizacion;
    private String estadoAnticipo;
    private Date fechaAnticipo;
    private String descripcion;
    private Integer audUsuarioI;
    private Date audFechaI;
    private int codEmpresa;

    //parametros auxiliares
    private String Search;
    private int fila;
    private int pagina;
    private int tamanoPagina;
    private Integer totalPaginas;
    private Integer totalRegistros;

}
