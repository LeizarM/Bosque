package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.util.Date;
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ChipTigo implements Serializable {

    private int codLinea;
    private int codEmpleado;
    private Date fechaSolicitud;
    private String telefono;
    private String nombreCompleto;
    private String descripcion;
    private String codigo;
    private int audUsuarioI;
    private Date audFechaI;

    //auxiliares
    private String search;
    private int fila;
    private int pagina;
    private int tamanoPagina;
    private String periodo;
    private Integer totalPaginas;
}
