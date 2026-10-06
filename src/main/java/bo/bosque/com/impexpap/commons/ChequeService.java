package bo.bosque.com.impexpap.commons;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import bo.bosque.com.impexpap.config.SinPermisoException;
import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.dao.IChAccion;
import bo.bosque.com.impexpap.dao.IChCheque;
import bo.bosque.com.impexpap.dao.IPersonalCheque;
import bo.bosque.com.impexpap.dao.ITalonario;
import bo.bosque.com.impexpap.dao.ISucursalCheque;
import bo.bosque.com.impexpap.dto.AccionChequeDto;
import bo.bosque.com.impexpap.dto.AccionChequeRequest;
import bo.bosque.com.impexpap.dto.BotonesChequeDto;
import bo.bosque.com.impexpap.dto.ChequeDetalleDto;
import bo.bosque.com.impexpap.dto.ChequeFilaDto;
import bo.bosque.com.impexpap.dto.ChequeFiltroDto;
import bo.bosque.com.impexpap.dto.ChequePaginaDto;
import bo.bosque.com.impexpap.dto.ChequeRegistroDto;
import bo.bosque.com.impexpap.dto.ChequeResumenDto;
import bo.bosque.com.impexpap.dto.CustodiaChequeRequest;
import bo.bosque.com.impexpap.dto.DarCustodiaRequest;
import bo.bosque.com.impexpap.dto.EmpresaChequeDto;
import bo.bosque.com.impexpap.dto.HoraAccionDto;
import bo.bosque.com.impexpap.dto.PersonalChequeDto;
import bo.bosque.com.impexpap.dto.SucursalChequeDto;
import bo.bosque.com.impexpap.dto.TalonarioValidacionDto;
import bo.bosque.com.impexpap.dto.TalonarioValidacionRequest;
import bo.bosque.com.impexpap.model.ChAccion;
import bo.bosque.com.impexpap.model.ChCheque;
import bo.bosque.com.impexpap.security.jwt.DatosToken;
import bo.bosque.com.impexpap.utils.Tipos;

/**
 * Las operaciones del modulo de Cheques (tablas {@code tch_}), con las reglas que el legacy aplicaba
 * en {@code WizardCheque}.
 *
 * <h2>Decision 2 de CLAUDE.md: paridad funcional</h2>
 * Las reglas y la logica de negocio se mantienen <b>tal como el legacy las aplica hoy</b>. Lo unico que
 * se corrige son detalles tecnicos que no cambian la regla ni el resultado para quien usa la pantalla:
 * <ul>
 *   <li>(a) la rama {@code K} de {@code p_list_Cheque} se evalua <b>tambien aqui</b>. En el legacy solo
 *       deshabilitaba botones: el cliente no es una frontera de confianza.</li>
 *   <li>(b) el id del alta sale de {@code SCOPE_IDENTITY()} (en el {@code ALTER} de {@code p_abm_Cheque}).</li>
 *   <li>(c) las operaciones de varias escrituras van en <b>una sola transaccion</b>
 *       ({@code saveFechacobro}: cheque + accion; cerrar: accion + estado del cheque; "A Custodio":
 *       una accion por cheque).</li>
 *   <li>(d) la paginacion de la grilla se corta aqui.</li>
 *   <li>(e) el usuario de auditoria sale del token.</li>
 * </ul>
 *
 * <h2>Lo que el servidor impone y el JSF solo insinuaba</h2>
 * El JSF deshabilitaba campos y botones; el servidor no puede confiar en eso. Cada metodo repite el
 * permiso por boton ({@code tb_vistaBtn} de la vista 42), el permiso por sucursal y los campos que
 * cada formulario bloqueaba. Ver {@link ChequeRegistroDto#modo}.
 *
 * <h2>Reglas heredadas que se conservan a proposito</h2>
 * Eliminar una accion con el cheque cerrado, eliminar acciones REC/TRASP/CUS, "Dar Custodia" sin
 * filtrar, el administrador sin restriccion de estado. Estan listadas en CLAUDE.md ("Reglas heredadas
 * que se mantienen") y no se "mejoran" aqui.
 *
 * <h2>Quien escribe en una sucursal</h2>
 * {@link ISucursalCheque#puedeEscribir} llama a {@code p_list_Sucursal} 'D', que pretende consultar una
 * tabla fija <i>dentro del procedimiento</i> (usuarios nombrados) pero <b>hoy autoriza a cualquiera</b>:
 * tiene un {@code --union all} comentado y devuelve dos result sets, y el legacy lee la ultima fila del
 * primero (13). Se conserva asi (decision 2) y se sigue llamando al SP: si se corrige, la regla rige sola
 * en los dos sistemas. Ver "Hallazgos de datos" en CLAUDE.md.
 */
@Service
public class ChequeService {

    /** {@code tb_vista.codVista} de "Cheques" ({@code tchCheque/cheque}, padre 41 {@code modCheques}). */
    public static final int VISTA_CHEQUES = 42;

    // Nombres de tb_vistaBtn.nombreBtn de la vista 42, tal como existen hoy.
    static final String BTN_NUEVO = "btnNuevoCH";               // Registrar (formulario estandar)
    static final String BTN_NUEVO_ADMIN = "btnNuevo2CH";        // Registre (formulario del administrador)
    static final String BTN_EDITAR_1 = "btnEditar1CH";          // Editar / Editar talonario
    static final String BTN_EDITAR_2 = "btnEditar2CH";          // Fecha Cobro (en la fila)
    static final String BTN_EDITAR_3 = "btnEditar3CH";          // Edite / Fecha Cobr (variantes del administrador)
    static final String BTN_DETALLE = "btnDetalleCH";           // Completar: entra al panel de acciones
    static final String BTN_ELIMINAR_ACCION = "btnEliminarSegCH";
    static final String BTN_TRASPASO = "btnTraspasoCH";
    static final String BTN_CUSTODIA = "btnCustodiaCH";         // A Custodio
    static final String BTN_CUSTODIA_ADMIN = "btnCustodia2CH";  // Dar Custodia
    static final String BTN_SUCURSALES = "btnChqSucrs";         // habilita el combo de sucursales

    /** Filas por pagina que mostraba el legacy. */
    static final int TAMANIO_PAGINA = 20;
    static final int TAMANIO_PAGINA_MAXIMO = 200;

    /** Observacion que {@code saveCustodios} escribia, con su espacio inicial y su redaccion. */
    static final String OBSERVACION_CUSTODIA = " Entregado en Para Cobranza";

    static final String MSG_SUCURSAL = MensajesCheque.SUCURSAL_AJENA;

    private final IChCheque cheques;
    private final IChAccion acciones;
    private final ISucursalCheque sucursales;
    private final IPersonalCheque personal;
    private final ITalonario talonarios;
    private final AccesoModuloHelper acceso;
    private final Clock reloj;

    @Autowired
    public ChequeService(IChCheque cheques, IChAccion acciones, ISucursalCheque sucursales,
                         IPersonalCheque personal, ITalonario talonarios, AccesoModuloHelper acceso) {
        this(cheques, acciones, sucursales, personal, talonarios, acceso, Clock.systemDefaultZone());
    }

