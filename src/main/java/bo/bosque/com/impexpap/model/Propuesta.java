package bo.bosque.com.impexpap.model;

import lombok.*;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Accessors( chain = true )
public class Propuesta implements Serializable {

    private int idPropuesta;
    private int codEmpresa;
    private int tipo;
    private String titulo;
    private String obs;
    private int estado;
    private int audUsGenerado;
    private Date audFecGenerado;
    private int audUsuario;

    private Date audFecha;



}
