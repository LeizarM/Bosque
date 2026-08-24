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
public class EmpresaSap implements Serializable {

    private int    codEmpresa;
    private String sigla;
    private String nombre;
    private String baseDatosSap;
    private int    activo;
    private long   audUsuario;
    private Date   audFecha;

}
