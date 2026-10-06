package bo.bosque.com.impexpap.commons;

import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.dao.IChChequeReporte;
import bo.bosque.com.impexpap.dao.IPersonalCheque;
import bo.bosque.com.impexpap.dao.ISucursalCheque;
import bo.bosque.com.impexpap.dto.ChequeCobranzaRptDto;
import bo.bosque.com.impexpap.dto.ChequeCustodioRptDto;
import bo.bosque.com.impexpap.dto.ChequeRecibidoRptDto;
import bo.bosque.com.impexpap.dto.ChequeTraspasoRptDto;
import bo.bosque.com.impexpap.dto.EmpresaChequeDto;
import bo.bosque.com.impexpap.dto.HoraTraspasoChequeDto;
import bo.bosque.com.impexpap.dto.NotaRemisionRptDto;
import bo.bosque.com.impexpap.dto.PersonalChequeDto;
import bo.bosque.com.impexpap.dto.ReciboChequeRptDto;
import bo.bosque.com.impexpap.dto.ReporteChequeRequest;
import bo.bosque.com.impexpap.dto.SucursalChequeDto;
import bo.bosque.com.impexpap.security.jwt.DatosToken;
import bo.bosque.com.impexpap.utils.Tipos;

/**
 * Los reportes en PDF del modulo de Cheques: los cinco botones {@code btnRpt1CH}..{@code btnRpt5CH} y la nomina
 * que sale al hacer el traspaso. Salen de {@code WizardCheque.cargarDatosReport01..05} y {@code cargarReporteTraspasosMad}.
 *
 * <p>Como en el legacy, cada reporte es una plantilla Jasper alimentada por una rama de {@code p_list_Cheque}; la
 * diferencia es de plomeria: aqui los datos los carga el DAO (por procedimiento almacenado) y la plantilla los recibe
 * como coleccion ({@link JasperReportExport#exportPDFDesdeColeccion}), el mismo patron que Garantias.
 *
 * <h2>Reglas</h2>
 * <ul>
 *   <li><b>Boton</b> ({@code tb_vistaBtn} de la vista 42): el administrador pasa; el servidor no confia en que la
 *       pantalla oculte el boton. La nomina del traspaso exige {@code btnTraspasoCH}.</li>
 *   <li><b>Empresa y sucursal</b>: las de la pantalla. La empresa debe ser una del combo ({@code p_list_Empresa} 'C')
 *       y el usuario debe poder <i>ver</i> la sucursal (la propia, {@code btnChqSucrs} o admin), como el resto de las
 *       lecturas. Las consultas filtran por sucursal y no por empresa, igual que la grilla.</li>
 *   <li><b>Logos</b>: por empresa, como {@code datosBasicosBosque} / {@code datosBasicosEPP}: la 1 usa los de Bosque y
 *       las demas los de EPP.</li>
 *   <li><b>Titulo y rotulos</b>: los mismos textos del legacy ("Desde ... Hasta el ...", " y son PENDIENTE (s)", ...).
 *       "Impreso por" es el nombre del usuario del token.</li>
 *   <li><b>Fechas</b> de rango: opcionales, como el legacy; sin ellas el procedimiento usa hoy (un rango sin fechas
 *       da un reporte sin filas, igual que hoy).</li>
 * </ul>
 *
 * <p>Una diferencia con el legacy, a proposito: el recibo del ultimo cheque sin ningun cheque del usuario da un
 * error claro (el legacy imprimia un recibo en blanco con {@code codigo = 0}).
 */
@Service
public class ChequeReporteService {

    // tb_vistaBtn.nombreBtn de la vista 42
    static final String BTN_RPT_RECIBIDOS = "btnRpt1CH";   // "Reporte"
    static final String BTN_RPT_COBRANZAS = "btnRpt2CH";   // "Reporte Cheques"
    static final String BTN_RPT_CUSTODIO = "btnRpt3CH";    // "Reporte Custodio"
    static final String BTN_RPT_RECIBO = "btnRpt4CH";      // "Recibo del Ultimo Cheque"
    static final String BTN_RPT_TRASPASO = "btnRpt5CH";    // "Imp Traspso" (reimpresion, administrador)
    static final String BTN_TRASPASO = "btnTraspasoCH";    // "Traspaso": su nomina

    /** Empresa que usa los logos de Bosque; las demas, los de EPP ({@code WizardCheque}: {@code codEmpresa == 1}). */
    static final int EMPRESA_BOSQUE = 1;

    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final IChChequeReporte datos;
    private final ChequeService cheques;
    private final ISucursalCheque sucursales;
    private final IPersonalCheque personal;
    private final AccesoModuloHelper acceso;
    private final JasperReportExport jasper;

