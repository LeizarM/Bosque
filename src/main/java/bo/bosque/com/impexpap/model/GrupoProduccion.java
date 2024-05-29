package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class GrupoProduccion implements Serializable {

    private int idGrupo;
    private String grupo;
    private String descripcion;
    private int audUsuario;

}