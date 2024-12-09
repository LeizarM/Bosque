package bo.bosque.com.impexpap.model;

import java.io.Serializable;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class TipoSolicitud implements Serializable {

    private int idES;
    private String descripcion;
    private int estado;
    private int audUsuario;

}