    /** Para las pruebas: un reloj fijo. */
    ChequeService(IChCheque cheques, IChAccion acciones, ISucursalCheque sucursales,
                  IPersonalCheque personal, ITalonario talonarios, AccesoModuloHelper acceso, Clock reloj) {
        this.cheques = cheques;
        this.acciones = acciones;
        this.sucursales = sucursales;
        this.personal = personal;
        this.talonarios = talonarios;
        this.acceso = acceso;
        this.reloj = reloj;
    }

    // ===================================================================== //
    //                              LECTURAS                                 //
    // ===================================================================== //

    /**
     * Las empresas del combo "Empresa" de la pantalla ({@code p_list_Empresa} 'C'). <b>No es la empresa del
     * usuario</b>: el legacy la elige en la pantalla ({@code wizardCheque.codEmpresa}) y de ella salen las
     * sucursales, los clientes y la empresa del cheque que se registra.
     */
    public List<EmpresaChequeDto> empresas() {
        return sucursales.empresasDeCheques();
    }

    /**
     * La empresa con la que se trabaja: la pedida o, si no se pidio ninguna, la primera del combo (el legacy
     * arranca en {@code codEmpresa = 1}; con los datos de hoy la primera es la 1). Nunca la del token: el login
     * resuelve la empresa del cargo del empleado, que puede ser una sin clientes ni cheques (p. ej. GENERAL).
     */
    public int empresaEfectiva(Integer pedida) {
        if (pedida != null && pedida > 0) return pedida;
        List<EmpresaChequeDto> lista = empresas();
        if (lista == null || lista.isEmpty()) {
            throw new SpBusinessException(MensajesCheque.SIN_EMPRESAS);
        }
        return lista.get(0).getCodEmpresa();
    }

    /**
     * La sucursal con la que el usuario abre la pantalla en esa empresa ({@code p_list_Sucursal} 'E', como
     * {@code SucursalDao.sucPorUsr}); 0 si no tiene.
     */
    public long sucursalInicial(Authentication auth, Integer codEmpresa) {
        DatosToken t = DatosToken.de(auth);
        return sucursales.sucursalInicial(empresaEfectiva(codEmpresa), t.getCodUsuario(), 0L);
    }

    /**
     * Las sucursales del combo para una empresa ({@code p_list_Sucursal} 'C', como
     * {@code SucursalDao.listadoSucursalCheqs}): las parametrizadas y permitidas a este usuario.
     */
    public List<SucursalChequeDto> sucursales(Authentication auth, Integer codEmpresa) {
        return sucursales.sucursalesDeEmpresa(empresaEfectiva(codEmpresa), DatosToken.codUsuarioDe(auth));
    }

    /**
     * Las filas cuya fecha de recepcion cae en {@code [desde, hasta]}, por <b>dia</b> y con los dos extremos
     * incluidos; un extremo en null no limita. Conserva el orden del SP. Sin ningun extremo devuelve la lista tal cual.
     * Una fila sin fecha de recepcion (no debe haber: la consulta hace INNER JOIN con la accion REC) queda fuera
     * en cuanto hay un extremo.
     */
    static List<ChequeFilaDto> enRangoDeRecepcion(List<ChequeFilaDto> filas, Date desde, Date hasta) {
        if (desde == null && hasta == null) return filas;
        LocalDate piso = desde == null ? null : ReglasCheque.dia(desde);
        LocalDate techo = hasta == null ? null : ReglasCheque.dia(hasta);
        List<ChequeFilaDto> salida = new ArrayList<>();
        for (ChequeFilaDto c : filas) {
            if (c.getFechaRecepcion() == null) continue;
            LocalDate d = ReglasCheque.dia(c.getFechaRecepcion());
            if (piso != null && d.isBefore(piso)) continue;
            if (techo != null && d.isAfter(techo)) continue;
            salida.add(c);
        }
        return salida;
    }

    /** La grilla, una pagina. */
    public ChequePaginaDto listar(Authentication auth, ChequeFiltroDto f) {
        if (f == null || f.getCodSucursal() == null || f.getCodSucursal() <= 0) {
            throw new SpBusinessException(MensajesCheque.FALTA_SUCURSAL);
        }
        exigirSucursalLectura(auth, f.getCodSucursal());

        if (f.getFechaRecepcionDesde() != null && f.getFechaRecepcionHasta() != null
                && ReglasCheque.dia(f.getFechaRecepcionDesde()).isAfter(ReglasCheque.dia(f.getFechaRecepcionHasta()))) {
            throw new SpBusinessException(MensajesCheque.RANGO_DE_RECEPCION_INVERTIDO);
        }

        List<ChequeFilaDto> todas = enRangoDeRecepcion(cheques.listar(f), f.getFechaRecepcionDesde(), f.getFechaRecepcionHasta());
        int total = todas.size();
        int tamanio = f.getTamanio() == null || f.getTamanio() <= 0
                ? TAMANIO_PAGINA : Math.min(f.getTamanio(), TAMANIO_PAGINA_MAXIMO);
        int paginas = Math.max(1, (total + tamanio - 1) / tamanio);
        int pagina = f.getPagina() == null || f.getPagina() < 1 ? 1 : Math.min(f.getPagina(), paginas);

        List<ChequeFilaDto> filas = new ArrayList<>();
        if (total > 0) {
            int desde = (pagina - 1) * tamanio;
            filas.addAll(todas.subList(desde, Math.min(total, desde + tamanio)));
        }
        return new ChequePaginaDto(total, pagina, tamanio, filas);
    }

    /** "Completar": el cheque, su historial y las acciones habilitadas. Boton {@code btnDetalleCH}. */
    public ChequeDetalleDto detalle(Authentication auth, int codCheque) {
        exigirPermiso(auth, BTN_DETALLE);
        ChequeFilaDto c = chequeExistente(codCheque);
        exigirSucursalLectura(auth, c.getCodSucursal());

        List<AccionChequeDto> historial = acciones.listarPorCheque(codCheque);
        BotonesChequeDto botones = BotonesChequeDto.desde(cheques.codigoBotones(codCheque));
        return new ChequeDetalleDto(c, historial, botones);
    }

    // ===================================================================== //
    //                           ALTA Y EDICION                              //
    // ===================================================================== //

