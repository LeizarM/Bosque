package bo.bosque.com.impexpap.commons;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.dao.IChBanco;
import bo.bosque.com.impexpap.dao.IChCheque;
import bo.bosque.com.impexpap.dao.IChVerificacionDeposito;
import bo.bosque.com.impexpap.dto.ChequeFilaDto;
import bo.bosque.com.impexpap.dto.ChequePendienteFilaDto;
import bo.bosque.com.impexpap.dto.ChequePendienteFiltroDto;
import bo.bosque.com.impexpap.dto.FilaConChequeDto;
import bo.bosque.com.impexpap.dto.PrepararVerificacionDto;
import bo.bosque.com.impexpap.dto.VerificacionDepositoFilaDto;
import bo.bosque.com.impexpap.dto.VerificacionFiltroDto;
import bo.bosque.com.impexpap.dto.VerificacionPaginaDto;
import bo.bosque.com.impexpap.dto.VerificacionRegistroDto;
import bo.bosque.com.impexpap.model.ChVerificacionDeposito;
import bo.bosque.com.impexpap.security.jwt.DatosToken;

/**
 * Las operaciones de la pantalla "Verificar Cheques" (vista 77, tabla {@code tch_verificacionDeposito}), con las
 * reglas que el legacy aplicaba en {@code WizardVerificacionDeDepositos} y {@code VerificacionDepositoManagedBean}.
 *
 * <h2>Decision 2 de CLAUDE.md: paridad funcional</h2>
 * Las reglas se mantienen tal como el legacy las aplica hoy. Solo se corrigen detalles tecnicos: la paginacion se corta
 * aqui (el JSF cargaba todo en memoria), el usuario de auditoria sale del token y el numero de cheque se lee como texto
 * (el legacy lo leia con {@code getInt} y un solo cheque con guion tiraba la lista entera).
 *
 * <h2>Lo que el servidor impone y el JSF solo insinuaba</h2>
 * El JSF ocultaba botones y filtraba listas; el servidor no puede confiar en eso:
 * <ul>
 *   <li><b>No se verifica un cheque cerrado</b>: el boton "Seleccionar" no se dibujaba si el estado era CERRADO.</li>
 *   <li><b>Una sola verificacion valida (Y) por cheque</b>: la lista de seleccion solo traia los cheques sin ninguna Y.</li>
 *   <li>El banco de la verificacion tiene que existir: el combo solo ofrecia bancos existentes.</li>
 *   <li>El estado lo decide el servidor: el alta es {@code Y}; la edicion conserva el que tenia; "Cancelar" es otro
 *       endpoint ({@code N}).</li>
 * </ul>
 *
 * <h2>Reglas heredadas que se conservan a proposito</h2>
 * Editar una verificacion <b>no</b> vuelve a exigir que el cheque este abierto ni que no haya otra {@code Y}: el JSF no
 * lo hacia (editaba el registro cargado, y editar una anulada la deja anulada). "Cancelar" una verificacion de un cheque
 * cerrado tambien se permite. No hay borrado: el legacy no lo ofrece.
 *
 * <h2>Costo conocido: la moneda</h2>
 * Los procedimientos de verificacion no devuelven la moneda del cheque. Para poder separar Bs de $us, el servicio la lee
 * con {@link IChCheque#obtener} (una vez por cheque distinto, <b>solo de la pagina</b>: hasta {@code tamanio} lecturas por
 * pagina, 20 por defecto). Si la lectura falla o no encuentra el cheque, {@code moneda} queda en null: nunca rompe el
 * listado.
 *
 * <p>No hay permiso por boton ni por sucursal: la vista 77 no tiene botones en {@code tb_vistaBtn} y los procedimientos
 * no filtran por sucursal. Solo se exige JWT y rol, como el legacy.
 */
@Service
public class VerificacionChequeService {

    private static final Logger log = LoggerFactory.getLogger(VerificacionChequeService.class);

    /** La zona del proyecto: "hoy" es el de La Paz y no el de la JVM del contenedor. */
    static final ZoneId ZONA = ZoneId.of("America/La_Paz");

    static final int TAMANIO_PAGINA = 20;
    static final int TAMANIO_PAGINA_MAXIMO = 200;

    /** Estado de la verificacion: Y = valida, N = anulada ({@code v_tipos} grupo 36). */
    static final String VALIDA = "Y";
    static final String ANULADA = "N";

