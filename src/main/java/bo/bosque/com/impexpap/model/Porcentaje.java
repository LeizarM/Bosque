package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Porcentaje implements Serializable {

    private int idPorcen;
    private int codigoFamilia;
    private int idClasificacion;
    private int porcen;
    private int audUsuario;

}
