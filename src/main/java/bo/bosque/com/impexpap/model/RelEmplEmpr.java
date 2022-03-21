package bo.bosque.com.impexpap.model;

import lombok.*;

import java.util.Date;

@Data
public class RelEmplEmpr {

    private int codRelEmplEmpr;
    private int codEmpleado;
    private int esActivo;
    private String tipoRel;
    private String nombreFileContrato;
    private Date fechaIni;
    private Date fechaFin;
    private String motivoFin;
    private int audUsuario;
}
