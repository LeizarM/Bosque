package bo.bosque.com.impexpap.commons;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.dao.IChCheque;
import bo.bosque.com.impexpap.dto.ChequeFilaDto;

/**
 * Lo que comparten los tres paneles del detalle del cheque (notas de remision, transacciones bancarias y postergaciones) a la
 * hora de decidir quien puede hacer que: el permiso de boton, que el cheque exista, que su sucursal sea visible para el usuario
 * y, al escribir, el permiso de escritura en esa sucursal.
 *
 * <h3>Lo que el legacy hacia, y se conserva</h3>
 * <ul>
 *   <li>Nueva nota, nueva transaccion y nueva postergacion usan el MISMO boton, {@value #BTN_NUEVO}
 *       ({@code cheque.xhtml:279, 311, 342}); eliminar una nota usa {@value #BTN_ELIMINAR_NOTA} ({@code cheque.xhtml:295},
 *       {@code WizardCheque.java:1991-2006}, donde el parametro "cheque cerrado" esta comentado: se ignora). Eliminar una
 *       transaccion o una postergacion y cargar/descargar el PDF de la postergacion no tienen ningun boton
 *       ({@code cheque.xhtml}, sin {@code rendered}). El administrador pasa los botones siempre.</li>
 *   <li>Las tres altas piden permiso de escritura en la sucursal ({@code WizardCheque.java:1033-1041, 2182-2190, 2237-2244}).</li>
 * </ul>
 *
 * <h3>Lo que el servidor agrega (el JSF lo garantizaba ocultando o deshabilitando)</h3>
 * Que el cheque exista y que el usuario pueda <b>ver</b> su sucursal ({@link ChequeService#exigirSucursalLectura}) antes de leer o
 * escribir cualquiera de las tres cosas: en el legacy los paneles solo se alcanzaban desde la fila de un cheque que el usuario ya
 * veia.
 */
@Component
public class AccesoPanelesCheque {

    /** Nueva nota de remision, nueva transaccion bancaria y nueva postergacion. */
    public static final String BTN_NUEVO = "btnNuevoNRCH";

    /** Eliminar una nota de remision. */
    public static final String BTN_ELIMINAR_NOTA = "btnEliminarNRCH";

    /**
     * «Completar» ({@code cheque.xhtml:198}): en el legacy los tres paneles solo se alcanzan desde el detalle de un cheque, asi
     * que quien no tiene este boton no llega a ninguno (auditoria de permisos, diferencia #4; ningun perfil real cambia).
     */
    public static final String BTN_DETALLE = "btnDetalleCH";

    private final IChCheque cheques;
    private final ChequeService chequeService;
    private final AccesoModuloHelper acceso;

    public AccesoPanelesCheque(IChCheque cheques, ChequeService chequeService, AccesoModuloHelper acceso) {
        this.cheques = cheques;
        this.chequeService = chequeService;
        this.acceso = acceso;
    }

    /** El permiso de boton de la vista 42; el administrador pasa. El 403 explica que boton falta. */
    void exigirBoton(Authentication auth, String boton) {
        MensajesCheque.exigirBoton(acceso, auth, ChequeService.VISTA_CHEQUES, boton);
    }

    /**
     * El usuario puede entrar al detalle ({@value #BTN_DETALLE}; el administrador pasa), el cheque existe y puede ver su
     * sucursal (la propia, {@code btnChqSucrs} o administrador).
     */
    ChequeFilaDto chequeVisible(Authentication auth, Integer codCheque) {
        exigirBoton(auth, BTN_DETALLE);
        if (codCheque == null || codCheque <= 0) throw new SpBusinessException(MensajesCheque.FALTA_CHEQUE_DEL_PANEL);
        ChequeFilaDto c = cheques.obtener(codCheque);
        if (c == null) throw new SpBusinessException(MensajesCheque.chequeNoEncontrado(codCheque));
        chequeService.exigirSucursalLectura(auth, c.getCodSucursal());
        return c;
    }

    /** Permiso de escritura en la sucursal del cheque ({@code obtenerPermisoSucursal} o administrador del legacy). */
    void exigirEscritura(Authentication auth, ChequeFilaDto cheque) {
        chequeService.exigirSucursalEscritura(auth, cheque.getCodSucursal());
    }
}
