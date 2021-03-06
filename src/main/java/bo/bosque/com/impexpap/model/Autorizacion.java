package bo.bosque.com.impexpap.model;

import java.io.Serializable;
import java.util.Date;
import lombok.*;
import lombok.experimental.Accessors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Accessors( chain = true)
public class Autorizacion implements Serializable {

    private int idAutorizacion;
    private int idPropuesta;
    private int esAprobada;
    private int audUsuario;//quien autorizo la propuesta
    private Date audFecha;//cuando autorizo la propuesta

    /**
     * Variables auxiliares
     */
    private String datoUsuarioAP; //Nombre del usuario que autorizo la propuesta
    private String datoUsuarioGP; // nombre del usuario que genero la propuesta (exportacion)
    private String datoUsuarioP; //nombre del usuario que creo la propuesta

    private String correo;

    /**
     * Variables auxiliares
     */
    private String estadoCad;
    private Propuesta regProp = new Propuesta();


}
