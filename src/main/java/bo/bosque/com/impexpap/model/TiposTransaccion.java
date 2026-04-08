package bo.bosque.com.impexpap.model;

import java.io.Serializable;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class TiposTransaccion implements Serializable {

    private long idTipoTransaccion;
    private String codigo;
    private String nombre;
    private String descripcion;
    private int requiereForward;
    private int requiereBanco;
    private int activo;
    private long audUsuario;   // bigint en BD (era int)

}