    @Autowired
    public ChequeReporteService(IChChequeReporte datos, ChequeService cheques, ISucursalCheque sucursales,
                                IPersonalCheque personal, AccesoModuloHelper acceso, JasperReportExport jasper) {
        this.datos = datos;
        this.cheques = cheques;
        this.sucursales = sucursales;
        this.personal = personal;
        this.acceso = acceso;
        this.jasper = jasper;
    }

    // ===================================================================== //
    //                              REPORTES                                 //
    // ===================================================================== //

    /** btnRpt1CH "Reporte": cheques recibidos en caja entre dos fechas ({@code RptChequeCajaRecepcion}, 'R'). */
    public byte[] recibidos(Authentication auth, ReporteChequeRequest r) {
        Contexto c = contexto(auth, r, BTN_RPT_RECIBIDOS);
        String titulo = "";
        if (r.getFechaDesde() != null) titulo += " Desde " + dia(r.getFechaDesde());
        if (r.getFechaHasta() != null) titulo += " Hasta el " + dia(r.getFechaHasta());

        List<ChequeRecibidoRptDto> filas = datos.recibidos(c.codSucursal, dia(r.getFechaDesde()), dia(r.getFechaHasta()));

        Map<String, Object> p = c.parametros();
        p.put("codSucursal", String.valueOf(c.codSucursal));
        p.put("fecha", dia(r.getFechaDesde()));
        p.put("fechaFin", dia(r.getFechaHasta()));
        p.put("titulo", titulo);
        return jasper.exportPDFDesdeColeccion("RptChequeCajaRecepcion", filas, p);
    }

    /** btnRpt2CH "Reporte Cheques": cheques de cobranzas, por fechas, estado y cliente ({@code RptChequeCobranzas}, 'P'). */
    public byte[] cobranzas(Authentication auth, ReporteChequeRequest r) {
        Contexto c = contexto(auth, r, BTN_RPT_COBRANZAS);
        String estado = todos(r.getEstado());
        String cliente = todos(r.getCodCliente());
        if (estado != null && nombreEstado(estado) == null) {
            throw new SpBusinessException(MensajesCheque.estadoDeChequeNoValido(estado, nombresDeEstados()));
        }

        String titulo = "";
        if (r.getFechaDesde() != null) titulo += " Desde " + dia(r.getFechaDesde());
        if (r.getFechaHasta() != null) titulo += " Hasta el " + dia(r.getFechaHasta());
        if (estado != null) titulo += " y son " + nombreEstado(estado) + " (s)";
        if (cliente != null) titulo += " del Cliente con Código  " + cliente;

        List<ChequeCobranzaRptDto> filas = datos.cobranzas(c.codSucursal, dia(r.getFechaDesde()),
                dia(r.getFechaHasta()), estado, cliente);

        Map<String, Object> p = c.parametros();
        p.put("codSucursal", String.valueOf(c.codSucursal));
        p.put("fecha", dia(r.getFechaDesde()));
        p.put("fechaFin", dia(r.getFechaHasta()));
        p.put("estado", estado);
        p.put("codCliente", cliente);
        p.put("titulo", titulo);
        return jasper.exportPDFDesdeColeccion("RptChequeCobranzas", filas, p);
    }

    /**
     * btnRpt3CH "Reporte Custodio": cheques entregados a cobranza, de un dia y/o de un cobrador
     * ({@code RptChequeCajaSalida} + subreporte de notas de remision, 'S').
     */
    public byte[] custodio(Authentication auth, ReporteChequeRequest r) {
        Contexto c = contexto(auth, r, BTN_RPT_CUSTODIO);
        Integer empleado = r.getCodEmpleado() == null || r.getCodEmpleado() == 0 ? null : r.getCodEmpleado();

        String titulo = "";
        if (r.getFecha() != null) titulo += " En Fecha : " + dia(r.getFecha());
        if (empleado != null) titulo += "   entregados al Cobrador : " + nombreDelCobrador(c.codSucursal, empleado);

        List<ChequeCustodioRptDto> filas = datos.custodio(c.codSucursal, dia(r.getFecha()), empleado);
        // Las notas de remision de cada cheque (el legacy las pedia desde el subreporte, fila por fila).
        Map<Integer, List<NotaRemisionRptDto>> porCheque = new HashMap<>();
        for (ChequeCustodioRptDto f : filas) {
            Integer cod = f.getCodCheque();
            if (cod == null) {
                f.setFacturas(Collections.<NotaRemisionRptDto>emptyList());
                continue;
            }
            List<NotaRemisionRptDto> notas = porCheque.get(cod);
            if (notas == null) {
                notas = datos.notasDeRemision(cod);
                porCheque.put(cod, notas);
            }
            f.setFacturas(notas);
        }

        Map<String, Object> p = c.parametros();
        p.put("codSucursal", String.valueOf(c.codSucursal));
        p.put("fecha", dia(r.getFecha()));
        p.put("estado", empleado == null ? null : String.valueOf(empleado));
        p.put("titulo", titulo);
        return jasper.exportPDFDesdeColeccionConSubreportes("RptChequeCajaSalida", filas, p,
                "subRptChequeCajaSalidaFact");
    }