    /**
     * Comprueba el par talonario/recibo <b>mientras el usuario escribe</b>, con la MISMA regla que aplica
     * {@link #registrar} al guardar (formato, y despues la rama {@code M} de {@code p_list_Cheque}): es un aviso
     * temprano para no esperar al boton, no una regla nueva. El servidor vuelve a validar al guardar.
     *
     * <p>Si el talonario o el recibo estan vacios o son {@code "0"} no hay nada que comprobar contra la base (como
     * {@link ReglasCheque#requiereValidarTalonario}) y responde valido. Es solo lectura: pide poder registrar o editar
     * cheques, que es quien ve esos campos.
     */
    public TalonarioValidacionDto validarTalonario(Authentication auth, TalonarioValidacionRequest r) {
        boolean puede = acceso.tieneBoton(auth, VISTA_CHEQUES, BTN_NUEVO) || acceso.tieneBoton(auth, VISTA_CHEQUES, BTN_NUEVO_ADMIN)
                || acceso.tieneBoton(auth, VISTA_CHEQUES, BTN_EDITAR_1) || acceso.tieneBoton(auth, VISTA_CHEQUES, BTN_EDITAR_3);
        if (!puede) throw new SinPermisoException(MensajesCheque.SIN_PERMISO_COMPROBAR_TALONARIO);
        if (r == null) throw new SpBusinessException(MensajesCheque.SIN_DATOS);

        if (r.getCodEmpresa() == null || r.getCodEmpresa() <= 0) throw new SpBusinessException(MensajesCheque.FALTA_EMPRESA);
        boolean empresaValida = false;
        for (EmpresaChequeDto e : empresas()) if (e.getCodEmpresa() == r.getCodEmpresa()) empresaValida = true;
        if (!empresaValida) throw new SpBusinessException(MensajesCheque.empresaNoHabilitada(nombresDeEmpresas()));
        int empresa = r.getCodEmpresa();

        String talonario = r.getNroTalonario() == null ? "" : r.getNroTalonario().trim();
        String recibo = r.getReciboManual() == null ? "" : r.getReciboManual().trim();
        if (talonario.isEmpty() || recibo.isEmpty() || !ReglasCheque.requiereValidarTalonario(recibo, talonario)) {
            return new TalonarioValidacionDto(true, null, null);
        }

        List<String> formato = new ArrayList<>();
        agregarSiHay(formato, ReglasCheque.errorNroTalonario(talonario));
        agregarSiHay(formato, ReglasCheque.errorReciboManual(recibo));
        if (!formato.isEmpty()) return new TalonarioValidacionDto(false, String.join("\n", formato), null);

        if (cheques.talonarioYReciboValidos(talonario, recibo, empresa)) {
            String detalle = null;
            try {   // el detalle es un extra: si la consulta falla, el par igual es valido
                detalle = MensajesCheque.detalleDelTalonario(talonario, empresa, talonarios.buscarPorNroTalonario(talonario),
                        nombreDeEmpresas());
            } catch (RuntimeException e) {
                // sin detalle
            }
            return new TalonarioValidacionDto(true, null, detalle);
        }
        return new TalonarioValidacionDto(false, explicarTalonario(talonario, recibo, empresa), null);
    }

    /**
     * {@code saveCheque} / {@code saveChequeTal}: alta (codCheque 0/null) o edicion de un cheque.
     *
     * @return el codCheque
     */
    @Transactional
    public long registrar(Authentication auth, ChequeRegistroDto dto) {
        if (dto == null) throw new SpBusinessException(MensajesCheque.SIN_DATOS);
        int usuario = DatosToken.codUsuarioDe(auth);
        boolean admin = acceso.esAdmin(auth);
        String modo = dto.modoEfectivo();
        boolean alta = dto.esAlta();

        ChequeFilaDto actual = null;
        if (!alta) actual = chequeExistente(dto.getCodCheque());

        // 1. Permiso por boton y por sucursal (WizardCheque.esAutorizado*, obtenerPermisoSucursal)
        exigirPermisoDeFormulario(auth, admin, modo, alta, actual);
        Long sucursal = alta ? dto.getCodSucursal() : actual.getCodSucursal();
        exigirSucursalEscritura(auth, sucursal);
        if (alta) exigirEmpresaYSucursalDelCombo(usuario, dto.getCodEmpresa(), sucursal);

        // 2. Lo que cada formulario deja cambiar: el resto se toma de la base, como hacia el JSF al
        //    deshabilitar el campo (un campo deshabilitado no se aplica al bean).
        ChCheque c = alta ? nuevoDesde(dto) : baseDe(actual, dto, modo);
        c.setCodSucursal(sucursal);
        c.setAudUsuario(usuario);
        // Una observacion ausente (null) en una edicion conserva la de la accion REC: p_abm_Cheque 'U'
        // la reescribe con lo que reciba. Una vacia ("") es el usuario que la borro.
        String observacion = dto.getObservacion();
        if (!alta && (ChequeRegistroDto.MODO_TALONARIO.equals(modo) || observacion == null)) {
            observacion = actual.getObservacion();
        }
        // El legacy escribe "" cuando no hay observacion (0 NULL en la accion REC de PRUEBA, salvo 3 filas).
        if (observacion == null) observacion = "";

        // 3. Validacion de campos: en el JSF corre ANTES de la accion y la corta.
        List<String> formato = erroresDeCampos(c, dto, modo, alta);
        if (!formato.isEmpty()) throw new SpBusinessException(String.join("\n", formato));

        // 4. Fecha de cobro dentro de +-28 dias de la del cheque (saveCheque; saveChequeTal no lo hace)
        if (!ChequeRegistroDto.MODO_TALONARIO.equals(modo)
                && !ReglasCheque.cobroEnRango(c.getFechaCobrar(), c.getFechaCheque())) {
            throw new SpBusinessException(MensajesCheque.cobroFueraDeRango(c.getFechaCobrar(), c.getFechaCheque()));
        }

        // 5. Reglas de negocio: se acumulan todos los mensajes, como en el legacy
        List<String> negocio = new ArrayList<>(ReglasCheque.erroresReciboTalonario(
                c.getCodEmpleado() == null ? 0 : c.getCodEmpleado(), c.getReciboManual(), c.getNroTalonario()));
        if (ReglasCheque.requiereValidarTalonario(c.getReciboManual(), c.getNroTalonario())
                && !cheques.talonarioYReciboValidos(c.getNroTalonario(), c.getReciboManual(),
                        c.getCodEmpresa() == null ? 0 : c.getCodEmpresa())) {
            negocio.add(explicarTalonario(c.getNroTalonario(), c.getReciboManual(), c.getCodEmpresa() == null ? 0 : c.getCodEmpresa()));
        }

        String nroOriginal = c.getNrocheque();
        c.setNrocheque(ReglasCheque.sinCerosIzquierda(c.getNrocheque()));
        if (c.getNrocheque() == null || c.getNrocheque().isEmpty()) {
            negocio.add(MensajesCheque.nroChequeSoloCeros(nroOriginal));
        } else if (alta && c.getCodBanco() != null && c.getCodSucursal() != null
                && cheques.existeDuplicado(c.getNrocheque(), c.getCodCliente(), c.getCodBanco(),
                        c.getCodSucursal(), c.getFechaCheque())) {
            negocio.add(MensajesCheque.chequeDuplicado(c.getNrocheque(), c.getCodCliente(), c.getFechaCheque()));
        }
        if (c.getCodSucursal() == null || c.getCodSucursal() <= 0) {
            negocio.add(MensajesCheque.FALTA_SUCURSAL);
        }
        if (!negocio.isEmpty()) throw new SpBusinessException(String.join("\n", negocio));

        // 6. Escritura
        if (alta) {
            return cheques.alta(c, observacion).getIdGenerado();
        }
        cheques.actualizar(c, observacion);
        return c.getCodCheque();
    }

    // ===================================================================== //
    //                    ACCIONES DEL DETALLE (rama K)                      //
    // ===================================================================== //

