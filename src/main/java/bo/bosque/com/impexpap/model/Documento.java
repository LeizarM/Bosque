package bo.bosque.com.impexpap.model;

import java.io.Serializable;
import java.util.Date;
import lombok.*;



@Data
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
    private String dirigido;
    private String cargoDirigo;
    private String referencia;
    private String via;
    private String cargoVia;
    private String asunto;
    private String cuerpo;
    private String estado;
    private int audUsuario;


}
