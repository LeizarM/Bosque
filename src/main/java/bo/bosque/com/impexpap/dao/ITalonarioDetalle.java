package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TalonarioDetalle;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

/** Log de eventos de los talonarios (tmto_talonarioDetalle). */
public interface ITalonarioDetalle {

    /**
     * Registrar, corregir o borrar un evento.
     *
     * El INSERT valida la transicion contra el estado actual, que se deriva
     * contando el log. Solo acepta codEstado 2, 3 o 4; el 1 (Adquirido) lo
     * genera el alta del talonario.
     *
     * El UPDATE solo toca destinatario y observacion: un log de eventos no se
     * edita.
     *
     * El DELETE solo permite borrar el ULTIMO evento y nunca el inicial;
     * borrar uno del medio corromperia el conteo del que depende el estado.
     *
     * @param acc Accion ('I', 'U', 'D')
     */
    RespuestaSp registrarTalonarioDetalle(TalonarioDetalle mb, String acc);

    /** Un evento por su id. */
    List<TalonarioDetalle> obtenerTalonarioDetalle(long codDetalle);

    /** Historial completo de un talonario, del evento mas viejo al mas nuevo. */
    List<TalonarioDetalle> listarPorTalonario(long codTalonario);
}
