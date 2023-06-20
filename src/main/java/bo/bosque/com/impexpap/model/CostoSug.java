package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CostoSug implements Serializable {

    private int idCosSug;
    private int idPropuesta;
    private int codigoFamilia;
    private float costoSug;
    private Date fechaI;
    private int audUsuario;

}
