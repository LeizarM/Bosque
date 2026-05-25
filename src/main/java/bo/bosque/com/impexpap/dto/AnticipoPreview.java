// Archivo: bo.bosque.com.impexpap.model.AnticipoPreview.java
package bo.bosque.com.impexpap.dto;

import lombok.*;
import java.io.Serializable;
@Data
@NoArgsConstructor

public class AnticipoPreview implements Serializable {
    private Long codEmpleado;
    private String nombreCompleto;
    private String tipo;
    private Double montoCalculado;
    private Double montoSAP;
    private Double sumaTotalCalculada;
    private Double diferenciaGlobal;
    private Integer esValido; // 1 = Ok, 0 = Error
}