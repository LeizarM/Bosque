package bo.bosque.com.impexpap.model;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Dependiente implements Serializable {

    private int codDependiente;
    private int codPersona;
    private int codEmpleado;
    private String parentesco;
    private String esActivo;
    private int audUsuario;


    /**
     * Variables de Apoyo
     */
    // 🔹 Constructor específico para eliminar por codEmail
    public Dependiente(int codDependiente) {
        this.codDependiente = codDependiente;
    }


    private String nombreCompleto;
    private String descripcion;
    private int edad;

}
