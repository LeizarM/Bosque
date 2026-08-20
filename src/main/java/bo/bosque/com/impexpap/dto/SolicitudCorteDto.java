package bo.bosque.com.impexpap.dto;

import bo.bosque.com.impexpap.model.CcrSolicitud;
import bo.bosque.com.impexpap.model.CcrSolicitudDetalle;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Lo que viaja al registrar o consultar una solicitud de corte.
 *
 * La cabecera y su detalle van juntos porque se guardan en una sola operacion:
 * una solicitud sin items no significa nada.
 */
@Getter
@Setter
public class SolicitudCorteDto {

    private CcrSolicitud solicitud;
    private List<CcrSolicitudDetalle> detalle = new ArrayList<>();

    // ── Para las consultas y los reportes ───────────────────────────────────
    private long idSolicitud;
    private String observacion;
    private long audUsuario;
    private Date fechaIni;
    private Date fechaFin;

    // ── Para buscar en el catalogo SAP ──────────────────────────────────────
    private String texto;
    private int limite;

}
