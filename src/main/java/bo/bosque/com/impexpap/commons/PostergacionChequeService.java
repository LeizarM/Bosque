package bo.bosque.com.impexpap.commons;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.dao.IChPostergacion;
import bo.bosque.com.impexpap.dto.ChequeFilaDto;
import bo.bosque.com.impexpap.dto.PostergacionDto;
import bo.bosque.com.impexpap.dto.PostergacionRequest;
import bo.bosque.com.impexpap.model.ChPostergacion;
import bo.bosque.com.impexpap.security.jwt.DatosToken;

/**
 * El panel "Postergaciones" del detalle de un cheque ({@code cheque.xhtml:341-376, 442-467}, {@code WizardCheque.savePostePostrgcn}
 * y {@code eliminarPostergacion}, {@code ChPostergacionManagedBean}). El PDF de cada postergacion es de
 * {@link PostergacionPdfService}.
 *
 * <h2>Decision 2 de CLAUDE.md: paridad funcional</h2>
 * <ul>
 *   <li>Registrar pide {@value AccesoPanelesCheque#BTN_NUEVO} y permiso de escritura en la sucursal. <b>Eliminar no tiene boton</b>
 *       ({@code cheque.xhtml}: el boton "Eliminar" de la fila no tiene {@code rendered}): solo el rol, como hoy.</li>
 *   <li>La fecha es obligatoria y la observacion mas larga que 2 caracteres, tal cual llega ({@code WizardCheque.java:2247});
 *       sin regex. Orden del legacy: lo obligatorio (JSF), el permiso de sucursal, la observacion.</li>
 *   <li>Solo se registra ({@code 'I'}): {@code nuevaPostergcn} siempre crea {@code codPostergacion = 0}, asi que la rama
 *       {@code 'U'} de {@code p_abm_ChPostergacion} no se usa desde ninguna pantalla y <b>no se expone</b>.</li>
 *   <li>Eliminar <b>no borra el PDF</b> de la postergacion, igual que el legacy: el archivo queda en la carpeta del servidor y
 *       nadie lo ve (el codigo de postergacion es identity y no se reutiliza).</li>
 * </ul>
 *
 * <h2>Lo que el servidor agrega sin cambiar lo que se puede hacer desde la pantalla</h2>
 * Que el cheque exista y su sucursal sea visible; observacion de hasta 200 caracteres (el {@code maxlength} del campo; el
 * procedimiento truncaria en silencio); que la postergacion a eliminar exista y sea de <b>ese</b> cheque ({@code p_abm_ChPostergacion}
 * 'D' borra solo por {@code codPostergacion}); el usuario de auditoria sale del token.
 */
@Service
public class PostergacionChequeService {

    private final IChPostergacion postergaciones;
    private final PostergacionPdfService pdfs;
    private final AccesoPanelesCheque acceso;

    public PostergacionChequeService(IChPostergacion postergaciones, PostergacionPdfService pdfs, AccesoPanelesCheque acceso) {
        this.postergaciones = postergaciones;
        this.pdfs = pdfs;
        this.acceso = acceso;
    }

    /**
     * Las postergaciones del cheque por codigo, con {@code tienePdf} (true / false, o null si la carpeta de PDF no esta
     * disponible: el listado nunca falla por eso). Solo exige {@code btnDetalleCH} (los paneles se alcanzan desde «Completar»): el cheque debe existir y su sucursal ser visible.
     */
    public List<PostergacionDto> listar(Authentication auth, Integer codCheque) {
        ChequeFilaDto ch = acceso.chequeVisible(auth, codCheque);
        List<PostergacionDto> filas = postergaciones.listarPorCheque(ch.getCodCheque());
        for (PostergacionDto f : filas) {
            f.setTienePdf(f.getCodPostergacion() == null ? null : pdfs.tienePdf(f.getCodPostergacion()));
        }
        return filas;
    }

    /**
     * Registra una postergacion ({@code savePostePostrgcn}).
     *
     * @return el {@code codPostergacion} nuevo
     */
    @Transactional
    public long registrar(Authentication auth, PostergacionRequest r) {
        if (r == null) throw new SpBusinessException(MensajesCheque.SIN_DATOS);
        acceso.exigirBoton(auth, AccesoPanelesCheque.BTN_NUEVO);
        ChequeFilaDto ch = acceso.chequeVisible(auth, r.getCodCheque());

        String sinFecha = ReglasPanelesCheque.errorFechaPostergacion(r.getFecha());
        if (sinFecha != null) throw new SpBusinessException(sinFecha);
        acceso.exigirEscritura(auth, ch);
        String obs = ReglasPanelesCheque.errorObservacionPostergacion(r.getObservacion());
        if (obs != null) throw new SpBusinessException(obs);

        ChPostergacion p = new ChPostergacion();
        p.setCodCheque(ch.getCodCheque());
        p.setFecha(r.getFecha());
        p.setObservacion(r.getObservacion());
        p.setAudUsuario(DatosToken.codUsuarioDe(auth));
        return postergaciones.registrar(p).getIdGenerado();
    }

    /**
     * Elimina la postergacion ({@code eliminarPostergacion}). No toca el archivo PDF.
     *
     * @return el {@code codPostergacion} eliminado
     */
    @Transactional
    public long eliminar(Authentication auth, PostergacionRequest r) {
        if (r == null) throw new SpBusinessException(MensajesCheque.SIN_DATOS);
        ChequeFilaDto ch = acceso.chequeVisible(auth, r.getCodCheque());
        Integer cod = r.getCodPostergacion();
        if (cod == null || cod <= 0) throw new SpBusinessException(MensajesCheque.FALTA_POSTERGACION);

        ChPostergacion p = postergaciones.obtener(cod);
        if (p == null) throw new SpBusinessException(MensajesCheque.postergacionNoEncontrada(cod));
        if (p.getCodCheque() == null || !p.getCodCheque().equals(ch.getCodCheque())) {
            throw new SpBusinessException(MensajesCheque.postergacionDeOtroCheque(cod, ch.getCodCheque()));
        }

        postergaciones.eliminar(cod, DatosToken.codUsuarioDe(auth));
        return cod;
    }
}
