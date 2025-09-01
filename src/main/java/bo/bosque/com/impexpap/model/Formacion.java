package bo.bosque.com.impexpap.model;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.DateDeserializers;
import lombok.Data;
import lombok.NoArgsConstructor;



@Data
@NoArgsConstructor
public class Formacion {

    private int codFormacion;
    private int codEmpleado;
    private String descripcion;
    private int duracion;
    private String tipoDuracion;
    private String tipoFormacion;
    @JsonDeserialize(using = DateDeserializers.DateDeserializer.class) // Especifica el deserializador
    private Date fechaFormacion;
    private int audUsuario;
    // 🔹 Constructor específico para eliminar por codFormacion
    public Formacion(int codFormacion) {
        this.codFormacion = codFormacion;
    }
}