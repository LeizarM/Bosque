package bo.bosque.com.impexpap.model;

import java.io.Serializable;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class EstadoChofer implements Serializable {

    private int idEst;
    private String estado;
    private int audUsuario;

}