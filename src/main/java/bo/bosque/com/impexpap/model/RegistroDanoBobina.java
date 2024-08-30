package bo.bosque.com.impexpap.model;

import java.io.Serializable;
import java.util.Date;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class RegistroDanoBobina implements Serializable {

    private int idReg;
    private Date fecha;
    private int codEmpleado;
    private float totalKilosBobinas;
    private float totalKilosDanados;
    private float totalKilosDanadosReal;
    private float totalUsd;
    private String obs;
    private int docNum;
    private int codEmpresa;
    private int audUsuario;

    // atributos auxiliares
    private String codArticulo;
    private String descripcion;
    private int docNumB;
    private String db;
    private String articulo;
}