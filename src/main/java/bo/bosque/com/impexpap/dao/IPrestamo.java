package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Prestamo;
import bo.bosque.com.impexpap.model.PrestamoDetalle;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.Tipos;

import java.util.List;
import java.util.Map;

public interface IPrestamo {

    /**
     * Obtendra los prestamos provenientes de SAP cruzados con la base de datos BOSQUE
     * @param p
     * @return
     */
    List<Prestamo> obtenerPrestamosSAP(Prestamo p);

    /**
     * Registra prestamos (ej: asignacion masiva desde SAP)
     */
    RespuestaSp registrarPrestamo(Prestamo p, String acc);

    List<PrestamoDetalle> previsualizarCuotas(Prestamo p);

    List<Prestamo> listarEmpleadosPrestamo(Prestamo param);

    /**
     * Obtiene los tipos de estado (PEN, CAN, ANU) desde v_tipos (grupo 26)
     */
    List<Tipos> listEstadosPrestamo(Prestamo p);

    /**
     * Obtiene los tipos de pago desde v_tipos (grupo 25)
     */
    List<Tipos> listTiposPagoPrestamo(Prestamo p);

    /**
     * Actualiza una cuota individual de préstamo (ej. cambio de fecha o tipo de pago)
     */
    RespuestaSp actualizarCuotaPrestamo(PrestamoDetalle p, String acc);

    /**
     * Obtener el detalle (amortización) de un préstamo específico
     * @param p Prestamo con codPrestamo
     * @return Lista de detalles
     */
    List<PrestamoDetalle> obtenerDetallesPrestamo(Prestamo p);

    /**
     * Adelantar una cuota de préstamo
     */
    RespuestaSp adelantarCuota(PrestamoDetalle p, String acc);
}
