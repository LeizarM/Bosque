package bo.bosque.com.impexpap.model;

import java.io.Serializable;
import java.util.Date;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudPago implements Serializable {

    private long idSolicitud;
    private int codEmpresa;
    private Date fechaSolicitud;
    private float montoTotalSolicitud;
    private String estado;
    private int audUsuario;

}