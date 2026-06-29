package bo.bosque.com.impexpap.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
public class BonoEmpleado {
    private int codBonEmp;
    private int codBono;
    private int codEmpleado;
    private Float monto;
    private int audUsuarioI;

    //parametros auxiliares
    private int fila;
    private int pagina;
    private int tamanoPagina;
    private Integer totalPaginas;
    private Integer totalRegistros;
    private String mes;
    private String anio;
    private String nombreCompleto;
    private String search;
    private Integer soloBono;
}