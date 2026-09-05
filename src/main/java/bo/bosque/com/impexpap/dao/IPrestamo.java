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
     * Obtiene los préstamos vigentes locales (Pestaña 2)
     */
    List<Prestamo> obtenerVigentes(Prestamo p);
    List<Prestamo> obtenerVigentesPorEmpleado(Prestamo p);
    List<Tipos> obtenerTotalPrestamos(Prestamo p);
    List<Tipos> obtenerTotalPrestamosSAP(Prestamo p);

    /**
     * Registra prestamos (ej: asignacion masiva desde SAP)
     */
    RespuestaSp registrarPrestamo(Prestamo p, String acc);
    
    /**
     * Registra pagos masivos desde XML
     */
    RespuestaSp registrarPagoMasivo(PrestamoDetalle p, String acc);
    RespuestaSp revertirPagoMasivo(PrestamoDetalle p);

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
