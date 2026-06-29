package bo.bosque.com.impexpap.dao;


import bo.bosque.com.impexpap.dto.DocumentoProyectoDto;
import bo.bosque.com.impexpap.dto.SolicitudPagoDto;
import bo.bosque.com.impexpap.model.SolicitudPago;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.Date;
import java.util.List;

public interface ISolicitudPago {


    /**
     * Procedimiento para registrar las Solicitudes de Pago
     *
     * @param mb
     * @param acc
     * @return
     */
    RespuestaSp registrarSolicitudPago(SolicitudPago mb, String acc);

    /**
     * Procedimiento para obtener las Solicitudes de Pago por Id
     *
     * @param idSolicitud
     * @return
     */
    List<SolicitudPago> obtenerSolicitudPago(long idSolicitud);

    /**
     * Carga UNA solicitud por su ID exacto (para load-before-update).
     * Solo envía @idSolicitud al SP; los demás params quedan en DEFAULT NULL.
     */
    SolicitudPago obtenerSolicitudPagoPorId(long idSolicitud);

    /**
     * Procedimiento para obtener las Solicitudes de Pago entre fechas
     *
     * @param fechaInicio
     * @param fechaFin
     * @return
     */
    List<SolicitudPagoDto> reporteSolicitudesXFecha(Date fechaInicio, Date fechaFin);

    /**
     * [C] Documentos abiertos de SAP (facturas proveedor / órdenes de compra)
     * filtrados por proyecto. Consulta OPENQUERY a SRV_2022.
     *
     * @param codEmpresa empresa (NULL/0 = todas)
     * @param project    código de proyecto SAP (NULL/vacío = todos)
     */
    List<DocumentoProyectoDto> obtenerDocumentosPorProyecto(int codEmpresa, String project);

}