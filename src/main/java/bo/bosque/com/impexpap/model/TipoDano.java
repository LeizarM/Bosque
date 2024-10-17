package bo.bosque.com.impexpap.model;

import java.io.Serializable;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class TipoDano implements Serializable {

    private int idTd;
    private String descripcion;
    private int audusuario;

}