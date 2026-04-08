package bo.bosque.com.impexpap.model;

import java.io.Serializable;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CanalesPago implements Serializable {

    private long idCanal;
    private String nombre;
    private String tipo;
    private String contacto;
    private int activo;
    private long audUsuario;   // bigint en BD (era int)

}