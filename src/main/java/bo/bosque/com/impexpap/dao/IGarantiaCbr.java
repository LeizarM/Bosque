package bo.bosque.com.impexpap.dao;

import java.util.List;

import bo.bosque.com.impexpap.dto.ClienteSapDto;
import bo.bosque.com.impexpap.dto.EntregaGarantiaRptDto;
import bo.bosque.com.impexpap.dto.FiltroGarantiaDto;
import bo.bosque.com.impexpap.dto.GarantiaDto;
import bo.bosque.com.impexpap.dto.GarantiaResumenClienteDto;
import bo.bosque.com.impexpap.dto.ReciboGarantiaRptDto;
import bo.bosque.com.impexpap.model.GarantiaCbr;
import bo.bosque.com.impexpap.utils.RespuestaSp;

/**
 * Acceso a <b>tcbr_garantia</b>. Procedimientos: {@code p_abm_GarantiaCbr} (I, A, U, B, D)
 * y {@code p_list_GarantiaCbr} (L, A, C, D, E, T del legacy; S, R, G agregadas en tcbr_04).
 */
public interface IGarantiaCbr {

    /** Una fila por cliente con garantias. ACCION 'R'. */
    List<GarantiaResumenClienteDto> resumenPorCliente(String buscar);

    /** Garantias con estado y datos calculados. ACCION 'G'. */
    List<GarantiaDto> listar(FiltroGarantiaDto filtro);

    /** Una garantia con sus datos calculados, o {@code null} si no existe. ACCION 'G'. */
    GarantiaDto obtener(long codGarantia);

    /** Clientes de SAP por codigo o nombre, maximo 50. ACCION 'S'. */
    List<ClienteSapDto> buscarClientesSap(String buscar);

    /**
     * Alta de la garantia y de su accion REG (el procedimiento las graba en una transaccion).
     * ABM ACCION 'A'. {@code idGenerado} = codGarantia nuevo.
     */
    RespuestaSp alta(GarantiaCbr garantia, String observacion);

    /** Reemplaza todas las columnas editables menos el cliente. ABM ACCION 'U'. */
    RespuestaSp actualizar(GarantiaCbr garantia);

    /** Solo reconocimiento de firmas y numero de protesta. ABM ACCION 'B'. */
    RespuestaSp actualizarFirmas(long codGarantia, String recFirmas, String nroProtesta, long audUsuario);

    /** Filas del recibo de una garantia, una sola por garantia. LIST ACCION 'D'. */
    List<ReciboGarantiaRptDto> recibo(long codGarantia);

    /** Nomina del ultimo traspaso, una fila por garantia. LIST ACCION 'T'. */
    List<EntregaGarantiaRptDto> ultimoTraspaso();
}
