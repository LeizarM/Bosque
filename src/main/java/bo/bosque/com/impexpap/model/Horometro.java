package bo.bosque.com.impexpap.model;

import java.io.Serializable;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Horometro implements Serializable {

    private int idH;
    private int idMaquina;
    private float horometroInicial;
    private int audUsuario;

}