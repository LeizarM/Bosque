package bo.bosque.com.impexpap.commons;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.dao.IChBanco;
import bo.bosque.com.impexpap.dao.IChTransaccionBancaria;
import bo.bosque.com.impexpap.dto.ChequeFilaDto;
import bo.bosque.com.impexpap.dto.TransaccionBancariaDto;
import bo.bosque.com.impexpap.dto.TransaccionBancariaRequest;
import bo.bosque.com.impexpap.model.ChTransaccionBancaria;
import bo.bosque.com.impexpap.security.jwt.DatosToken;

/**
 * El panel "Transacciones bancarias" del detalle de un cheque ({@code cheque.xhtml:310-340},
 * {@code WizardCheque.saveNroTransaccion} y {@code eliminarNroTransaccion}, {@code ChTransaccionBancariaManagedBean}).
 *
 * <h2>Decision 2 de CLAUDE.md: paridad funcional</h2>
 * <ul>
 *   <li>Registrar pide {@value AccesoPanelesCheque#BTN_NUEVO} y permiso de escritura en la sucursal. <b>Eliminar no tiene boton</b>
 *       ({@code cheque.xhtml}: el boton "Eliminar" de la fila no tiene {@code rendered}): solo el rol, como hoy.</li>
 *   <li>El numero es obligatorio y mas largo que 4 caracteres, tal cual llega ({@code WizardCheque.java:2193}); la fecha es
 *       obligatoria y sin rango; no hay regex. Orden del legacy: lo obligatorio (JSF), el permiso de sucursal, el largo.</li>
 *   <li>No hay validacion de duplicados; eliminar borra por (cheque, numero) todas las filas que coincidan.</li>
 * </ul>
 *
 * <h2>Lo que el servidor agrega sin cambiar lo que se puede hacer desde la pantalla</h2>
 * Que el cheque exista y su sucursal sea visible; numero de hasta 30 caracteres (la columna es {@code varchar(30)} y el
 * procedimiento truncaria en silencio); que el banco exista (el JSF lo garantizaba con el desplegable y el listado hace
 * {@code JOIN} con {@code tch_banco}: sin banco la transaccion no se veria); que la transaccion a eliminar sea de <b>ese</b>
 * cheque; el usuario de auditoria sale del token.
 */
@Service
public class TransaccionChequeService {

    private final IChTransaccionBancaria transacciones;
    private final IChBanco bancos;
    private final AccesoPanelesCheque acceso;

    public TransaccionChequeService(IChTransaccionBancaria transacciones, IChBanco bancos, AccesoPanelesCheque acceso) {
        this.transacciones = transacciones;
        this.bancos = bancos;
        this.acceso = acceso;
    }

    /** Las transacciones del cheque. Solo exige {@code btnDetalleCH} (los paneles se alcanzan desde «Completar»): el cheque debe existir y su sucursal ser visible. */
    public List<TransaccionBancariaDto> listar(Authentication auth, Integer codCheque) {
        ChequeFilaDto ch = acceso.chequeVisible(auth, codCheque);
        return transacciones.listarPorCheque(ch.getCodCheque());
    }

    /**
     * Registra una transaccion ({@code saveNroTransaccion}).
     *
     * @return las filas insertadas (siempre 1)
     */
    @Transactional
    public int registrar(Authentication auth, TransaccionBancariaRequest r) {
        if (r == null) throw new SpBusinessException(MensajesCheque.SIN_DATOS);
        acceso.exigirBoton(auth, AccesoPanelesCheque.BTN_NUEVO);
        ChequeFilaDto ch = acceso.chequeVisible(auth, r.getCodCheque());

        // 1. lo que el JSF exigia con required (antes de llamar al guardado)
        List<String> obligatorios = new ArrayList<>();
        agregar(obligatorios, ReglasPanelesCheque.errorNroTransaccionObligatorio(r.getNroTransaccion()));
        agregar(obligatorios, ReglasPanelesCheque.errorFechaTransaccion(r.getFechaTransaccion()));
        if (!obligatorios.isEmpty()) throw new SpBusinessException(String.join("\n", obligatorios));

        // 2. el guardado: primero el permiso de sucursal y despues el largo del numero
        acceso.exigirEscritura(auth, ch);
        List<String> errores = new ArrayList<>();
        agregar(errores, ReglasPanelesCheque.errorLargoNroTransaccion(r.getNroTransaccion()));
        agregar(errores, errorDeBanco(r.getCodBanco()));
        if (!errores.isEmpty()) throw new SpBusinessException(String.join("\n", errores));

        ChTransaccionBancaria t = new ChTransaccionBancaria();
        t.setCodCheque(ch.getCodCheque());
        t.setNroTransaccion(r.getNroTransaccion());
        t.setCodBanco(r.getCodBanco());
        t.setFechaTransaccion(r.getFechaTransaccion());
        t.setAudUsuario(DatosToken.codUsuarioDe(auth));
        transacciones.registrar(t);
        return 1;
    }

    /**
     * Elimina la transaccion ({@code eliminarNroTransaccion}): todas las filas del cheque con ese numero.
     *
     * @return las filas eliminadas
     */
    @Transactional
    public int eliminar(Authentication auth, TransaccionBancariaRequest r) {
        if (r == null) throw new SpBusinessException(MensajesCheque.SIN_DATOS);
        ChequeFilaDto ch = acceso.chequeVisible(auth, r.getCodCheque());
        String nro = r.getNroTransaccion();
        if (nro == null || nro.isEmpty()) throw new SpBusinessException(MensajesCheque.FALTA_TRANSACCION_A_ELIMINAR);

        int existentes = 0;
        for (TransaccionBancariaDto f : transacciones.listarPorCheque(ch.getCodCheque())) {
            if (ReglasPanelesCheque.mismoValorSql(f.getNroTransaccion(), nro)) existentes++;
        }
        if (existentes == 0) throw new SpBusinessException(MensajesCheque.transaccionNoEncontrada(ch.getCodCheque(), nro));

        transacciones.eliminar(ch.getCodCheque(), nro, DatosToken.codUsuarioDe(auth));
        return existentes;
    }

    private String errorDeBanco(Integer codBanco) {
        if (codBanco == null || codBanco <= 0) return MensajesCheque.FALTA_BANCO_DE_TRANSACCION;
        return bancos.obtener(codBanco) == null ? MensajesCheque.bancoNoEncontrado(codBanco) : null;
    }

    private static void agregar(List<String> errores, String error) {
        if (error != null) errores.add(error);
    }
}
