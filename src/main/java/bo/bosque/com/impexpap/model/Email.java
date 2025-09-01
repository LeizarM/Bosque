package bo.bosque.com.impexpap.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Email {

    private int codEmail;
    private int codPersona;
    private String email;
    private int audUsuario;
    // 🔹 Constructor específico para eliminar por codEmail
    public Email(int codEmail) {
        this.codEmail = codEmail;
    }

}
