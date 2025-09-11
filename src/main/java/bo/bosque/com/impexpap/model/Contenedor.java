package bo.bosque.com.impexpap.model;

import java.io.Serializable;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Contenedor implements Serializable {

    private int idContenedor;
    private String codigo;
    private int idTipo;
    private int codSucursal;
    private String descripcion;
    private String unidadMedida;
    private int audUsuario;


    /**
     * Variables adicionales para la implementación del modelo de datos
     */
    private String clase;
    private float saldoActualCombustible;
    private String nombreSucursal;
    private int codCiudad;
}