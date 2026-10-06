package bo.bosque.com.impexpap.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import bo.bosque.com.impexpap.commons.ChequePdfService;
import bo.bosque.com.impexpap.commons.ChequeReporteService;
import bo.bosque.com.impexpap.commons.ChequeService;
import bo.bosque.com.impexpap.dao.ISocionegocio;
import bo.bosque.com.impexpap.dto.AccionChequeRequest;
import bo.bosque.com.impexpap.dto.CatalogosChequeDto;
import bo.bosque.com.impexpap.dto.ChequeFiltroDto;
import bo.bosque.com.impexpap.dto.ChequePdfRequest;
import bo.bosque.com.impexpap.dto.ChequePdfSubidoDto;
import bo.bosque.com.impexpap.dto.ChequeRegistroDto;
import bo.bosque.com.impexpap.dto.CustodiaChequeRequest;
import bo.bosque.com.impexpap.dto.DarCustodiaRequest;
import bo.bosque.com.impexpap.dto.FiltroIdDto;
import bo.bosque.com.impexpap.dto.FiltroSucursalFechaDto;
import bo.bosque.com.impexpap.dto.OpcionChequeDto;
import bo.bosque.com.impexpap.dto.ReporteChequeRequest;
import bo.bosque.com.impexpap.dto.TalonarioValidacionRequest;
import bo.bosque.com.impexpap.model.SocioNegocio;
import bo.bosque.com.impexpap.utils.ApiResponse;
import bo.bosque.com.impexpap.utils.Tipos;

/**
 * Modulo de Cheques recibidos (tablas {@code tch_}).
 *
 * <p>Reemplaza la pantalla {@code tchCheque/cheque.xhtml} de Bosque v2 (1428 lineas, 22 dialogos, bean
 * {@code WizardCheque}). El contrato, endpoint por endpoint, esta en {@code API_CHEQUES.md} del espacio de
 * migracion; las reglas y por que cada una se conserva, en la seccion "Reglas de habilitado, visibilidad y
 * validacion del legacy" de su {@code CLAUDE.md}.
 *
 * <p>Este controlador solo traduce HTTP: todas las reglas viven en {@link ChequeService}, que es lo que se
 * prueba. Todo es {@code POST} sobre {@code ApiResponse}, como el resto de la casa. El usuario de
 * auditoria sale siempre del token, nunca del cuerpo.
 *
 * <p>Los combos fijos salen de {@code /catalogos}; los bancos, de {@code /banco/bancosX}.
 */
@RestController
@CrossOrigin(origins = "*", methods = {RequestMethod.POST, RequestMethod.GET})
@RequestMapping("/cheque")
@PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
public class ChChequeController {

    private static final String SUCCESS_MESSAGE = "Operación realizada exitosamente";

    private final ChequeService servicio;
    private final ChequeReporteService reportes;
    private final ChequePdfService pdfs;
    private final ISocionegocio clientes;

    public ChChequeController(ChequeService servicio, ChequeReporteService reportes, ChequePdfService pdfs,
                              ISocionegocio clientes) {
        this.servicio = servicio;
        this.reportes = reportes;
        this.pdfs = pdfs;
        this.clientes = clientes;
    }

    /** La empresa que pidio el cliente ({@code id}); null si no pidio ninguna (el servicio usa la primera del combo). */
    private static Integer empresaPedida(FiltroIdDto filtro) {
        return filtro != null && filtro.getId() > 0 ? (int) filtro.getId() : null;
    }

    // ===================================================================== //
    //                              LECTURAS                                 //
    // ===================================================================== //

    /** Las empresas del combo "Empresa" (IMPEXPAP, ESPPAPEL): la pantalla trabaja con una a la vez. */
    @PostMapping("/empresas")
    public ResponseEntity<ApiResponse<?>> listarEmpresas() {
        return lista(servicio.empresas(), "No hay empresas habilitadas para cheques.");
    }

