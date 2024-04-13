package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class LoteProduccion implements Serializable {

    private int idLp;
    private int numLote;
    private int anio;
    private Date fecha;
    private String hraInicioCorte;
    private String hraInicio;
    private String hraFin;
    private int cantBobinasIngresoTotal;
    private float pesoKilosTotalIngreso;
    private float pesoTotalSalida;
    private float pesoPaletaSalida;
    private float pesoMaterialSalida;
    private int cantResmaSalida;
    private float cantHojasSalida;
    private float mermaTotal;
    private float diferenciaProduccion;
    private String obs;
    private int audUsuario;

    //===== ATRIBUTOS ADICIONALES
    private String codArticulo;
    private String datoArt;
    private String articulo;

}