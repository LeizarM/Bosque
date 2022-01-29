package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Propuesta implements Serializable {

    private int idPropuesta;
    private int codEmpresa;
    private int tipo;
    private String titulo;
    private String obs;
    private int estado;
    private int audUsGenerado;
    private Date audFecGenerado;
    private int audFecha;

}
