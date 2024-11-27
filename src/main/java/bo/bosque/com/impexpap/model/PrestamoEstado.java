package bo.bosque.com.impexpap.model;

import java.io.Serializable;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PrestamoEstado implements Serializable {

    private int idPE;
    private int idPrestamo;
    private int idEst;
    private String momento;
    private int audUsuario;

}