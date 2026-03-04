package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.sql.Date;


@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor

public class AfiliacionSeguro implements Serializable {
    private int codAfiliacion;
    private int codEmpleado;
    private int codSeguro;
    private Date fechaAfiliacion;
    private Date fechaBaja;
    private String nroAfiliacion;
    private int audUsuarioI;

    private int codPersona;
    private String nombreCompleto;
    private Seguro seguro= new Seguro();
}
