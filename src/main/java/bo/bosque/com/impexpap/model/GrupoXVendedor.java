package bo.bosque.com.impexpap.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class GrupoXVendedor implements Serializable {

    private long   idGrpVen;
    private long   idVendedor;
    private long   idGrupo;
    private int    estado;
    private int    ignoraComision;
    private Date   fechaInicio;
    private Date   fechaFinalizacion;
    private long   audUsuario;
    private Date   audFecha;

    // Campos de solo lectura que llegan del join.
    private String     nomVenSap;
    private String     grupo;
    private BigDecimal porcentaje;
    private BigDecimal porcenComision;
    private Integer    esParaVenta;
    private Integer    esInterno;
    private Integer    vigente;

}
