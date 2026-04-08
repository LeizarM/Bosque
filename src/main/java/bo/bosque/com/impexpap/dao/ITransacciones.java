package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Transacciones;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.Date;
import java.util.List;

public interface ITransacciones {

    /**
     * Registrar, actualizar o eliminar una Transacción.
     * @param mb   Objeto con los datos
     * @param acc  Acción ('I', 'U', 'D')
     */
    RespuestaSp registrarTransacciones(Transacciones mb, String acc);

    /**
     * [L] Grilla de transacciones de una solicitud con filtros opcionales.
     * @param idSolicitud       ID de la solicitud (0 = sin filtro)
     * @param cardCode          Código del proveedor (null = sin filtro)
     * @param codBanco          Código del banco    (0 = sin filtro)
     * @param estado            Estado              (null = sin filtro)
     * @param idTipoTransaccion Tipo de transacción (0 = sin filtro)
     * @param codEmpresa        Código de empresa para lookup de proveedores
     */
    List<Transacciones> obtenerTransacciones(long idSolicitud, String cardCode,
                                             int codBanco, String estado,
                                             long idTipoTransaccion, long codEmpresa);

    /**
     * [R] Registro completo de una transacción (para formulario).
     * @param idTransaccion ID de la transacción
     * @param codEmpresa    Código de empresa para lookup de proveedores
     */
    Transacciones obtenerTransaccion(long idTransaccion, long codEmpresa);

    /**
     * [R] Carga UNA transacción por ID exacto (para load-before-update).
     * Solo envía @idTransaccion al SP; todo lo demás queda en DEFAULT NULL.
     */
    Transacciones obtenerTransaccionPorId(long idTransaccion, long codEmpresa);

    /**
     * [C] Transacciones vinculadas a una cotización específica.
     * @param idCotizacion ID de la cotización
     */
    List<Transacciones> obtenerTransaccionesPorCotizacion(long idCotizacion);

    /**
     * [B] Reporte de transacciones entre fechas con filtros opcionales.
     * @param fechaInicio       Fecha inicial (null = sin límite)
     * @param fechaFin          Fecha final   (null = sin límite)
     * @param cardCode          Código del proveedor (null = sin filtro)
     * @param estado            Estado              (null = sin filtro)
     * @param idTipoTransaccion Tipo de transacción (0 = sin filtro)
     * @param codEmpresa        Código de empresa para lookup de proveedores
     */
    List<Transacciones> obtenerTransaccionesReporte(Date fechaInicio, Date fechaFin,
                                                    String cardCode, String estado,
                                                    long idTipoTransaccion, long codEmpresa);

    /**
     * [U-parcial] Actualiza SOLO rutaVoucher de una transacción (ACCION='U').
     * Todos los demás parámetros del SP quedan en NULL → no se sobreescriben.
     *
     * @param idTransaccion  ID de la transacción a actualizar
     * @param rutaVoucher    Ruta relativa del archivo voucher
     * @param audUsuario     ID del usuario que realiza la operación
     */
    RespuestaSp actualizarVoucher(long idTransaccion, String rutaVoucher, int audUsuario);
}