    /**
     * La sucursal con la que el usuario abre la pantalla en esa empresa ({@code id}; sin cuerpo o 0 = la primera
     * empresa del combo); 0 si no tiene ninguna.
     */
    @PostMapping("/sucursal-inicial")
    public ResponseEntity<ApiResponse<?>> sucursalInicial(@RequestBody(required = false) FiltroIdDto filtro,
                                                          Authentication auth) {
        return ok(servicio.sucursalInicial(auth, empresaPedida(filtro)));
    }

    /** Sucursales de una empresa para el combo ({@code id} = empresa; 0 = la primera del combo, no la del token). */
    @PostMapping("/sucursales")
    public ResponseEntity<ApiResponse<?>> listarSucursales(@RequestBody(required = false) FiltroIdDto filtro,
                                                           Authentication auth) {
        return lista(servicio.sucursales(auth, empresaPedida(filtro)), "No hay sucursales para la empresa.");
    }

    /** Los combos fijos: tipos, monedas, estados y los estados que permite cada boton. */
    @PostMapping("/catalogos")
    public ResponseEntity<ApiResponse<?>> catalogos() {
        Tipos t = new Tipos();
        return ok(new CatalogosChequeDto(
                opciones(t.lstTipoCheque()),
                opciones(t.lstMonedaCheque()),
                opciones(t.lstEstadoCheque()),
                opciones(t.lstEstadoAccionCheque()),
                opciones(t.lstAccionFechaCobroCheque()),
                opciones(t.lstAccionCierreConVerificacionCheque()),
                opciones(t.lstAccionCierreSinVerificacionCheque())));
    }

    /** Clientes de la empresa ({@code id} = empresa; 0 = la primera del combo, no la del token), para el combo del formulario. */
    @PostMapping("/clientes")
    public ResponseEntity<ApiResponse<?>> listarClientes(@RequestBody(required = false) FiltroIdDto filtro) {
        List<SocioNegocio> lista = clientes.obtenerSocioNegocio(servicio.empresaEfectiva(empresaPedida(filtro)));
        return lista == null || lista.isEmpty() ? sinDatos("No hay clientes para la empresa.") : ok(lista);
    }

    /**
     * Comprueba el par talonario/recibo MIENTRAS se escribe (aviso temprano; al guardar el servidor valida igual). Solo lectura.
     * {@code data} = {@code {valido, mensaje, detalle}}.
     */
    @PostMapping("/talonario/validar")
    public ResponseEntity<ApiResponse<?>> validarTalonario(@RequestBody TalonarioValidacionRequest r, Authentication auth) {
        return ok(servicio.validarTalonario(auth, r));
    }

    /** "Entregado por": jefe de cobranzas, cobradores y choferes activos de la sucursal ({@code id}). */
    @PostMapping("/personal/entregan")
    public ResponseEntity<ApiResponse<?>> quienesEntregan(@RequestBody FiltroIdDto filtro, Authentication auth) {
        return lista(servicio.quienesEntregan(auth, filtro.getId()), "No hay personal asignado a la sucursal.");
    }

    /** Responsables de custodia: jefe de cobranzas y cobradores activos de la sucursal ({@code id}). */
    @PostMapping("/personal/custodia")
    public ResponseEntity<ApiResponse<?>> responsablesDeCustodia(@RequestBody FiltroIdDto filtro, Authentication auth) {
        return lista(servicio.responsablesDeCustodia(auth, filtro.getId()), "No hay personal asignado a la sucursal.");
    }

    /** La grilla: una pagina de cheques de la sucursal, con los filtros. */
    @PostMapping("/listar")
    public ResponseEntity<ApiResponse<?>> listar(@RequestBody ChequeFiltroDto filtro, Authentication auth) {
        return ok(servicio.listar(auth, filtro));
    }