    /**
     * btnRpt4CH "Recibo del Ultimo Cheque": el recibo de caja del ultimo cheque que registro este usuario en la
     * sucursal ({@code RptCheqCajRec}, 'E' y 'D').
     */
    public byte[] reciboDelUltimoCheque(Authentication auth, ReporteChequeRequest r) {
        Contexto c = contexto(auth, r, BTN_RPT_RECIBO);
        Integer codCheque = datos.ultimoChequeDelUsuario(c.codUsuario, c.codSucursal);
        if (codCheque == null || codCheque <= 0) {
            throw new SpBusinessException(MensajesCheque.SIN_CHEQUE_PROPIO);
        }
        ReciboChequeRptDto recibo = datos.recibo(codCheque, c.codSucursal);
        if (recibo == null) {
            throw new SpBusinessException(MensajesCheque.reciboSinRecepcion(codCheque));
        }

        Map<String, Object> p = c.parametros();
        p.put("logoEmpresa2", c.logoEmpresa());
        p.put("codigo", codCheque);
        p.put("codSucursal", (int) c.codSucursal);
        return jasper.exportPDFDesdeColeccion("RptCheqCajRec", Collections.singletonList(recibo), p);
    }

    /** La nomina del ULTIMO traspaso de la sucursal ({@code RptChequeEntregaCaja}, 'T'): sale al traspasar. Exige btnTraspasoCH. */
    public byte[] nominaDelTraspaso(Authentication auth, ReporteChequeRequest r) {
        Contexto c = contexto(auth, r, BTN_TRASPASO);
        List<ChequeTraspasoRptDto> filas = datos.ultimoTraspaso(c.codSucursal);

        Map<String, Object> p = c.parametros();
        p.put("codSucursal", String.valueOf(c.codSucursal));
        return jasper.exportPDFDesdeColeccion("RptChequeEntregaCaja", filas, p);
    }

    /** btnRpt5CH "Imp Traspso": reimprime la nomina de un traspaso elegido ({@code RptChkTraspasoAdm}, 'H'). */
    public byte[] reimpresionDeTraspaso(Authentication auth, ReporteChequeRequest r) {
        Contexto c = contexto(auth, r, BTN_RPT_TRASPASO);
        if (r.getCodAccion() == null || r.getCodAccion() <= 0) {
            throw new SpBusinessException(MensajesCheque.ELEGIR_TRASPASO);
        }
        List<ChequeTraspasoRptDto> filas = datos.reimpresionTraspaso(c.codSucursal, r.getCodAccion());

        Map<String, Object> p = c.parametros();
        p.put("codSucursal", (int) c.codSucursal);
        p.put("codCliente", String.valueOf(r.getCodAccion()));   // el legacy lo manda en este parametro
        // La plantilla trae su propio titulo y no declara "titulo": el legacy tambien lo mandaba en vano.
        p.put("titulo", " Nómina de Cheques a Entregar ");
        return jasper.exportPDFDesdeColeccion("RptChkTraspasoAdm", filas, p);
    }

    /** btnRpt5CH: los traspasos de un dia (codigo de la accion y hora), para elegir cual reimprimir. */
    public List<HoraTraspasoChequeDto> horasDeTraspaso(Authentication auth, Long codSucursal, Date fecha) {
        MensajesCheque.exigirBoton(acceso, auth, ChequeService.VISTA_CHEQUES, BTN_RPT_TRASPASO);
        if (codSucursal == null || codSucursal <= 0 || fecha == null) {
            throw new SpBusinessException(MensajesCheque.FALTA_SUCURSAL_Y_FECHA);
        }
        cheques.exigirSucursalLectura(auth, codSucursal);
        return datos.horasDeTraspaso(codSucursal, fecha);
    }

    // ===================================================================== //
    //                              APOYO                                    //
    // ===================================================================== //

    /** Todo lo que cada reporte necesita saber de quien lo pide y de donde lo pide. */
    private final class Contexto {
        final int codUsuario;
        final String usuario;
        final int codEmpresa;
        final long codSucursal;
        final String nombreSucursal;

        Contexto(int codUsuario, String usuario, int codEmpresa, long codSucursal, String nombreSucursal) {
            this.codUsuario = codUsuario;
            this.usuario = usuario;
            this.codEmpresa = codEmpresa;
            this.codSucursal = codSucursal;
            this.nombreSucursal = nombreSucursal;
        }

