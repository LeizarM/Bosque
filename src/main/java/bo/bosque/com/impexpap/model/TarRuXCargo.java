package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.util.Date;



@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class TarRuXCargo extends TareaRutinaria implements Serializable {

    private int idTarXCargo;
    private int idTarRuti;
    private int codCargo;
    private int estado;
    private int audUsuario;
    private int codUsuario;
    private int codEmpleado;

    private int idBitTar;
    private int fueRealizado;
    private String obs;
    private Date fechaPresentacion;
    private TareaRutinaria mbTar = new TareaRutinaria();


}
