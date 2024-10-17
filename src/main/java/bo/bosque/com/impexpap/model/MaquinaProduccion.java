package bo.bosque.com.impexpap.model;

import java.io.Serializable;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class MaquinaProduccion implements Serializable {

    private int idMa;
    private String descripcion;
    private int numero;
    private int audUsuario;

}