    /** "Completar": el cheque, su historial y las cuatro acciones habilitadas. */
    @PostMapping("/detalle")
    public ResponseEntity<ApiResponse<?>> detalle(@RequestBody FiltroIdDto filtro, Authentication auth) {
        return ok(servicio.detalle(auth, (int) filtro.getId()));
    }

    // ===================================================================== //
    //                             ESCRITURAS                                //
    // ===================================================================== //

    /** Alta (codCheque 0/null) o edicion; ver {@link ChequeRegistroDto#modo}. {@code data} = codCheque. */
    @PostMapping("/registrar")
    public ResponseEntity<ApiResponse<?>> registrar(@RequestBody ChequeRegistroDto dto, Authentication auth) {
        long id = servicio.registrar(auth, dto);
        return creado(dto != null && dto.esAlta() ? "Cheque registrado." : "Cheque actualizado.", id);
    }

    /** Nueva fecha de cobro: accion VEN o ADE. {@code data} = codAccion. */
    @PostMapping("/accion/fecha-cobro")
    public ResponseEntity<ApiResponse<?>> fechaCobro(@RequestBody AccionChequeRequest r, Authentication auth) {
        return creado("Fecha de cobro modificada.", servicio.fechaCobro(auth, r));
    }

    /** Devolver: accion DEV. {@code data} = codAccion. */
    @PostMapping("/accion/devolver")
    public ResponseEntity<ApiResponse<?>> devolver(@RequestBody AccionChequeRequest r, Authentication auth) {
        return creado("Cheque devuelto.", servicio.devolver(auth, r));
    }

    /** Cerrar, con o sin verificacion: accion de cierre y cheque a CER. {@code data} = codAccion. */
    @PostMapping("/accion/cerrar")
    public ResponseEntity<ApiResponse<?>> cerrar(@RequestBody AccionChequeRequest r, Authentication auth) {
        return creado("Cheque cerrado.", servicio.cerrar(auth, r));
    }

    /** Elimina una accion ({@code id}). Se conservan las reglas del legacy: ver {@link ChequeService#eliminarAccion}. */
    @PostMapping("/accion/eliminar")
    public ResponseEntity<ApiResponse<?>> eliminarAccion(@RequestBody FiltroIdDto filtro, Authentication auth) {
        servicio.eliminarAccion(auth, (int) filtro.getId());
        return creado("Acción eliminada.", filtro.getId());
    }

    // ---- traspaso

    /** Cuantos cheques de la sucursal ({@code id}) esperan el traspaso. */
    @PostMapping("/traspaso/pendientes")
    public ResponseEntity<ApiResponse<?>> pendientesDeTraspaso(@RequestBody FiltroIdDto filtro, Authentication auth) {
        return ok(servicio.pendientesDeTraspaso(auth, filtro.getId()));
    }

    /** Traspaso masivo de la sucursal ({@code id}). {@code data} = cuantos cheques se traspasaron. */
    @PostMapping("/traspaso")
    public ResponseEntity<ApiResponse<?>> traspasar(@RequestBody FiltroIdDto filtro, Authentication auth) {
        long cantidad = servicio.traspasar(auth, filtro.getId());
        return creado("Se traspasaron " + cantidad + " cheques.", cantidad);
    }

    // ---- custodia

    /** Cheques de la sucursal ({@code id}) listos para entregar en custodia hoy. */
    @PostMapping("/custodia/cheques")
    public ResponseEntity<ApiResponse<?>> sinCustodio(@RequestBody FiltroIdDto filtro, Authentication auth) {
        return lista(servicio.sinCustodio(auth, filtro.getId()), "No hay cheques para entregar en custodia.");
    }

    /** "A Custodio": entrega los cheques elegidos al responsable. {@code data} = cuantos. */
    @PostMapping("/custodia")
    public ResponseEntity<ApiResponse<?>> custodia(@RequestBody CustodiaChequeRequest r, Authentication auth) {
        int n = servicio.custodia(auth, r);
        return creado("Se entregaron " + n + " cheques en custodia.", n);
    }

