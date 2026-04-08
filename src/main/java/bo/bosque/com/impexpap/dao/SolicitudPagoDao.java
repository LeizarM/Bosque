package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.*;
import bo.bosque.com.impexpap.model.SolicitudPago;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class SolicitudPagoDao implements ISolicitudPago{


    // 1. Variable final, imposible que sea null en tiempo de ejecución
    private final SpHelper spHelper;

    public SolicitudPagoDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    /**
     * Procedimiento para registrar, actualizar o eliminar las Solicitudes de Pago
     *
     * @param mb Objeto con los datos
     * @param acc Acción ('I', 'U', 'D')
     * @return RespuestaSp con el error, errormsg y el idGenerado
     */
    @Override
    public RespuestaSp registrarSolicitudPago(SolicitudPago mb, String acc) {
        // Ejecutamos el SP de ABM. El Helper inyectará automáticamente el @ACCION

        System.out.println("Ejecutando ABM de SolicitudPago con acción: " + acc);
        System.out.println("Datos recibidos: " + mb.toString());

        return this.spHelper.ejecutarAbm("p_abm_tpex_SolicitudPago", mb, acc);
    }

    /**
     * Procedimiento para obtener las Solicitudes de Pago por Id
     *
     * @param idSolicitud El ID a buscar
     * @return Lista de solicitudes (mapeada automáticamente al modelo)
     */
    @Override
    public List<SolicitudPago> obtenerSolicitudPago(long idSolicitud) {
        // 1. Instanciamos un objeto "filtro" y le seteamos solo el parámetro de búsqueda
        SolicitudPago filtro = new SolicitudPago();
        filtro.setIdSolicitud(idSolicitud);

        // 2. Ejecutamos el listado.

        System.out.println("Filtro es: " + filtro.toString());

        return spHelper.ejecutarListado(
                "p_list_tpex_SolicitudPago",
                filtro,
                "L",
                SolicitudPago.class
        );
    }


    /**
     * Carga UNA solicitud por su ID exacto (para load-before-update).
     * Usa el overload Map del SpHelper para enviar SOLO @idSolicitud,
     * evitando que los campos primitivos en 0 (codEmpresa, audUsuario…)
     * interfieran con los filtros AND del SP.
     */
    @Override
    public SolicitudPago obtenerSolicitudPagoPorId(long idSolicitud) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idSolicitud", idSolicitud);
        List<SolicitudPago> resultado = spHelper.ejecutarListado(
                "p_list_tpex_SolicitudPago", filtro, "L", SolicitudPago.class);
        return resultado.isEmpty() ? null : resultado.get(0);
    }


    /**
     * Procedimiento para obtener las Solicitudes de Pago entre fechas
     * @param fechaInicio
     * @param fechaFin
     * @return
     */
    public List<SolicitudPagoDto> reporteSolicitudesXFecha( Date fechaInicio, Date fechaFin ) {

        FiltroFechasDto filtro = new FiltroFechasDto();
        filtro.setFechaInicio(fechaInicio);
        filtro.setFechaFin(fechaFin);

        List<ReportePlanoDto> listaPlana = spHelper.ejecutarListado("p_list_tpex_SolicitudPago", filtro, "B", ReportePlanoDto.class  );

        if (listaPlana == null || listaPlana.isEmpty()) return new ArrayList<>();

        Map<Long, SolicitudPagoDto> mapSolicitudes = new LinkedHashMap<>();
        Map<Long, SolicitudProveedorDto> mapProveedores = new HashMap<>();

        for (ReportePlanoDto fila : listaPlana) {

            // --- A. Agrupar Cabecera ---
            SolicitudPagoDto solicitud = mapSolicitudes.computeIfAbsent(fila.getIdSolicitud(), id -> {
                SolicitudPagoDto s = new SolicitudPagoDto();
                BeanUtils.copyProperties(fila, s); // ¡Magia! Copia todos los atributos que se llamen igual
                return s;
            });

            // --- B. Agrupar Proveedor ---
            if ( fila.getIdSolicitudProveedor() > 0) {
                SolicitudProveedorDto proveedor = mapProveedores.computeIfAbsent(fila.getIdSolicitudProveedor(), idProv -> {
                    SolicitudProveedorDto p = new SolicitudProveedorDto();
                    BeanUtils.copyProperties(fila, p);
                    solicitud.getProveedores().add(p);
                    return p;
                });

                // --- C. Agregar Detalle ---
                if ( fila.getIdDetalle() > 0) {
                    DetalleSolicitudDto detalle = new DetalleSolicitudDto();
                    BeanUtils.copyProperties(fila, detalle);
                    proveedor.getDetalles().add(detalle);
                }
            }
        }

        return new ArrayList<>(mapSolicitudes.values());
    }
}
