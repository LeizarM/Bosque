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
    private String tipo;
    private int audUsuario;
    // 🔹 Constructor específico para eliminar por codTelefono
    public Telefono(int codTelefono) {
        this.codTelefono = codTelefono;
    }

}
