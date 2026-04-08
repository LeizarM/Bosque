package bo.bosque.com.impexpap.model;

import java.io.Serializable;
import java.util.List;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudProveedor implements Serializable {

    private long idSolicitudProveedor;
    private long idSolicitud;
    private String cardCode;
    private String cardName;
    private double totalFacturasUsd;     // decimal(18,2) en BD
    private double totalAmortizadoUsd;   // decimal(18,2) en BD
    private double totalAPagarUsd;       // decimal(18,2) en BD
    private String obs;
    private int audUsuario;

    private int codEmpresa;

    // --- ATRIBUTOS PARA EL JSON ANIDADO ---
    private List<DetalleSolicitud> detalles;
    private List<Long> detallesAEliminar;

}