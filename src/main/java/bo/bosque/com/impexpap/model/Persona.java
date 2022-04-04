package bo.bosque.com.impexpap.model;

import lombok.*;
import java.io.Serializable;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Persona implements Serializable {

    private int codPersona;
    private int codZona;
    private String nombres;
    private String apPaterno;
    private String apMaterno;
    private String ciExpedido;
    private Date ciFechaVencimiento;
    private long ciNumuro;
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
    private Pais pais;
    private Ciudad ciudad;
    private Zona zona;
    /**
     * Constructor
     */

    public Persona(int codPersona, int codZona, String nombres, String apPaterno, String apMaterno, String ciExpedido, Date ciFechaVencimiento, long ciNumuro, String direccion, String estadoCivil, Date fechaNacimiento, String lugarNacimiento, int nacionalidad, String sexo, int audUsuarioI) {
        this.codPersona = codPersona;
        this.codZona = codZona;
        this.nombres = nombres;
        this.apPaterno = apPaterno;
        this.apMaterno = apMaterno;
        this.ciExpedido = ciExpedido;
        this.ciFechaVencimiento = ciFechaVencimiento;
        this.ciNumuro = ciNumuro;
        this.direccion = direccion;
        this.estadoCivil = estadoCivil;
        this.fechaNacimiento = fechaNacimiento;
        this.lugarNacimiento = lugarNacimiento;
        this.nacionalidad = nacionalidad;
        this.sexo = sexo;
        this.audUsuarioI = audUsuarioI;
    }
}