    private final IChVerificacionDeposito verificaciones;
    private final IChBanco bancos;
    private final IChCheque cheques;
    private final Clock reloj;

    @Autowired
    public VerificacionChequeService(IChVerificacionDeposito verificaciones, IChBanco bancos, IChCheque cheques) {
        this(verificaciones, bancos, cheques, Clock.system(ZONA));
    }

    /** Para las pruebas: un reloj fijo. */
    VerificacionChequeService(IChVerificacionDeposito verificaciones, IChBanco bancos, IChCheque cheques, Clock reloj) {
        this.verificaciones = verificaciones;
        this.bancos = bancos;
        this.cheques = cheques;
        this.reloj = reloj;
    }

    // ===================================================================== //
    //                              LECTURAS                                 //
    // ===================================================================== //

    /**
     * La lista principal ("NOMINA DE CHEQUES DEPOSITADOS - VERIFICADOS"): las verificaciones de un dia, o todas si no se
     * indica. Orden estable por {@code codvd} descendente (el procedimiento no ordena y el legacy mostraba el orden
     * fisico de la tabla): lo mas reciente primero.
     */
    public VerificacionPaginaDto<VerificacionDepositoFilaDto> listar(VerificacionFiltroDto f) {
        LocalDate dia = f == null ? null : f.getFechaBanco();
        List<VerificacionDepositoFilaDto> todas = new ArrayList<>(verificaciones.listar(dia));
        todas.sort(Comparator.comparing(VerificacionDepositoFilaDto::getCodvd,
                Comparator.nullsLast(Comparator.<Integer>reverseOrder())));

        VerificacionPaginaDto<VerificacionDepositoFilaDto> pagina = paginar(todas,
                f == null ? null : f.getPagina(), f == null ? null : f.getTamanio(), VerificacionDepositoFilaDto::setFila);
        completar(pagina.getFilas());
        return pagina;
    }

    /**
     * Los cheques que todavia no tienen una verificacion valida: el modal "Cheques Pendientes Sin Regularizar". El orden es
     * el del procedimiento (fecha de cobranza descendente, numero de cheque).
     */
    public VerificacionPaginaDto<ChequePendienteFilaDto> pendientes(ChequePendienteFiltroDto f) {
        String estado = estadoDeChequeOpcional(f == null ? null : f.getEstado());
        boolean soloHoy = f == null || !Boolean.FALSE.equals(f.getSoloCobranzaHoy());

        List<ChequePendienteFilaDto> todas = soloHoy
                ? verificaciones.sinRegularizarDelDia(hoy(), estado)
                : verificaciones.sinRegularizarHastaHoy(estado);

        VerificacionPaginaDto<ChequePendienteFilaDto> pagina = paginar(new ArrayList<>(todas),
                f == null ? null : f.getPagina(), f == null ? null : f.getTamanio(), ChequePendienteFilaDto::setFila);
        completar(pagina.getFilas());
        return pagina;
    }

    /**
     * "Seleccionar" un cheque pendiente ({@code registroSeleccionado}): lo relee y dice si todavia se puede verificar. La
     * fecha que se propone es hoy.
     */
    public PrepararVerificacionDto preparar(Long codCheque) {
        if (codCheque == null || codCheque <= 0) throw new SpBusinessException(MensajesVerificacion.FALTA_CHEQUE_A_PREPARAR);

        List<String> errores = new ArrayList<>();
        ChequePendienteFilaDto c = candidato(codCheque, errores);
        if (c == null) throw new SpBusinessException(String.join("\n", errores));
        // El JSF no dibujaba "Seleccionar" en un cheque CERRADO: el servidor tampoco deja prepararlo.
        if (ReglasVerificacion.esCerrado(c.getDatoEstadoCheque())) {
            throw new SpBusinessException(MensajesVerificacion.chequeCerrado(c.getNroCheque(), c.getCodCheque()));
        }
        completar(Collections.singletonList(c));
        c.setFila(1);
        return new PrepararVerificacionDto(c, hoy());
    }

    // ===================================================================== //
    //                           ESCRITURAS                                  //
    // ===================================================================== //

