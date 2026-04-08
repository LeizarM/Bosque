package bo.bosque.com.impexpap.model;

import java.io.Serializable;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class TiposCargo implements Serializable {


    private long idTipoCargo;
    private String nombre;
    private int esPorcentaje;
    private int activo;
    private long audUsuario;   // bigint en BD (era int)

}
