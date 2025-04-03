package bo.bosque.com.impexpap.model;

import java.io.Serializable;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Kilometraje implements Serializable {

    private int idK;
    private int idCoche;
    private float kilometrajeIni;
    private int audUsuario;

}