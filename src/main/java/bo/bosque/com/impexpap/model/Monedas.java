package bo.bosque.com.impexpap.model;

import java.io.Serializable;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Monedas implements Serializable {


    private long idMoneda;     // bigint en BD (era int)
    private String codigo;
    private String nombre;
    private String simbolo;
    private int decimales;
    private int activo;
    private long audUsuario;   // bigint en BD (era int)

}
