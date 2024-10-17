package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ProveedorExtSap implements Serializable {

    private int idProveedorSap;
    private int codProvExtSap;
    private String proveedorExtSap;
    private int audUsuario;
}
