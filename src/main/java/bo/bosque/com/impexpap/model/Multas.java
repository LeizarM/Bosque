package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Multas implements Serializable {
    private int codMulta;
    private int codEmpleado;
    private int mes;
    private int anio;
    private float diasTrabajados;
    private float diasMulta;
    private float diasFalta;
    private float diasPagar;
    private float monto;
    private int audUsuarioI;
    private String estado;

    //variables auxiliares
    private int fila;
    private int pagina;
    private int tamanoPagina;
    private String nombreCompleto;
    private String seguroNombre;
    private float haberBasico;
    private Integer codEmpresa;
    private int totalRegistros;
    private int totalPaginas;
    private String search;
    private String xmlMultas;
    private boolean soloConMulta;
}
