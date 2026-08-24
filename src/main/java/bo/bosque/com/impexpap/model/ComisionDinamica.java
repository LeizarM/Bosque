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
public class ComisionDinamica implements Serializable {

    private long       idDc;
    private int        esInterno;
    private BigDecimal metaUsd;
    private BigDecimal metaBs;
    private BigDecimal porcentaje;      // decimal(9,6): 0.05 = 5%
    private Date       vigenteDesde;
    private Date       vigenteHasta;
    private long       audUsuario;
    private Date       audFecha;

}
