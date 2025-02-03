package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.util.Date;


@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class TareaRutinaria implements Serializable {


    private int idTarRuti;
    private int idFrec;
    private int idArea;
    private Date fechaPartida;
    private int iniFin;
    private int idATR;
    private String descripcion;
    private int audUsuario;

    private String descFrec;


}
