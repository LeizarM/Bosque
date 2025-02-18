package bo.bosque.com.impexpap.model;

import java.io.Serializable;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class SocioNegocio implements Serializable {

    private String codCliente;
    private String datoCliente;
    private String razonSocial;
    private String nit;
    private int codCiudad;
    private String datoCiudad;
    private String esVigente;
    private int codEmpresa;
    private int audUsuario;

    private String nombreCompleto;

}