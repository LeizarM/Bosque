package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.NotaPendiente;

import java.util.List;

/**
 * Notas cerradas pendientes de pago.
 * <p>
 * En Bosque v2 esta lista existia en la pantalla pero nunca se llenaba: el
 * metodo que la cargaba estaba comentado, asi que el dialogo salia siempre
 * vacio. Solo funcionaba el PDF.
 */
public interface INotaPendiente {

    /** Notas pendientes con sus totales por vendedor. Rama B del SP heredado. */
    List<NotaPendiente> obtenerPendientes();
}
