package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class GrupoFamTipoRangoGram implements Serializable {

    private int idGrpFamiliaSap;
    private int idTipo;
    private int idRangoGram;
    private int audUsuario;
}
