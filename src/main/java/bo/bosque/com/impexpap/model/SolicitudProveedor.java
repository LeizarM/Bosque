package bo.bosque.com.impexpap.model;

import java.io.Serializable;

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
    private float totalFacturasUsd;
    private float totalAmortizadoUsd;
    private float totalAPagarUsd;
    private String obs;
    private int audUsuario;

    private int codEmpresa;

}