package bo.bosque.com.impexpap.model;

import lombok.*;
import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class Persona implements Serializable {

    private int codPersona;
    private int codZona;
    private String nombres;
    private String apPaterno;
    private String apMaterno;
    private String ciExpedido;
    private Date ciFechaVencimiento;
    private long ciNumero;
    private String direccion;
    private String estadoCivil;
    private Date fechaNacimiento;
    private String lugarNacimiento;
    private int nacionalidad;
    private String sexo;
    private int audUsuarioI;

    /**
     * Variable de apoyo
     */
    private String datoPersona;
    private Pais pais = new Pais();
    private Ciudad ciudad = new Ciudad();
    private Zona zona =  new Zona();

}
