package bo.bosque.com.impexpap.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
public class Permiso {
    private int codPermiso;
    private int codEmpleado;
    private Integer codUsuarioAutorizador;
    private String tipoPermiso;
    private Date desde;
    private Date hasta;
    private String motivo;
    private float cantidadDias;
    private int codRelEmplEmpr;
    private int audUsuarioI;
    private Date audFechaI;

    //auxiliares
    private float cantidadDiasTotal;
    private float cantidadDiasAsig;
    private float cantidadDiasAbon;
    private Date fecRango;



}
