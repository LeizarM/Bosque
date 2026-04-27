package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@ToString
@AllArgsConstructor
public class Area implements Serializable {

    private Integer codArea;
    private Integer codEmpresa;
    private String nombreArea;
    private String descripcion;
    private Integer estado;
    private Integer audUsuario;
    private Date audFecha;

}
