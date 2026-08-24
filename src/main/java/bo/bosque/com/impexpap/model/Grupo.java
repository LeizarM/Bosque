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
public class Grupo implements Serializable {

    private long       idGrupo;
    private String     grupo;
    private BigDecimal porcentaje;      // decimal(9,6): 0.05 = 5%
    private int        esParaVenta;
    private int        esInterno;
    private Integer    bd;              // empresa SAP; FK a tcom_empresaSap
    private String     siglaEmpresa;    // solo lectura, viene de la vista
    private BigDecimal factorComision;  // porcentaje/100, lo calcula v_tcom_grupo
    private int        activo;
    private long       audUsuario;
    private Date       audFecha;

}
