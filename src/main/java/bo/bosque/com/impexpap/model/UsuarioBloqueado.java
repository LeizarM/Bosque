package bo.bosque.com.impexpap.model;


import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
public class UsuarioBloqueado {

    private int codUsuario;
    private Date fechaAdvertencia;
    private Date fechaLimite;
    private int bloqueado;
    private int audUsuario;
    // 🔹 Constructor específico para eliminar por codEmail
    public UsuarioBloqueado(int codUsuario) {
        this.codUsuario = codUsuario;
    }

}