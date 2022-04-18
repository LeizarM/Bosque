package bo.bosque.com.impexpap.model;

import java.util.Date;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
public class Licencia {

    private int codLicencia;
    private int codPersona;
    private String categoria;
    private Date fechaCaducidad;
    private int audUsuario;
}