        /** Los parametros comunes: logos de la empresa, quien imprime y la sucursal. Un stream nuevo por llamada: el llenado lo consume. */
        Map<String, Object> parametros() {
            Map<String, Object> p = new LinkedHashMap<>();
            boolean bosque = codEmpresa == EMPRESA_BOSQUE;
            p.put("logoEmpresa", logoEmpresa());
            p.put("logoSistema", recurso("/logos/logoBosque.jpg"));
            // EPP no tiene su logo de agua en el repositorio; las plantillas de cheques no lo dibujan.
            p.put("logoAgua", bosque ? recurso("/logos/logoEmpresaAgua.jpg") : null);
            p.put("usuario", usuario);
            p.put("sucursal", " ( " + nombreSucursal + " )");
            return p;
        }

        InputStream logoEmpresa() {
            return recurso(codEmpresa == EMPRESA_BOSQUE ? "/logos/logoEmpresa.jpg" : "/logos/logoEmpresaEPP.jpg");
        }
    }

    /**
     * Boton, empresa del combo y sucursal visible; todo lo que el JSF garantizaba con el ACL y con los dos
     * desplegables y que el servidor no puede dar por hecho.
     */
    private Contexto contexto(Authentication auth, ReporteChequeRequest r, String boton) {
        MensajesCheque.exigirBoton(acceso, auth, ChequeService.VISTA_CHEQUES, boton);
        if (r == null || r.getCodSucursal() == null || r.getCodSucursal() <= 0) {
            throw new SpBusinessException(MensajesCheque.FALTA_SUCURSAL);
        }
        if (r.getCodEmpresa() == null || r.getCodEmpresa() <= 0) {
            throw new SpBusinessException(MensajesCheque.FALTA_EMPRESA);
        }
        boolean empresaValida = false;
        for (EmpresaChequeDto e : cheques.empresas()) {
            if (e.getCodEmpresa() == r.getCodEmpresa()) empresaValida = true;
        }
        if (!empresaValida) throw new SpBusinessException(MensajesCheque.empresaNoHabilitada(nombresDeEmpresas()));
        cheques.exigirSucursalLectura(auth, r.getCodSucursal());

        DatosToken t = DatosToken.de(auth);
        return new Contexto(t.getCodUsuario(), t.getNombreCompleto(), r.getCodEmpresa(), r.getCodSucursal(),
                nombreDeSucursal(r.getCodEmpresa(), r.getCodSucursal(), t.getCodUsuario()));
    }

    /** El nombre del combo (como el legacy: {@code obtenerDatoSucursal2daForma}); si no esta, el codigo. */
    private String nombreDeSucursal(int codEmpresa, long codSucursal, int codUsuario) {
        for (SucursalChequeDto s : sucursales.sucursalesDeEmpresa(codEmpresa, codUsuario)) {
            if (s.getCodSucursal() == codSucursal) return s.getNombre() == null ? "" : s.getNombre().trim();
        }
        return String.valueOf(codSucursal);
    }

    private String nombreDelCobrador(long codSucursal, int codEmpleado) {
        List<PersonalChequeDto> cobradores = personal.responsablesDeCustodia(codSucursal);
        for (PersonalChequeDto p : cobradores == null ? new ArrayList<PersonalChequeDto>() : cobradores) {
            if (p.getCodEmpleado() == codEmpleado) return p.getNombreCompleto() == null ? "" : p.getNombreCompleto().trim();
        }
        return "";
    }

    /** El nombre del estado del cheque ({@code v_tipos} 23), o null si no es uno. */
    private static String nombreEstado(String codigo) {
        for (Tipos t : new Tipos().lstEstadoCheque()) {
            if (t.getCodTipos().equalsIgnoreCase(codigo)) return t.getNombre();
        }
        return null;
    }

    private List<String> nombresDeEmpresas() {
        List<String> l = new ArrayList<>();
        for (EmpresaChequeDto e : cheques.empresas()) l.add(e.getNombre());
        return l;
    }

    private static List<String> nombresDeEstados() {
        List<String> l = new ArrayList<>();
        for (Tipos t : new Tipos().lstEstadoCheque()) l.add(t.getNombre());
        return l;
    }

    /** "-" y vacio significan "todos" en los combos del legacy: no se manda el filtro. */
    private static String todos(String valor) {
        if (valor == null) return null;
        String v = valor.trim();
        return v.isEmpty() || "-".equals(v) ? null : v;
    }

    /** dd/MM/yyyy, que es lo que el procedimiento convierte con {@code CONVERT(datetime, x, 103)}; null si no hay fecha. */
    private static String dia(Date d) {
        return d == null ? null : ReglasCheque.dia(d).format(DIA);
    }

    private InputStream recurso(String ruta) {
        return getClass().getResourceAsStream(ruta);
    }
}