    /**
     * {@code saveVerificacion}: alta ({@code codvd} 0 o ausente) o edicion de una verificacion.
     *
     * @return el codvd
     */
    @Transactional
    public long registrar(Authentication auth, VerificacionRegistroDto dto) {
        if (dto == null) throw new SpBusinessException(MensajesVerificacion.SIN_DATOS);
        int usuario = DatosToken.codUsuarioDe(auth);
        boolean alta = dto.esAlta();

        // 1. Campos: en el JSF corren ANTES de la accion y la cortan (fecha requerida, validador de la observacion).
        List<String> campos = new ArrayList<>();
        if (dto.getFechaBanco() == null) campos.add(MensajesVerificacion.FALTA_FECHA);
        if (dto.getCodBanco() == null || dto.getCodBanco() <= 0) campos.add(MensajesVerificacion.FALTA_BANCO);
        if (alta && (dto.getCodCheque() == null || dto.getCodCheque() <= 0)) campos.add(MensajesVerificacion.FALTA_CHEQUE);
        String errorObs = ReglasVerificacion.errorObservacion(dto.getObservacion());
        if (errorObs != null) campos.add(errorObs);
        if (!campos.isEmpty()) throw new SpBusinessException(String.join("\n", campos));

        // 2. Negocio: se acumulan todos los mensajes, como en el resto del modulo.
        List<String> negocio = new ArrayList<>();
        if (!bancoExiste(dto.getCodBanco())) negocio.add(MensajesVerificacion.bancoNoEncontrado(dto.getCodBanco()));

        VerificacionDepositoFilaDto actual = null;
        if (alta) {
            ChequePendienteFilaDto c = candidato(dto.getCodCheque(), negocio);
            if (c != null && ReglasVerificacion.esCerrado(c.getDatoEstadoCheque())) {
                negocio.add(MensajesVerificacion.chequeCerrado(c.getNroCheque(), c.getCodCheque()));
            }
        } else {
            actual = verificaciones.obtener(dto.getCodvd());
            if (actual == null) negocio.add(MensajesVerificacion.verificacionNoEncontrada(dto.getCodvd().longValue()));
        }
        if (!negocio.isEmpty()) throw new SpBusinessException(String.join("\n", negocio));

        // 3. Escritura. El legacy no guardaba NULL: sin observacion escribe vacio.
        ChVerificacionDeposito v = new ChVerificacionDeposito();
        v.setCodBanco(dto.getCodBanco());
        v.setFechaBanco(dto.getFechaBanco());
        v.setObservacion(observacionAGuardar(dto.getObservacion()));
        v.setAudUsuario(usuario);
        if (alta) {
            v.setCodCheque(dto.getCodCheque());
            v.setEstado(VALIDA);
            return verificaciones.registrar(v).getIdGenerado();
        }
        // La edicion conserva el cheque y el estado de la verificacion cargada.
        v.setCodvd(actual.getCodvd());
        v.setCodCheque(actual.getCodCheque());
        v.setEstado(actual.getEstado());
        verificaciones.actualizar(v);
        return actual.getCodvd();
    }

    /**
     * "Cancelar" ({@code deleteRegistro}): anula la verificacion (estado N) sin borrarla, con los demas datos como
     * estaban. Si ya estaba anulada no escribe nada.
     *
     * @return {@code true} si la anulo; {@code false} si ya estaba anulada
     */
    @Transactional
    public boolean anular(Authentication auth, Long codvd) {
        int usuario = DatosToken.codUsuarioDe(auth);
        if (codvd == null || codvd <= 0) throw new SpBusinessException(MensajesVerificacion.FALTA_VERIFICACION_A_ANULAR);

        VerificacionDepositoFilaDto actual = codvd > Integer.MAX_VALUE ? null : verificaciones.obtener(codvd.intValue());
        if (actual == null) throw new SpBusinessException(MensajesVerificacion.verificacionNoEncontrada(codvd));
        if (ANULADA.equals(actual.getEstado())) return false;

        ChVerificacionDeposito v = new ChVerificacionDeposito();
        v.setCodvd(actual.getCodvd());
        v.setCodCheque(actual.getCodCheque());
        v.setCodBanco(actual.getCodBanco());
        v.setFechaBanco(actual.getFechaBanco());
        v.setObservacion(actual.getObservacion());
        v.setEstado(ANULADA);
        v.setAudUsuario(usuario);
        verificaciones.actualizar(v);
        return true;
    }

    // ===================================================================== //
    //                              APOYO                                    //
    // ===================================================================== //

