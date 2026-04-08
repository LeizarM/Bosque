package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.sql.Date;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CambiosTigo implements Serializable {
    private int codCambio;
    private Integer codEmpleado;
    private Integer codTelefono;
    private Integer codCuenta;
    private String nombreOrigen;
    private String nombreCompleto;
    private String telefono;
    private String tipoSocio;
    private String descripcion;
    private String estado;
    private String periodoCobrado;
    private int audUsuario;
    private Date audFecha;

    private String search;
    private int fila;
    private int pagina;
    private int tamanoPagina;


}
