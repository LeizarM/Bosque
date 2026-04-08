package bo.bosque.com.impexpap.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
@Setter
public class SolicitudPagoDto {
    private long idSolicitud;
    private int codEmpresa;
    private String nombre; // Importante: Te faltaba el nombre de la empresa que devuelve el JOIN
    private Date fechaSolicitud;
    private double montoTotalSolicitud;  // decimal(18,2) en BD
    private String estado;
    private int audUsuario;

    // CORRECCIÓN: Usar la lista del DTO, no del Modelo
    private List<SolicitudProveedorDto> proveedores = new ArrayList<>();
}