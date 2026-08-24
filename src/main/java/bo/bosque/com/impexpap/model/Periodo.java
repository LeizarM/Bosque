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
public class Periodo implements Serializable {

    private long   idPeriodo;
    private int    anio;
    private int    mes;
    private int    esInterno;
    private String estado;              // ABIERTO | CARGADO | EJECUTADO | CERRADO
    private Date   fechaCarga;
    private Date   fechaEjecucion;
    private Long   usuarioCarga;
    private Long   usuarioEjecucion;
    private long   audUsuario;
    private Date   audFecha;

}
