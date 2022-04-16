package bo.bosque.com.impexpap.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class Persona implements Serializable {

    private int codPersona;
    private int codZona;
    private String nombres;
    private String apPaterno;
    private String apMaterno;
    private String ciExpedido;
    private Date ciFechaVencimiento;
    private String ciNumero;
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
    private Zona zona = new Zona();
    private Pais pais  = new Pais();
    private Ciudad ciudad = new Ciudad();

    /**
     * Constructor
     */

    public Persona(int codPersona, int codZona, String nombres, String apPaterno, String apMaterno, String ciExpedido, Date ciFechaVencimiento, String ciNumero, String direccion, String estadoCivil, Date fechaNacimiento, String lugarNacimiento, int nacionalidad, String sexo, int audUsuarioI) {
        this.codPersona = codPersona;
        this.codZona = codZona;
        this.nombres = nombres;
        this.apPaterno = apPaterno;
        this.apMaterno = apMaterno;
        this.ciExpedido = ciExpedido;
        this.ciFechaVencimiento = ciFechaVencimiento;
        this.ciNumero = ciNumero;
        this.direccion = direccion;
        this.estadoCivil = estadoCivil;
        this.fechaNacimiento = fechaNacimiento;
        this.lugarNacimiento = lugarNacimiento;
        this.nacionalidad = nacionalidad;
        this.sexo = sexo;
        this.audUsuarioI = audUsuarioI;
    }
}
