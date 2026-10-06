package bo.bosque.com.impexpap.commons;

/**
 * Los textos de "Actualizar datos SAP" (traer los clientes nuevos de SAP), en un solo lugar y sin ninguna regla de
 * negocio. Mismo estilo que {@link MensajesCheque}: cada mensaje dice <i>que</i> fallo, <i>por que</i> y <i>que
 * hacer</i>; espanol neutro, tuteo. El legacy solo decia "no se pudieron actualizar los registros".
 */
public final class MensajesClientesSap {

    private MensajesClientesSap() {
    }

    /** El permiso que exige la accion (el mismo que "Registrar" en el legacy: {@code btnNuevoCH}). */
    public static String sinPermiso(String boton) {
        return "No tienes permiso para traer los clientes nuevos de SAP. Esta acción usa el botón " + boton
                + " de la pantalla de Cheques (el mismo de «Registrar»), que tu usuario no tiene asignado; "
                + "pídele al administrador que te lo asigne.";
    }

    public static final String YA_EN_CURSO =
            "Ya hay una actualización de clientes de SAP en curso (la pidió otro usuario o tú hace un momento). "
                    + "Espera a que termine y vuelve a abrir la lista de clientes; no hace falta repetirla.";

    /**
     * Cualquier falla de base de datos. "Lo mas probable" porque la unica consulta que sale del servidor es la de SAP. No se
     * promete que "no se modifico nada": el procedimiento no es atomico (actualiza y despues inserta), y es el mismo que usa
     * Depositos, asi que no se altera.
     */
    public static final String NO_SE_PUDO =
            "No se pudieron traer los clientes de SAP. Lo más probable es que el servidor de SAP no esté respondiendo en "
                    + "este momento. Intenta de nuevo en unos minutos y, si el error se repite, avisa a Sistemas.";

    /** El resultado. El procedimiento no dice cuantos clientes trajo, asi que no se da un numero. */
    public static final String ACTUALIZADO =
            "Clientes actualizados desde SAP. Los cheques de los clientes que antes no estaban cargados ya aparecen en la lista.";
}
