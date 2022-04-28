package bo.bosque.com.impexpap.model;


import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Telefono {

    private int codTelefono;
    private int codPersona;
    private int codTipoTel;
    private String telefono;
    private int audUsuario;

}
