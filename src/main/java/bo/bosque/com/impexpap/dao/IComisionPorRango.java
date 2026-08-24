package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.ComisionPorRango;
import bo.bosque.com.impexpap.model.TipoCambioComision;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

/** Tramos de comision por dias de pago. Mantenimiento reservado a administradores. */
public interface IComisionPorRango {

    /** Alta, modificacion o eliminacion. acc: I, U, D. */
    RespuestaSp registrarRango(ComisionPorRango mb, String acc);

    /** Todos los tramos, ordenados por tipo y dia inicial. */
    List<ComisionPorRango> obtenerRangos();

    /** Tramos de un tipo: Contado o Credito. */
    List<ComisionPorRango> obtenerPorTipo(String tipo);

    /**
     * Tipo de cambio vigente a la fecha indicada, o el mas reciente anterior.
     * Sin fecha usa la de hoy.
     */
    List<TipoCambioComision> obtenerTipoCambio(java.util.Date fecha);
}
