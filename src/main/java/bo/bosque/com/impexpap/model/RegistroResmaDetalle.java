package bo.bosque.com.impexpap.model;

import java.io.Serializable;
import java.util.Date;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class RegistroResmaDetalle implements Serializable {

    private String idRmd;
    private int idMer;
    private int idTd;
    private String codArticulo;
    private String descripcion;
    private int cantidad;
    private float precioUnitario;
    private float subtotalUsd;
    private String placa;
    private String chofer;
    private int audUsuario;

}