    /** "Dar Custodia", paso 1: todos los cheques de la sucursal ({@code id}). Boton {@code btnCustodia2CH}. */
    @PostMapping("/dar-custodia/cheques")
    public ResponseEntity<ApiResponse<?>> chequesParaDarCustodia(@RequestBody FiltroIdDto filtro, Authentication auth) {
        return lista(servicio.chequesParaDarCustodia(auth, filtro.getId()), "No hay cheques en la sucursal.");
    }

    /** "Dar Custodia", paso 2: las entregas a cobranza de un dia. Boton {@code btnCustodia2CH}. */
    @PostMapping("/dar-custodia/entregas")
    public ResponseEntity<ApiResponse<?>> entregasDelDia(@RequestBody FiltroSucursalFechaDto f, Authentication auth) {
        return lista(servicio.entregasDelDia(auth, f == null ? null : f.getCodSucursal(), f == null ? null : f.getFecha()),
                "No hay entregas en esa fecha.");
    }

    /** "Dar Custodia": copia una entrega CUS a otro cheque. {@code data} = codAccion nuevo. */
    @PostMapping("/dar-custodia")
    public ResponseEntity<ApiResponse<?>> darCustodia(@RequestBody DarCustodiaRequest r, Authentication auth) {
        return creado("Custodia asignada.", servicio.darCustodia(auth, r));
    }

    // ===================================================================== //
    //                    REPORTES (PDF) Y SUS LISTAS DE APOYO               //
    // ===================================================================== //

    /** btnRpt1CH "Reporte": cheques recibidos en caja entre dos fechas. */
    @PostMapping("/reporte/recibidos")
    public ResponseEntity<byte[]> reporteRecibidos(@RequestBody ReporteChequeRequest r, Authentication auth) {
        return pdf(reportes.recibidos(auth, r));
    }

    /** btnRpt2CH "Reporte Cheques": cheques de cobranzas por fechas, estado y cliente. */
    @PostMapping("/reporte/cobranzas")
    public ResponseEntity<byte[]> reporteCobranzas(@RequestBody ReporteChequeRequest r, Authentication auth) {
        return pdf(reportes.cobranzas(auth, r));
    }

    /** btnRpt3CH "Reporte Custodio": cheques entregados a cobranza, de un dia y/o un cobrador. */
    @PostMapping("/reporte/custodio")
    public ResponseEntity<byte[]> reporteCustodio(@RequestBody ReporteChequeRequest r, Authentication auth) {
        return pdf(reportes.custodio(auth, r));
    }

    /** btnRpt4CH "Recibo del Ultimo Cheque": el recibo de caja del ultimo cheque que registro el usuario. */
    @PostMapping("/reporte/ultimo-recibo")
    public ResponseEntity<byte[]> reporteUltimoRecibo(@RequestBody ReporteChequeRequest r, Authentication auth) {
        return pdf(reportes.reciboDelUltimoCheque(auth, r));
    }

    /** La nomina del ultimo traspaso de la sucursal. Boton {@code btnTraspasoCH}. */
    @PostMapping("/reporte/traspaso")
    public ResponseEntity<byte[]> reporteTraspaso(@RequestBody ReporteChequeRequest r, Authentication auth) {
        return pdf(reportes.nominaDelTraspaso(auth, r));
    }

    /** btnRpt5CH "Imp Traspso": reimprime la nomina de un traspaso elegido. */
    @PostMapping("/reporte/reimpresion-traspaso")
    public ResponseEntity<byte[]> reporteReimpresionTraspaso(@RequestBody ReporteChequeRequest r, Authentication auth) {
        return pdf(reportes.reimpresionDeTraspaso(auth, r));
    }

    /** btnRpt5CH: los traspasos de un dia ({@code codAccion} y hora), para elegir cual reimprimir. */
    @PostMapping("/traspaso/horas")
    public ResponseEntity<ApiResponse<?>> horasDeTraspaso(@RequestBody FiltroSucursalFechaDto f, Authentication auth) {
        return lista(reportes.horasDeTraspaso(auth, f == null ? null : f.getCodSucursal(), f == null ? null : f.getFecha()),
                "No hay traspasos en esa fecha.");
    }

