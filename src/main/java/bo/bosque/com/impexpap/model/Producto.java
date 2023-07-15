package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;


@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Producto implements Serializable {

    private int codigoFamilia;
    private int idGrpFamiliaSap;
    private int idProveedorSap;
    private int idPresentacion;
    private int idTipo;
    private int idRangoGramaje;
    private String formato;
    private String gramaje;
    private int idColor;
    private int estado;
    private float costoTM;
    private int idPropuestaAprobada;
    private int audUsuario;

    /**
     * Variables de Apoyo
     */
    private String nombreProveedor;
    private String nombreFamilia;
    private String presentacion;
    private String color;
    private String tipo;
    private String rangoGramaje;
}
