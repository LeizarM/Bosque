package bo.bosque.com.impexpap.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GaranteReferencia {

    private int codGarante;
    private int codPersona;
    private int codEmpleado;
    private String direccionTrabajo;
    private String empresaTrabajo;
    private String tipo;
    private String observacion;

    private int audUsuario;

    /**
     * Variables de apoyo
     */

    private String esEmpleado;
    private String nombreCompleto;
    private String direccionDomicilio;


}