    /**
     * "Fecha Cobro" ({@code saveFechacobro}): nueva fecha de cobro y accion {@code VEN} / {@code ADE},
     * en una sola transaccion. El legacy hacia las dos escrituras seguidas y sin transaccion.
     */
    @Transactional
    public long fechaCobro(Authentication auth, AccionChequeRequest r) {
        if (r == null) throw new SpBusinessException(MensajesCheque.SIN_DATOS_ACCION);
        int usuario = DatosToken.codUsuarioDe(auth);
        boolean admin = acceso.esAdmin(auth);
        ChequeFilaDto ch = chequeExistente(r.getCodCheque());

        // El boton esta en la fila (btnEditar2CH, btnEditar3CH) y en el panel de acciones (btnDetalleCH).
        boolean sinRestriccion = admin || acceso.tieneBoton(auth, VISTA_CHEQUES, BTN_EDITAR_3);
        if (!sinRestriccion && !acceso.tieneBoton(auth, VISTA_CHEQUES, BTN_EDITAR_2)
                && !acceso.tieneBoton(auth, VISTA_CHEQUES, BTN_DETALLE)) {
            throw new SinPermisoException(MensajesCheque.sinPermiso(BTN_EDITAR_2));
        }
        exigirSucursalEscritura(auth, ch.getCodSucursal());

        List<String> errores = new ArrayList<>();
        exigirEstadoDe(r.getEstado(), new Tipos().lstAccionFechaCobroCheque(), errores);
        validarFechaDeAccion(r.getFecha(), errores);
        if (r.getNuevaFechaCobro() == null) {
            errores.add(MensajesCheque.FALTA_NUEVA_FECHA_COBRO);
        } else if (!sinRestriccion && !ReglasCheque.cobroEnRango(r.getNuevaFechaCobro(), ch.getFechaCheque())) {
            errores.add(MensajesCheque.nuevaFechaCobroFueraDeRango(r.getNuevaFechaCobro(), ch.getFechaCheque()));
        }
        agregarSiHay(errores, ReglasCheque.errorObservacion(r.getObservacion()));
        if (!errores.isEmpty()) throw new SpBusinessException(String.join("\n", errores));

        // (a) La rama K, evaluada aqui: boton 1 = Fecha Cobro. El administrador NO pasa por ella: sus botones
        // de fila (esAutorizado, siempre true para el admin) funcionan con el cheque CER y saveFechacobro no
        // mira el estado; solo exige un traspaso y devoluciones = entregas, que se comprueban abajo. (En
        // PRUEBA hay 403 cheques CER en ese caso.) Devolver y cerrar si siguen la K para todos: son botones
        // del panel, deshabilitados por la K tambien para el administrador.
        if (!admin) exigirBoton(ch, MensajesCheque.AccionDelDetalle.FECHA_COBRO, BotonesChequeDto::isFechaCobro);

        int traspasos = acciones.traspasosDelCheque(ch.getCodCheque());
        if (traspasos != 1) {   // el legacy exige EXACTAMENTE un traspaso
            throw new SpBusinessException(MensajesCheque.sinTraspaso(traspasos));
        }
        if (!acciones.devolucionesIgualAEntregas(ch.getCodCheque())) {
            throw new SpBusinessException(MensajesCheque.NO_ESTA_EN_LA_OFICINA);
        }

        cheques.cambiarFechaCobro(ch.getCodCheque(), r.getNuevaFechaCobro(), usuario);
        String obs = ReglasCheque.observacionFechaCobro(ReglasCheque.dia(r.getNuevaFechaCobro()), r.getObservacion());
        return acciones.registrar(accionManual(ch.getCodCheque(), r.getEstado(), r.getFecha(), null, obs, usuario))
                .getIdGenerado();
    }

    /** "Devolver": accion {@code DEV}. Habilitada por el boton 2 de la rama K. */
    @Transactional
    public long devolver(Authentication auth, AccionChequeRequest r) {
        if (r == null) throw new SpBusinessException(MensajesCheque.SIN_DATOS_ACCION);
        r.setEstado("DEV");
        return guardarAccion(auth, r, false, BotonesChequeDto::isDevolver);
    }

    /**
     * "Cerrar con verificacion" ({@code conVerificacion}: {@code COB}, boton 3) o "Cerrar sin
     * verificacion" ({@code CEF}, {@code CCH}, {@code PAP}, {@code DPR}, boton 4). Guarda la accion y
     * pasa el cheque a {@code CER}, en una sola transaccion.
     */
    @Transactional
    public long cerrar(Authentication auth, AccionChequeRequest r) {
        if (r == null) throw new SpBusinessException(MensajesCheque.SIN_DATOS_ACCION);
        boolean con = Boolean.TRUE.equals(r.getConVerificacion());
        return guardarAccion(auth, r, true, con ? BotonesChequeDto::isCerrarConVerificacion
                                                : BotonesChequeDto::isCerrarSinVerificacion);
    }

    /** {@code saveAcciones} para devolver y cerrar. */
    private long guardarAccion(Authentication auth, AccionChequeRequest r, boolean cierra,
                               java.util.function.Predicate<BotonesChequeDto> boton) {
        int usuario = DatosToken.codUsuarioDe(auth);
        ChequeFilaDto ch = chequeExistente(r.getCodCheque());
        exigirPermiso(auth, BTN_DETALLE);   // el panel de acciones es de btnDetalleCH
        exigirSucursalEscritura(auth, ch.getCodSucursal());

        List<String> errores = new ArrayList<>();
        if (cierra) {
            boolean con = Boolean.TRUE.equals(r.getConVerificacion());
            exigirEstadoDe(r.getEstado(), con ? new Tipos().lstAccionCierreConVerificacionCheque()
                                              : new Tipos().lstAccionCierreSinVerificacionCheque(), errores);
            if (r.getNroSap() == null || r.getNroSap().isEmpty()) {
                errores.add(MensajesCheque.FALTA_NRO_SAP);
            } else {
                agregarSiHay(errores, ReglasCheque.errorNroSap(r.getNroSap()));
            }
        }
        validarFechaDeAccion(r.getFecha(), errores);
        agregarSiHay(errores, ReglasCheque.errorObservacion(r.getObservacion()));
        if (ReglasCheque.esEstadoReservado(r.getEstado())) {
            errores.add(MensajesCheque.ESTADO_RESERVADO);
        }
        if (!errores.isEmpty()) throw new SpBusinessException(String.join("\n", errores));

        // (a) La rama K, evaluada aqui.
        MensajesCheque.AccionDelDetalle cual = !cierra ? MensajesCheque.AccionDelDetalle.DEVOLVER
                : Boolean.TRUE.equals(r.getConVerificacion()) ? MensajesCheque.AccionDelDetalle.CERRAR_CON_VERIFICACION
                : MensajesCheque.AccionDelDetalle.CERRAR_SIN_VERIFICACION;
        exigirBoton(ch, cual, boton);

        int cantidadDeAcciones = acciones.contarPorCheque(ch.getCodCheque());
        if (cantidadDeAcciones < ReglasCheque.ACCIONES_MINIMAS_PARA_CERRAR) {
            throw new SpBusinessException(MensajesCheque.pocasAcciones(cantidadDeAcciones, ReglasCheque.ACCIONES_MINIMAS_PARA_CERRAR));
        }

        long id = acciones.registrar(accionManual(ch.getCodCheque(), r.getEstado(), r.getFecha(),
                cierra ? r.getNroSap() : null, r.getObservacion(), usuario)).getIdGenerado();
        if (cierra) {
            cheques.cambiarEstado(ch.getCodCheque(), "CER", usuario);
        }
        return id;
    }