    // ===================================================================== //
    //                    DOCUMENTO PDF DEL CHEQUE                           //
    // ===================================================================== //

    /**
     * Si el cheque ya tiene su documento PDF. Sin permiso de boton (como los botones de la fila del legacy); el cheque tiene
     * que existir y su sucursal ser visible para el usuario. {@code data} = {@code {existe, nombreArchivo, tamanoBytes, fechaModificacion}}.
     */
    @PostMapping("/pdf/estado")
    public ResponseEntity<ApiResponse<?>> pdfEstado(@RequestBody ChequePdfRequest r, Authentication auth) {
        return ok(pdfs.estado(auth, r == null ? null : r.getCodCheque()));
    }

    /**
     * "Cargar Documento PDF": {@code multipart/form-data} con la parte de texto {@code codCheque} y el archivo en {@code archivo}.
     * Reemplaza el que hubiera. Las dos partes son opcionales para Spring a proposito: si faltan, el servicio responde un
     * mensaje explicito en vez del error generico de una parte ausente. {@code data} = {@code {nombreArchivo, tamanoBytes, reemplazo}}.
     */
    @PostMapping("/pdf/subir")
    public ResponseEntity<ApiResponse<?>> pdfSubir(@RequestParam(value = "codCheque", required = false) String codCheque,
                                                   @RequestParam(value = "archivo", required = false) MultipartFile archivo,
                                                   Authentication auth) {
        ChequePdfSubidoDto subido = pdfs.subir(auth, codCheque, archivo);
        return ResponseEntity.ok(new ApiResponse<>(
                subido.isReemplazo() ? "PDF del cheque reemplazado." : "PDF del cheque guardado.", subido, HttpStatus.OK.value()));
    }

    /** "Descargar Documento PDF": los bytes del archivo, como {@code <codCheque>_.pdf}. Si no existe, error explicito (no un 200 vacio). */
    @PostMapping("/pdf/descargar")
    public ResponseEntity<byte[]> pdfDescargar(@RequestBody ChequePdfRequest r, Authentication auth) {
        ChequePdfService.Descarga d = pdfs.descargar(auth, r == null ? null : r.getCodCheque());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentLength(d.getContenido().length);
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment().filename(d.getNombreDescarga()).build());
        return new ResponseEntity<>(d.getContenido(), headers, HttpStatus.OK);
    }

    // ===================================================================== //
    //                              APOYO                                    //
    // ===================================================================== //

    private static ResponseEntity<byte[]> pdf(byte[] pdf) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentLength(pdf.length);
        headers.setContentType(MediaType.APPLICATION_PDF);
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    private static List<OpcionChequeDto> opciones(List<Tipos> tipos) {
        List<OpcionChequeDto> l = new ArrayList<>(tipos.size());
        for (Tipos t : tipos) l.add(new OpcionChequeDto(t.getCodTipos(), t.getNombre()));
        return l;
    }

    private ResponseEntity<ApiResponse<?>> ok(Object data) {
        return ResponseEntity.ok(new ApiResponse<>(SUCCESS_MESSAGE, data, HttpStatus.OK.value()));
    }

    private ResponseEntity<ApiResponse<?>> creado(String mensaje, long id) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(mensaje, id, HttpStatus.CREATED.value()));
    }

    private ResponseEntity<ApiResponse<?>> sinDatos(String mensaje) {
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(new ApiResponse<>(mensaje, null, HttpStatus.NO_CONTENT.value()));
    }

    private <T> ResponseEntity<ApiResponse<?>> lista(List<T> lista, String mensajeVacio) {
        return lista == null || lista.isEmpty() ? sinDatos(mensajeVacio) : ok(lista);
    }
}
