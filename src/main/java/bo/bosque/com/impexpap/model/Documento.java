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
public class Documento implements Serializable {

    private int idDocumento;
    private int idTipoDoc;
    private int idGestion;
    private int codUsuario;
    private int codEmpleado;
    private String empleadoDe;
    private String cargoDe;
    private String ciudad;
    private String area;
    private int nroCite;
    private Date fechaDoc;
    private String dirigo;
    private String cargoDirigo;
    private String referencia;
    private String via;
    private String cargoVia;
    private String asunto;
    private String cuerpo;
    private String estado;
    private int audUsuario;

    /**
     * Constructores
     */
    public Documento(int idDocumento, int idTipoDoc, int idGestion, int codUsuario, int codEmpleado, String empleadoDe, String cargoDe, String ciudad, String area, int nroCite, Date fechaDoc, String dirigo, String cargoDirigo, String referencia, String via, String cargoVia, String asunto, String cuerpo, String estado, int audUsuario) {
        this.idDocumento = idDocumento;
        this.idTipoDoc = idTipoDoc;
        this.idGestion = idGestion;
        this.codUsuario = codUsuario;
        this.codEmpleado = codEmpleado;
        this.empleadoDe = empleadoDe;
        this.cargoDe = cargoDe;
        this.ciudad = ciudad;
        this.area = area;
        this.nroCite = nroCite;
        this.fechaDoc = fechaDoc;
        this.dirigo = dirigo;
        this.cargoDirigo = cargoDirigo;
        this.referencia = referencia;
        this.via = via;
        this.cargoVia = cargoVia;
        this.asunto = asunto;
        this.cuerpo = cuerpo;
        this.estado = estado;
        this.audUsuario = audUsuario;
    }
}