    /**
     * Elimina una accion ({@code eliminarAccion}). Boton {@code btnEliminarSegCH}.
     *
     * <p><b>Se conserva tal cual el legacy:</b> no mira si el cheque esta cerrado ({@code esAutorizadoB}
     * ignora ese parametro) ni excluye REC / TRASP / CUS. Borrar REC saca al cheque de la grilla (la
     * consulta hace INNER JOIN con esa accion) y borrar TRASP o CUS cambia lo que habilita la rama K.
     */
    @Transactional
    public void eliminarAccion(Authentication auth, int codAccion) {
        exigirPermiso(auth, BTN_ELIMINAR_ACCION);
        int usuario = DatosToken.codUsuarioDe(auth);
        ChAccion a = acciones.obtener(codAccion);
        if (a == null) throw new SpBusinessException(MensajesCheque.accionNoEncontrada(codAccion));

        ChequeFilaDto ch = cheques.obtener(a.getCodCheque());
        if (ch != null) exigirSucursalLectura(auth, ch.getCodSucursal());
        acciones.eliminar(codAccion, usuario);
    }

    // ===================================================================== //
    //                       TRASPASO Y CUSTODIA                             //
    // ===================================================================== //

    /** Cuantos cheques de la sucursal esperan el traspaso. Boton {@code btnTraspasoCH}. */
    public int pendientesDeTraspaso(Authentication auth, long codSucursal) {
        exigirPermiso(auth, BTN_TRASPASO);
        exigirSucursalLectura(auth, codSucursal);
        return acciones.pendientesDeTraspaso(codSucursal);
    }

    /**
     * Traspaso masivo ({@code entregarCheqsCaja}): {@code TRASP} en todos los cheques de la sucursal que
     * solo tienen una accion. La regla "una sola vez por dia" esta comentada en el SP: no se aplica.
     *
     * @return cuantos cheques se traspasaron
     */
    @Transactional
    public long traspasar(Authentication auth, long codSucursal) {
        exigirPermiso(auth, BTN_TRASPASO);
        int usuario = DatosToken.codUsuarioDe(auth);
        exigirSucursalEscritura(auth, codSucursal);

        long cantidad = acciones.traspasar(codSucursal, usuario).getIdGenerado();
        if (cantidad == 0) {
            // El legacy deshabilita "Generar" cuando no hay pendientes (totalCheqsPendts == 0), asi que este caso
            // no se alcanza desde su pantalla; si se llegara, daba un exito vacio (p_list_Accion corre con NOCOUNT
            // ON y executeUpdate() devuelve -1). Aqui es un error para no mostrar "se traspasaron 0".
            throw new SpBusinessException(MensajesCheque.SIN_PENDIENTES_DE_TRASPASO);
        }
        return cantidad;
    }

    /** Cheques de la sucursal listos para entregar en custodia hoy. Boton {@code btnCustodiaCH}. */
    public List<ChequeFilaDto> sinCustodio(Authentication auth, long codSucursal) {
        exigirPermiso(auth, BTN_CUSTODIA);
        exigirSucursalLectura(auth, codSucursal);
        return cheques.sinCustodio(codSucursal, fechaDeHoy());
    }

    /**
     * "A Custodio" ({@code saveCustodios}): una accion {@code CUS} por cada cheque elegido, con el
     * responsable. Todo o nada: el legacy las insertaba una a una sin transaccion y avisaba "ok" aunque
     * alguna fallara.
     *
     * @return cuantos cheques se entregaron
     */
    @Transactional
    public int custodia(Authentication auth, CustodiaChequeRequest r) {
        exigirPermiso(auth, BTN_CUSTODIA);
        int usuario = DatosToken.codUsuarioDe(auth);
        if (r == null || r.getCodSucursal() == null) throw new SpBusinessException(MensajesCheque.FALTA_SUCURSAL);
        exigirSucursalEscritura(auth, r.getCodSucursal());

        if (r.getCodEmpleado() == null || r.getCodEmpleado() <= 0) {
            throw new SpBusinessException(MensajesCheque.FALTA_RESPONSABLE);
        }
        if (r.getCodCheques() == null || r.getCodCheques().isEmpty()) {
            throw new SpBusinessException(MensajesCheque.FALTA_CHEQUE_PARA_CUSTODIA);
        }

        // En el legacy los cheques salen de un DataModel que el servidor carga para la sucursal de la pantalla
        // (los de hoy, p_list_Cheque 'C') y el cliente solo marca cuales. Aqui se exige lo mismo: cada id tiene
        // que estar en esa lista; sin repetidos. Un id ajeno o de un cheque cerrado cambiaria la rama K de otro.
        Set<Integer> disponibles = new HashSet<>();
        for (ChequeFilaDto f : cheques.sinCustodio(r.getCodSucursal(), fechaDeHoy())) disponibles.add(f.getCodCheque());
        Set<Integer> elegidos = new LinkedHashSet<>(r.getCodCheques());
        for (Integer codCheque : elegidos) {
            if (codCheque == null || !disponibles.contains(codCheque)) {
                throw new SpBusinessException(MensajesCheque.chequeNoDisponibleParaCustodia(codCheque == null ? 0 : codCheque));
            }
        }

        Date ahora = Date.from(reloj.instant());
        for (Integer codCheque : elegidos) {
            acciones.registrar(accionManual(codCheque, "CUS", ahora, null, OBSERVACION_CUSTODIA, usuario, r.getCodEmpleado()));
        }
        return elegidos.size();
    }

    /** "Dar Custodia", paso 1: todos los cheques de la sucursal. Boton {@code btnCustodia2CH}. */
    public List<ChequeResumenDto> chequesParaDarCustodia(Authentication auth, long codSucursal) {
        exigirPermiso(auth, BTN_CUSTODIA_ADMIN);
        exigirSucursalLectura(auth, codSucursal);
        return cheques.chequesDeSucursal(codSucursal);
    }

    /** "Dar Custodia", paso 2: las entregas a cobranza de un dia. Boton {@code btnCustodia2CH}. */
    public List<HoraAccionDto> entregasDelDia(Authentication auth, Long codSucursal, Date fecha) {
        exigirPermiso(auth, BTN_CUSTODIA_ADMIN);
        if (codSucursal == null || fecha == null) throw new SpBusinessException(MensajesCheque.FALTA_SUCURSAL_Y_FECHA);
        exigirSucursalLectura(auth, codSucursal);
        return cheques.custodiasDelDia(codSucursal, fecha);
    }

