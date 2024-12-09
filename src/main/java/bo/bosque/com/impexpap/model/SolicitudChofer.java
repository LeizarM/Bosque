package bo.bosque.com.impexpap.model;

import java.io.Serializable;
import java.util.Date;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudChofer implements Serializable {

    private int idSolicitud;
    private Date fechaSolicitud;
    private String motivo;
    private int codEmpSoli;
    private String cargo;
    private int estado;
    private int idCocheSol;
    private int idES;
    private int audUsuario;

    private String fechaSolicitudCad;
    private String estadoCad;
    private int codSucursal;
    private String coche;

}