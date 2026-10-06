package bo.bosque.com.impexpap.commons;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.dao.IChNotaRemision;
import bo.bosque.com.impexpap.dto.ChequeFilaDto;
import bo.bosque.com.impexpap.dto.NotaRemisionDto;
import bo.bosque.com.impexpap.dto.NotaRemisionRequest;
import bo.bosque.com.impexpap.model.ChNotaRemision;
import bo.bosque.com.impexpap.security.jwt.DatosToken;

/**
 * El panel "Notas de remision" del detalle de un cheque ({@code cheque.xhtml:278-310}, {@code WizardCheque.saveNotaRemision}
 * y {@code eliminarNotaRemision}, {@code NotaRemisionManagedBean}).
 *
 * <h2>Decision 2 de CLAUDE.md: paridad funcional</h2>
 * <ul>
 *   <li>Registrar pide {@value AccesoPanelesCheque#BTN_NUEVO} y permiso de escritura en la sucursal; eliminar pide
 *       {@value AccesoPanelesCheque#BTN_ELIMINAR_NOTA} y <b>no mira si el cheque esta cerrado</b>.</li>
 *   <li>Los campos cumplen {@link ReglasPanelesCheque} (nota: {@code ^[0-9  ]{5,10}}; factura mayor que cero; fecha obligatoria).</li>
 *   <li><b>No hay validacion de duplicados</b> (ni en Java ni en el procedimiento): {@code NotaRemisionDao.existeRegistro} nunca se
 *       usa. En {@code BOSQUE2PRUEBA} hay 41 pares (cheque, nota) repetidos. Se conserva, y como
 *       {@code p_abm_NotaRemision} 'D' borra por (cheque, nota) <b>todas</b> las filas del par, eliminar una repetida las borra
 *       todas: el mensaje lo dice.</li>
 * </ul>
 *
 * <h2>Lo que el servidor agrega sin cambiar lo que se puede hacer desde la pantalla</h2>
 * Que el cheque exista y su sucursal sea visible para el usuario ({@link AccesoPanelesCheque}); factura de hasta 6 digitos
 * (el {@code maxlength} del campo); que la nota a eliminar exista para <b>ese</b> cheque, con un mensaje claro (el legacy decia
 * "No se Elimino el Registro"); el usuario de auditoria sale del token.
 *
 * <p>Orden de las validaciones al registrar: boton, cheque y sucursal visible, campos (todos los mensajes juntos, uno por
 * linea, como el JSF), permiso de escritura en la sucursal.
 */
@Service
public class NotaRemisionChequeService {

    private final IChNotaRemision notas;
    private final AccesoPanelesCheque acceso;

    public NotaRemisionChequeService(IChNotaRemision notas, AccesoPanelesCheque acceso) {
        this.notas = notas;
        this.acceso = acceso;
    }

    /** Las notas del cheque. Solo exige {@code btnDetalleCH} (los paneles se alcanzan desde «Completar»): el cheque debe existir y su sucursal ser visible. */
    public List<NotaRemisionDto> listar(Authentication auth, Integer codCheque) {
        ChequeFilaDto ch = acceso.chequeVisible(auth, codCheque);
        return notas.listarPorCheque(ch.getCodCheque());
    }

    /**
     * Registra una nota ({@code saveNotaRemision}).
     *
     * @return las filas insertadas (siempre 1)
     */
    @Transactional
    public int registrar(Authentication auth, NotaRemisionRequest r) {
        if (r == null) throw new SpBusinessException(MensajesCheque.SIN_DATOS);
        acceso.exigirBoton(auth, AccesoPanelesCheque.BTN_NUEVO);
        ChequeFilaDto ch = acceso.chequeVisible(auth, r.getCodCheque());

        List<String> errores = ReglasPanelesCheque.erroresNotaRemision(r.getNotaRemision(), r.getNroFactura(), r.getFechaFactura());
        if (!errores.isEmpty()) throw new SpBusinessException(String.join("\n", errores));
        acceso.exigirEscritura(auth, ch);

        ChNotaRemision n = new ChNotaRemision();
        n.setCodCheque(ch.getCodCheque());
        n.setNotaRemision(r.getNotaRemision());
        n.setNroFactura(r.getNroFactura());
        n.setFechaFactura(r.getFechaFactura());
        n.setAudUsuario(DatosToken.codUsuarioDe(auth));
        notas.registrar(n);
        return 1;
    }

    /**
     * Elimina la nota ({@code eliminarNotaRemision}): todas las filas del cheque con ese numero, como el legacy.
     *
     * @return las filas eliminadas (1 o mas; mas de una si la nota estaba repetida en el cheque)
     */
    @Transactional
    public int eliminar(Authentication auth, NotaRemisionRequest r) {
        if (r == null) throw new SpBusinessException(MensajesCheque.SIN_DATOS);
        acceso.exigirBoton(auth, AccesoPanelesCheque.BTN_ELIMINAR_NOTA);
        ChequeFilaDto ch = acceso.chequeVisible(auth, r.getCodCheque());
        String nota = r.getNotaRemision();
        if (nota == null || nota.isEmpty()) throw new SpBusinessException(MensajesCheque.FALTA_NOTA_A_ELIMINAR);

        int existentes = 0;
        for (NotaRemisionDto f : notas.listarPorCheque(ch.getCodCheque())) {
            if (ReglasPanelesCheque.mismoValorSql(f.getNotaRemision(), nota)) existentes++;
        }
        if (existentes == 0) throw new SpBusinessException(MensajesCheque.notaNoEncontrada(ch.getCodCheque(), nota));

        notas.eliminar(ch.getCodCheque(), nota, DatosToken.codUsuarioDe(auth));
        return existentes;
    }
}
