package bo.bosque.com.impexpap.model;

import java.io.Serializable;
import java.util.Date;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Movimiento implements Serializable {

    private long idMovimiento;
    private String tipoMovimiento;
    private int idOrigen;
    private String codigoOrigen;
    private int sucursalOrigen;
    private int idDestino;
    private String codigoDestino;
    private int sucursalDestino;
    private int codSucursal;
    private Date fechaMovimiento;
    private float valor;
    private float valorEntrada;
    private float valorSalida;
    private float valorSaldo;
    private String unidadMedida;
    private int estado;
    private String obs;
    private int codEmpleado;
    private int idCompraGarrafa;
    private int audUsuario;

}