package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Precio implements Serializable {

    private int idPrecio;
    private int codigoFamilia;
    private int idClasificacion;
    private float precio;
    private int audUsuario;
}