    /** Jefe de cobranzas, cobradores y choferes de la sucursal ("Entregado por"). */
    public List<PersonalChequeDto> quienesEntregan(Authentication auth, long codSucursal) {
        exigirSucursalLectura(auth, codSucursal);
        return personal.quienesEntregan(codSucursal);
    }

    /** Jefe de cobranzas y cobradores de la sucursal (responsable de custodia). */
    public List<PersonalChequeDto> responsablesDeCustodia(Authentication auth, long codSucursal) {
        exigirSucursalLectura(auth, codSucursal);
        return personal.responsablesDeCustodia(codSucursal);
    }

    /**
     * "Dar Custodia" ({@code saveCustMad}, administrador): copia una accion {@code CUS} a otro cheque.
     * La lista de cheques trae todos los de la sucursal (el SP tiene el filtro "sin custodia"
     * comentado) y asi se conserva.
     *
     * @return el codAccion nuevo
     */
    @Transactional
    public long darCustodia(Authentication auth, DarCustodiaRequest r) {
        exigirPermiso(auth, BTN_CUSTODIA_ADMIN);
        int usuario = DatosToken.codUsuarioDe(auth);
        if (r == null || r.getCodSucursal() == null) throw new SpBusinessException(MensajesCheque.FALTA_SUCURSAL);
        exigirSucursalEscritura(auth, r.getCodSucursal());

        if (r.getCodAccionOrigen() == null || r.getCodAccionOrigen() <= 0
                || r.getCodCheque() == null || r.getCodCheque() <= 0) {
            throw new SpBusinessException(MensajesCheque.FALTA_CHEQUE_Y_HORA);
        }
        ChAccion origen = acciones.obtener(r.getCodAccionOrigen());
        // El legacy solo ofrece entregas CUS (lista 'I') y los cheques de la sucursal (lista 'J'). Estos controles
        // no cambian lo que se puede hacer desde la pantalla: impiden copiar a mano otra accion (un cobro, por
        // ejemplo) o apuntar a un cheque de otra sucursal.
        if (origen == null || !"CUS".equals(origen.getEstado())) {
            throw new SpBusinessException(MensajesCheque.ENTREGA_NO_ES_CUSTODIA);
        }
        ChequeFilaDto cheDestino = cheques.obtener(r.getCodCheque());
        if (cheDestino == null || !r.getCodSucursal().equals(cheDestino.getCodSucursal())) {
            throw new SpBusinessException(MensajesCheque.CHEQUE_DE_OTRA_SUCURSAL);
        }
        ChequeFilaDto cheOrigen = cheques.obtener(origen.getCodCheque());
        if (cheOrigen == null || !r.getCodSucursal().equals(cheOrigen.getCodSucursal())) {
            throw new SpBusinessException(MensajesCheque.ENTREGA_DE_OTRA_SUCURSAL);
        }
        return acciones.copiar(r.getCodAccionOrigen(), r.getCodCheque(), usuario).getIdGenerado();
    }

    // ===================================================================== //
    //                              APOYO                                    //
    // ===================================================================== //

    private ChequeFilaDto chequeExistente(Integer codCheque) {
        ChequeFilaDto c = codCheque == null ? null : cheques.obtener(codCheque);
        if (c == null) throw new SpBusinessException(MensajesCheque.chequeNoEncontrado(codCheque));
        return c;
    }

    /** Quien edita lo ve el ACL y el estado, igual que {@code esAutorizado}, {@code esAutorizadoTalCer}. */
    private void exigirPermisoDeFormulario(Authentication auth, boolean admin, String modo, boolean alta,
                                           ChequeFilaDto actual) {
        if (alta) {
            if (ChequeRegistroDto.MODO_TALONARIO.equals(modo)) {
                throw new SpBusinessException(MensajesCheque.TALONARIO_EN_ALTA);
            }
            exigirPermiso(auth, ChequeRegistroDto.MODO_ADMIN.equals(modo) ? BTN_NUEVO_ADMIN : BTN_NUEVO);
            return;
        }
        boolean cerrado = "CER".equals(actual.getEstado());
        boolean permitido;
        String boton;
        switch (modo) {
            case ChequeRegistroDto.MODO_TALONARIO:
                boton = BTN_EDITAR_1;
                permitido = admin || (cerrado && acceso.tieneBoton(auth, VISTA_CHEQUES, boton));
                break;
            case ChequeRegistroDto.MODO_ADMIN:
                boton = BTN_EDITAR_3;
                permitido = admin || (!cerrado && acceso.tieneBoton(auth, VISTA_CHEQUES, boton));
                break;
            default:
                boton = BTN_EDITAR_1;
                permitido = admin || (!cerrado && acceso.tieneBoton(auth, VISTA_CHEQUES, boton));
        }
        if (!permitido) {
            // Sin el boton: se dice cual falta. Con el boton, el problema es el estado del cheque (abierto / cerrado).
            String motivo;
            if (!acceso.tieneBoton(auth, VISTA_CHEQUES, boton)) motivo = MensajesCheque.sinPermiso(boton);
            else if (ChequeRegistroDto.MODO_TALONARIO.equals(modo)) motivo = MensajesCheque.TALONARIO_SOLO_CERRADOS;
            else motivo = MensajesCheque.editarUnCerrado(BTN_EDITAR_1);
            throw new SinPermisoException(motivo);
        }
    }

    /** Cheque nuevo desde el formulario. Estado {@code PEN}; la fecha de cobro la fija el JSF a la del cheque. */
    private static ChCheque nuevoDesde(ChequeRegistroDto d) {
        ChCheque c = new ChCheque();
        c.setNrocheque(d.getNrocheque());
        c.setCodCliente(d.getCodCliente());
        c.setaOrdenDe(d.getaOrdenDe());
        c.setFechaCheque(d.getFechaCheque());
        // En el formulario estandar "Fecha Cobrar" esta deshabilitada y se llena con la del cheque.
        c.setFechaCobrar(ChequeRegistroDto.MODO_ADMIN.equals(d.modoEfectivo()) && d.getFechaCobrar() != null
                ? d.getFechaCobrar() : d.getFechaCheque());
        c.setMonto(d.getMonto());
        c.setMoneda(d.getMoneda());
        c.setTipo(d.getTipo());
        c.setEstado("PEN");
        c.setCodBanco(d.getCodBanco());
        c.setCodEmpleado(d.getCodEmpleado() == null ? 0 : d.getCodEmpleado());
        c.setReciboManual(d.getReciboManual());
        c.setNroTalonario(d.getNroTalonario());
        c.setCodEmpresa(d.getCodEmpresa());
        return c;
    }

