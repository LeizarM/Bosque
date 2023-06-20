package bo.bosque.com.impexpap.model;
import lombok.*;
import java.io.Serializable;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CostoIncre implements Serializable {

    private int idIncre;
    private int codSucursal;
    private int idPropuesta;
    private int valor;
    private int audUsuario;

    /**
     * Variables Auxiliares
     */
    private int codCiudad;
    private String nombreSucursal;
    private String nombreCiudad;

}
