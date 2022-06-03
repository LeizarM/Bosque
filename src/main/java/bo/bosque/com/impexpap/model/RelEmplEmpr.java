package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class RelEmplEmpr implements Serializable {

    private int codRelEmplEmpr;
    private int codEmpleado;
    private int esActivo;
    private String tipoRel;
    private String nombreFileContrato;
    private Date fechaIni;
    private Date fechaFin;
    private String motivoFin;
    private int audUsuario;


    private Date fechaInicioBeneficio;
    private Date fechaInicioPlanilla;

    /**
     * Variables de apoyo
     */
    private String datoFechasBeneficio;
}
