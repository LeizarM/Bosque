package bo.bosque.com.impexpap.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class FiltroFechasDto {

    private long idSolicitud;
    private int codEmpresa;
    private Date fechaSolicitud;
    private double montoTotalSolicitud;  // decimal(18,2) en BD
    private String estado;
    private int audUsuario;
    private Date audFecha;

    private Date fechaInicio;
    private Date fechaFin;


}