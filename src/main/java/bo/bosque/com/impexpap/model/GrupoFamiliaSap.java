package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class GrupoFamiliaSap implements Serializable {

    private int idGrpFamiliaSap;
    private int codGrpFamSap;
    private int codGrpFamSapEpp;
    private String grpFam;
    private String alias;
    private int audUsuario;
}