    private LocalDate hoy() {
        return LocalDate.now(reloj);
    }

    /** El estado del cheque que se pide: PEN o CER (sin distinguir mayusculas); vacio = todos. */
    private static String estadoDeChequeOpcional(String estado) {
        if (estado == null || estado.trim().isEmpty()) return null;
        String e = estado.trim().toUpperCase();
        if (!"PEN".equals(e) && !"CER".equals(e)) {
            throw new SpBusinessException(MensajesVerificacion.estadoDeChequeNoValido(estado.trim(), Arrays.asList("PEN", "CER")));
        }
        return e;
    }

    /**
     * El cheque tal como lo ofrece la lista de pendientes (lectura {@code C} con su codigo), o null y un mensaje en
     * {@code errores} que dice por que no esta: ya tiene una verificacion valida, o no existe. Es la misma consulta que
     * hacia el legacy al elegir "Seleccionar" ({@code cargarNuevoRegistroSolic}); la comprobacion de "valida" es
     * {@code verifikChkValid}.
     */
    private ChequePendienteFilaDto candidato(Long codCheque, List<String> errores) {
        ChequePendienteFilaDto c = verificaciones.sinRegularizarDeUnCheque(codCheque);
        if (c != null) return c;
        errores.add(verificaciones.tieneVerificacionValida(codCheque)
                ? MensajesVerificacion.chequeYaVerificado(codCheque)
                : MensajesVerificacion.chequeNoEncontrado(codCheque));
        return null;
    }

    private boolean bancoExiste(Long codBanco) {
        return codBanco != null && codBanco > 0 && codBanco <= Integer.MAX_VALUE && bancos.obtener(codBanco.intValue()) != null;
    }

    /** Sin observacion (null o solo espacios) se escribe vacio; lo demas, tal como se escribio. */
    private static String observacionAGuardar(String observacion) {
        return observacion == null || observacion.trim().isEmpty() ? "" : observacion;
    }

    /**
     * Corta una pagina: {@code tamanio} 20 por defecto y 200 como maximo, {@code pagina} desde 1 acotada a la ultima. Numera
     * las filas de la pagina con su numero global (1..total).
     */
    static <T> VerificacionPaginaDto<T> paginar(List<T> todas, Integer pagina, Integer tamanio, BiConsumer<T, Integer> numerar) {
        int total = todas.size();
        int tam = tamanio == null || tamanio <= 0 ? TAMANIO_PAGINA : Math.min(tamanio, TAMANIO_PAGINA_MAXIMO);
        int paginas = Math.max(1, (total + tam - 1) / tam);
        int pag = pagina == null || pagina < 1 ? 1 : Math.min(pagina, paginas);

        List<T> filas = new ArrayList<>();
        if (total > 0) {
            int desde = (pag - 1) * tam;
            filas.addAll(todas.subList(desde, Math.min(total, desde + tam)));
            for (int i = 0; i < filas.size(); i++) numerar.accept(filas.get(i), desde + i + 1);
        }
        return new VerificacionPaginaDto<>(total, pag, tam, filas);
    }

    /**
     * Completa lo que los procedimientos no devuelven: la marca de cheque cerrado (la misma comparacion del JSF) y la
     * moneda, leyendo el cheque una sola vez aunque salga en varias filas. Si la lectura falla o no lo encuentra, la
     * moneda queda en null: nunca rompe el listado.
     */
    private void completar(List<? extends FilaConChequeDto> filas) {
        Map<Long, ChequeFilaDto> leidos = new HashMap<>();
        for (FilaConChequeDto f : filas) {
            f.setChequeCerrado(ReglasVerificacion.esCerrado(f.getDatoEstadoCheque()));

            Long cod = f.getCodCheque();
            if (cod == null || cod <= 0 || cod > Integer.MAX_VALUE) continue;
            ChequeFilaDto c;
            if (leidos.containsKey(cod)) {
                c = leidos.get(cod);
            } else {
                try {
                    c = cheques.obtener(cod.intValue());
                } catch (RuntimeException e) {
                    log.warn("No se pudo leer la moneda del cheque {}: {}", cod, e.getMessage());
                    c = null;
                }
                leidos.put(cod, c);
            }
            if (c != null) {
                f.setMoneda(c.getMoneda());
                f.setDescMoneda(c.getDescMoneda());
            }
        }
    }
}
