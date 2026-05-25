package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Anticipo implements Serializable {
    private Long codAnticipo;
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
    private String estado;
    private int audUsuarioI;
    private Date audFechaI;
    private String moduloOrigen;

    //parametros auxiliares
    private String Search;
    private int fila;
    private int pagina;
    private int tamanoPagina;
    private Integer totalPaginas;
    private Integer totalRegistros;
    private String xmlEmpleados;
    private String xmlDetalles;
    private String mes;
    private String anio;


}
