package bo.bosque.com.impexpap.model;

import java.io.Serializable;
import java.util.Date;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ControCombustibleMaquinaMontacarga implements Serializable {


    private long idCM;
    private int idMaquina;
    private Date fecha;
    private float litrosIngreso;
    private float litrosSalida;
    private float saldoLitros;
    private float horasUso;
    private float horometro;
    private int codEmpleado;
    private String codAlmacen;
    private String obs;
    private int audUsuario;


    /**
     * Varibles de apoyo
     **/

    private String whsCode;
    private String whsName; // nombre de almacen
    private String maquina;
    private String nombreCompleto;
}