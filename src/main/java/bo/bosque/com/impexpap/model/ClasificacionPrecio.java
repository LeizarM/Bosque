package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ClasificacionPrecio implements Serializable {

    private int idClasificacion;
    private int codSucursal;
    private int listNum;
    private String nombrePrecio;
    private int vpp;
    private int estado;
    private int audUsuario;


}
