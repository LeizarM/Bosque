package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

/**
 * Cabecera de una solicitud de servicio de corte (tccr_ccrSolicitud).
 *
 * estado: 'SOL' solicitada, 'CNC' cancelada.
 * tipoSolicitud: 'ESP' especial, 'STD' estandar (historico, ya no se genera).
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CcrSolicitud implements Serializable {

    private long idSolicitud;
    private long codEmpresa;
    private int numeracion;
    private String tipoSolicitud;
    private Date fechaSistema;
    private Date fechaSolicitud;
    private long idSolicitante;
    private String datoSolicitante;
    private String estado;
    private String observacion;
    private double totalToneladas;
    private String sapObservacion;
    private double sapToneladas;
    private long audUsuario;

    //===== ATRIBUTOS ADICIONALES (solo lectura, los resuelve el SP)
    private String datoNroSolicitud;   // numeracion con ceros a la izquierda
    private String datoEmpresa;
    private String datoEstado;
    private String datoTipoSolicitud;
    private String fechaSistemaString;
    private String fechaSolicitudString;

}
