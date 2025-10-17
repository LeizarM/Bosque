package bo.bosque.com.impexpap.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class TigoEjecutado {
    private int codCuenta;
    private String corporativo;
    private Integer codEmpleado;
    private String nombreCompleto;
    private String ciNumero;
    private String descripcion;
    private String periodoCobrado;
    private String empresa;
    private float totalCobradoXCuenta;
    private float montoCubiertoXEmpresa;
    private float montoEmpleado;
    private String estado;
    private int audUsuario;

    private int fila;
    private Integer codEmpleadoPadre;
    private List<TigoEjecutado> items = new ArrayList<TigoEjecutado>();

}
