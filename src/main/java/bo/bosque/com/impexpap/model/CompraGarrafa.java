package bo.bosque.com.impexpap.model;

import java.io.Serializable;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CompraGarrafa implements Serializable {

    private int idCG;
    private int codSucursal;
    private String descripcion;
    private int cantidad;
    private float monto;
    private int audUsuario;

}