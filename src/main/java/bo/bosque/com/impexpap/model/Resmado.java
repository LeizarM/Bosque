package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Resmado implements Serializable {

    private int idRes;
    private int idGrupo;
    private int codEmpleado;
    private Date fecha;
    private float total;
    private String hraInicio;
    private String hraFin;
    private int audUsuario;


}