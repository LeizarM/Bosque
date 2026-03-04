package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor

public class Seguro implements Serializable {
private int codSeguro;
private int codCiudad;
private String nombre;
private String nombreCorto;
private String numero;
private String regional;
private String tipo;
private int audUsuarioI;

private String descripcion;

}
