package bo.bosque.com.impexpap.model;

import java.io.Serializable;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class TipoContenedor implements Serializable {
    private int idTipo;
    private String tipo;
    private int audUsuario;

}