    /** Un cheque existente: todo viene de la base y solo cambia lo que el formulario deja editar. */
    private static ChCheque baseDe(ChequeFilaDto a, ChequeRegistroDto d, String modo) {
        ChCheque c = new ChCheque();
        c.setCodCheque(a.getCodCheque());
        c.setNrocheque(a.getNrocheque());
        c.setCodCliente(a.getCodCliente());
        c.setaOrdenDe(a.getaOrdenDe());
        c.setFechaCheque(a.getFechaCheque());
        c.setFechaCobrar(a.getFechaCobrar());
        c.setMonto(a.getMonto());
        c.setMoneda(a.getMoneda());
        c.setTipo(a.getTipo());
        c.setEstado(a.getEstado());
        c.setCodBanco(a.getCodBanco());
        c.setCodEmpleado(a.getCodEmpleado());
        c.setReciboManual(a.getReciboManual());
        c.setNroRecibo(a.getNroRecibo());
        c.setNroTalonario(a.getNroTalonario());
        c.setCodEmpresa(a.getCodEmpresa());

        // chequeModalTal: solo talonario y recibo.
        c.setNroTalonario(d.getNroTalonario());
        c.setReciboManual(d.getReciboManual());
        if (ChequeRegistroDto.MODO_TALONARIO.equals(modo)) return c;

        // chequeModal: ademas cliente, banco y Nro de cheque (entregado por, tipo, monto, moneda,
        // a la orden y fecha del cheque quedan bloqueados en edicion).
        c.setCodCliente(d.getCodCliente());
        c.setCodBanco(d.getCodBanco());
        c.setNrocheque(d.getNrocheque());
        if (!ChequeRegistroDto.MODO_ADMIN.equals(modo)) return c;

        // chequeModalMad (administrador): nada bloqueado salvo empresa y sucursal.
        c.setCodEmpleado(d.getCodEmpleado() == null ? 0 : d.getCodEmpleado());
        c.setTipo(d.getTipo());
        c.setMonto(d.getMonto());
        c.setMoneda(d.getMoneda());
        c.setaOrdenDe(d.getaOrdenDe());
        c.setFechaCheque(d.getFechaCheque());
        c.setFechaCobrar(d.getFechaCobrar());
        return c;
    }

    /** Campos obligatorios y formatos que el JSF validaba antes de ejecutar la accion. */
    private static List<String> erroresDeCampos(ChCheque c, ChequeRegistroDto d, String modo, boolean alta) {
        List<String> e = new ArrayList<>();
        boolean talonario = ChequeRegistroDto.MODO_TALONARIO.equals(modo);
        boolean datosBase = alta || ChequeRegistroDto.MODO_ADMIN.equals(modo);   // los editables de verdad

        if (!talonario) {
            if (vacio(c.getNrocheque())) e.add(MensajesCheque.FALTA_NRO_CHEQUE);
            else agregarSiHay(e, ReglasCheque.errorNroCheque(c.getNrocheque()));
            if (vacio(c.getCodCliente())) e.add(MensajesCheque.FALTA_CLIENTE);
            if (c.getCodBanco() == null || c.getCodBanco() <= 0) e.add(MensajesCheque.FALTA_BANCO);
        }
        if (datosBase) {
            if (c.getMonto() == null) e.add(MensajesCheque.FALTA_MONTO);
            if (vacio(c.getaOrdenDe())) e.add(MensajesCheque.FALTA_A_LA_ORDEN);
            else agregarSiHay(e, ReglasCheque.errorAOrdenDe(c.getaOrdenDe()));
            if (c.getFechaCheque() == null) e.add(MensajesCheque.FALTA_FECHA_CHEQUE);
            if (c.getFechaCobrar() == null) e.add(MensajesCheque.FALTA_FECHA_COBRO);
            if (!enCatalogo(c.getTipo(), new Tipos().lstTipoCheque())) e.add(MensajesCheque.tipoNoValido(nombres(new Tipos().lstTipoCheque())));
            if (!enCatalogo(c.getMoneda(), new Tipos().lstMonedaCheque())) e.add(MensajesCheque.monedaNoValida(nombres(new Tipos().lstMonedaCheque())));
        }
        if (vacio(c.getNroTalonario())) e.add(MensajesCheque.FALTA_TALONARIO);
        else agregarSiHay(e, ReglasCheque.errorNroTalonario(c.getNroTalonario()));
        if (vacio(c.getReciboManual())) e.add(MensajesCheque.FALTA_RECIBO);
        else agregarSiHay(e, ReglasCheque.errorReciboManual(c.getReciboManual()));
        return e;
    }

    private static boolean enCatalogo(String codigo, List<Tipos> catalogo) {
        if (codigo == null) return false;
        for (Tipos t : catalogo) if (t.getCodTipos().equalsIgnoreCase(codigo.trim())) return true;
        return false;
    }

    private static boolean vacio(String s) {
        return s == null || s.isEmpty();
    }

    private static void agregarSiHay(List<String> errores, String error) {
        if (error != null) errores.add(error);
    }

    private void exigirEstadoDe(String estado, List<Tipos> permitidos, List<String> errores) {
        if (!enCatalogo(estado, permitidos)) {
            errores.add(MensajesCheque.estadoDeAccionNoValido(nombres(permitidos)));
        }
    }

    /** La fecha de la accion no puede ser anterior a hoy ({@code mindate = fechaHoy} del calendario). */
    private void validarFechaDeAccion(Date fecha, List<String> errores) {
        if (fecha != null && !ReglasCheque.noAnteriorA(fecha, hoy())) {
            errores.add(MensajesCheque.fechaDeAccionPasada(fecha, hoy()));
        }
    }

    /**
     * (a) La rama {@code K}: el boton correspondiente tiene que estar habilitado para el estado actual
     * del cheque. En el legacy solo lo hacia la pantalla, deshabilitando el boton.
     */
    private void exigirBoton(ChequeFilaDto ch, MensajesCheque.AccionDelDetalle accion,
                             java.util.function.Predicate<BotonesChequeDto> boton) {
        BotonesChequeDto k = BotonesChequeDto.desde(cheques.codigoBotones(ch.getCodCheque()));
        if (!boton.test(k)) {
            throw new SpBusinessException(MensajesCheque.accionNoHabilitada(accion, "CER".equals(ch.getEstado()), k));
        }
    }

    private List<String> nombresDeEmpresas() {
        List<String> l = new ArrayList<>();
        for (EmpresaChequeDto e : empresas()) l.add(e.getNombre());
        return l;
    }

    /** Los nombres (para decir que opciones son validas). */
    private static List<String> nombres(List<Tipos> tipos) {
        List<String> l = new ArrayList<>();
        for (Tipos x : tipos) l.add(x.getNombre());
        return l;
    }

    /**
     * El par talonario/recibo no paso la rama 'M': se consulta el modulo de Talonarios (solo lectura) para decir POR QUE.
     * Si esa consulta falla, el mensaje cae a uno generico: nunca se oculta el rechazo ni se rechaza por otro motivo.
     */
    private String explicarTalonario(String talonario, String recibo, int codEmpresa) {
        java.util.function.IntFunction<String> nombreEmpresa = nombreDeEmpresas();
        try {
            return MensajesCheque.talonarioNoCoincide(talonario, recibo, codEmpresa,
                    talonarios.buscarPorNroTalonario(talonario), nombreEmpresa);
        } catch (RuntimeException e) {
            return MensajesCheque.talonarioNoCoincideGenerico(talonario, recibo, codEmpresa, nombreEmpresa);
        }
    }

