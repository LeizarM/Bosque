package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.EstadoPeriodo;
import bo.bosque.com.impexpap.model.SincronizacionNotas;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

/**
 * Carga y ejecucion del periodo de comisiones.
 * <p>
 * Por debajo llama a p_abm_paraPagar y p_abm_pagado, los mismos SP que usa
 * Bosque v2. Los envoltorios p_abm_tcom_* agregan el contrato de error y la
 * proteccion contra doble ejecucion.
 */
public interface IEjecucionComision {

    /** Indica si el periodo ya fue ejecutado, y con cuantos registros. */
    List<EstadoPeriodo> obtenerEstadoPeriodo(int mes, int anio, int esInterno);

    /**
     * Prepara las notas del periodo. Reversible: no mueve dinero.
     * Falla si el periodo ya fue ejecutado.
     */
    RespuestaSp cargarNotas(int mes, int anio, int esInterno, long audUsuario);

    /**
     * Realiza el corte y marca las notas como pagadas.
     * <b>No es reversible desde la aplicacion.</b>
     */
    RespuestaSp ejecutarPago(int mes, int anio, int esInterno, long audUsuario);

    /**
     * Trae las notas de SAP, siempre.
     *
     * <p>No llama a {@code p_abm_noPagado} directo: pasa por
     * {@code p_abm_tcom_SincronizarNotas}, que pone el candado y mira la
     * antigüedad. Ese proc no tiene candado propio —su guardia contra duplicados
     * es un {@code DocNum not in (select ...)}, o sea leer y después insertar— y
     * colgarlo de una pantalla que abren varios es fabricar notas repetidas.
     */
    List<SincronizacionNotas> sincronizarNotas( long audUsuario );

}
