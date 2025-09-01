package bo.bosque.com.impexpap.model;

import java.util.Date;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.DateDeserializers;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
public class ExperienciaLaboral {

    private int codExperienciaLaboral;
    private int codEmpleado;
    private String nombreEmpresa;
    private String cargo;
    private String descripcion;
    @JsonDeserialize(using = DateDeserializers.DateDeserializer.class) // Especifica el deserializador
    private Date fechaInicio;
    @JsonDeserialize(using = DateDeserializers.DateDeserializer.class) // Especifica el deserializador
    private Date fechaFin;
    private String nroReferencia;
    private int audUsuario;
    // 🔹 Constructor específico para eliminar por codExperienciaLaboral
    public ExperienciaLaboral(int codExperienciaLaboral) {
        this.codExperienciaLaboral = codExperienciaLaboral;
    }

}