    /** codigo -> nombre de las empresas del combo (una sola consulta); null si no es una de ellas. */
    private java.util.function.IntFunction<String> nombreDeEmpresas() {
        java.util.Map<Integer, String> porCodigo = new java.util.HashMap<>();
        try {
            for (EmpresaChequeDto e : empresas()) porCodigo.put(e.getCodEmpresa(), e.getNombre());
        } catch (RuntimeException e) {
            // sin nombres: se muestra el codigo
        }
        return porCodigo::get;
    }

    /**
     * Una accion manual. El legacy escribe {@code codEmpleado = 0}, no NULL, cuando no hay empleado
     * (0 nulos y miles de ceros en la base); se conserva.
     */
    private ChAccion accionManual(Integer codCheque, String estado, Date fecha, String nroSap,
                                  String observacion, int usuario) {
        return accionManual(codCheque, estado, fecha, nroSap, observacion, usuario, 0);
    }

    private ChAccion accionManual(Integer codCheque, String estado, Date fecha, String nroSap,
                                  String observacion, int usuario, int codEmpleado) {
        ChAccion a = new ChAccion();
        a.setCodCheque(codCheque);
        a.setFecha(marcaDeTiempo(fecha));
        a.setEstado(estado);
        a.setCodEmpleado(codEmpleado);
        a.setNroSAP(nroSap);
        a.setObservacion(observacion == null ? "" : observacion);   // el legacy escribe "", no NULL
        a.setAudUsuario(usuario);
        return a;
    }

    /**
     * La fecha-hora de una accion. Sin fecha, ahora. Con la fecha de hoy, ahora (para que el historial
     * quede en orden: las acciones se listan por fecha y un "hoy" a medianoche iria antes que la
     * entrega de esta manana). Con otra fecha, esa fecha.
     */
    private Date marcaDeTiempo(Date fecha) {
        Date ahora = Date.from(reloj.instant());
        if (fecha == null) return ahora;
        return ReglasCheque.dia(fecha).equals(hoy()) ? ahora : fecha;
    }

    private LocalDate hoy() {
        return LocalDate.now(reloj);
    }

    private Date fechaDeHoy() {
        return java.sql.Date.valueOf(hoy());
    }

    // ---- sucursal

    /** Como {@code acceso.exigirBoton}, pero el 403 explica que permiso falta y que hacer ({@link SinPermisoException}). */
    private void exigirPermiso(Authentication auth, String boton) {
        MensajesCheque.exigirBoton(acceso, auth, VISTA_CHEQUES, boton);
    }

    /**
     * Quien puede <b>escribir</b> en una sucursal: el administrador o lo que responda el procedimiento
     * ({@code obtenerPermisoSucursal} + {@code determinaUsuario} del legacy). Hoy el procedimiento
     * responde que si a todos; ver la nota de la clase.
     */
    public void exigirSucursalEscritura(Authentication auth, Long codSucursal) {
        if (acceso.esAdmin(auth)) return;
        if (codSucursal == null) throw new SpBusinessException(MSG_SUCURSAL);
        // Sin btnChqSucrs el combo de sucursal del legacy esta DESHABILITADO y JSF ignora el valor de un campo
        // deshabilitado: solo se opera la sucursal propia. El procedimiento ('D') no lo restringe (autoriza a todos),
        // asi que se exige aqui, igual que para ver (auditoria de permisos, diferencias #2, #2b y #2c).
        if (!puedeVerSucursal(auth, codSucursal)) {
            throw new SinPermisoException(MensajesCheque.SOLO_SU_SUCURSAL_ESCRIBIR);
        }
        if (!sucursales.puedeEscribir(codSucursal, DatosToken.codUsuarioDe(auth))) {
            throw new SpBusinessException(MSG_SUCURSAL);
        }
    }

    /**
     * Quien puede <b>ver</b> una sucursal: la suya ({@code p_list_Sucursal} 'E'), cualquiera si tiene
     * {@code btnChqSucrs} (el combo del legacy esta deshabilitado sin ese boton) o el administrador.
     *
     * <p>"La suya" se calcula como en el legacy, <b>por empresa de la pantalla</b>: la sucursal inicial del
     * usuario en cada empresa del combo. Una peticion trae la sucursal y no la empresa; basta con que sea la
     * inicial de alguna de ellas.
     */
    public void exigirSucursalLectura(Authentication auth, Long codSucursal) {
        if (!puedeVerSucursal(auth, codSucursal)) {
            throw new SinPermisoException(MensajesCheque.SOLO_SU_SUCURSAL);
        }
    }

    /** Si el usuario puede trabajar la sucursal: administrador, {@code btnChqSucrs} o su sucursal inicial en alguna empresa. */
    private boolean puedeVerSucursal(Authentication auth, Long codSucursal) {
        if (acceso.esAdmin(auth) || acceso.tieneBoton(auth, VISTA_CHEQUES, BTN_SUCURSALES)) return true;
        int usuario = DatosToken.codUsuarioDe(auth);
        if (codSucursal != null && codSucursal > 0) {
            for (EmpresaChequeDto e : empresas()) {
                if (sucursales.sucursalInicial(e.getCodEmpresa(), usuario, 0L) == codSucursal) return true;
            }
        }
        return false;
    }

    /**
     * En un alta, la empresa y la sucursal tienen que ser las que el combo del legacy ofrece: la empresa una de
     * {@code p_list_Empresa} 'C' y la sucursal una de las de ESA empresa para este usuario
     * ({@code p_list_Sucursal} 'C'). El JSF lo garantizaba con los dos desplegables; el servidor no puede confiar
     * en lo que mande el cliente (antes la empresa salia del login y cualquier valor pasaba).
     */
    private void exigirEmpresaYSucursalDelCombo(int usuario, Integer codEmpresa, Long codSucursal) {
        if (codEmpresa == null || codEmpresa <= 0) {
            throw new SpBusinessException(MensajesCheque.FALTA_EMPRESA);
        }
        boolean empresaValida = false;
        for (EmpresaChequeDto e : empresas()) {
            if (e.getCodEmpresa() == codEmpresa) empresaValida = true;
        }
        if (!empresaValida) {
            throw new SpBusinessException(MensajesCheque.empresaNoHabilitada(nombresDeEmpresas()));
        }
        // Sin sucursal ya lo informa el paso 5 ("No se selecciono la Sucursal"), junto al resto de los mensajes.
        if (codSucursal == null || codSucursal <= 0) return;
        for (SucursalChequeDto s : sucursales.sucursalesDeEmpresa(codEmpresa, usuario)) {
            if (s.getCodSucursal() == codSucursal) return;
        }
        // El legacy tambien guardaba con la sucursal INICIAL del usuario ('E') aunque no figurara en su combo ('C'):
        // WizardCheque.java:636 usa this.codSucursal sin mirar el combo. Hay usuarios reales asi (E = 9, C = [1]) y
        // registraron cheques en su sucursal inicial (auditoria de permisos, diferencia #1).
        if (sucursales.sucursalInicial(codEmpresa, usuario, 0L) == codSucursal) return;
        String nombreEmpresa = nombreDeEmpresas().apply(codEmpresa);
        throw new SpBusinessException(MensajesCheque.sucursalNoEsDeLaEmpresa(nombreEmpresa == null ? "N.º " + codEmpresa : nombreEmpresa));
    }
}
