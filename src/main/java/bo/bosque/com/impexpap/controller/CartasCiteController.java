package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.commons.service.CartaCitePdfService;
import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.dao.IDocumentoCite;
import bo.bosque.com.impexpap.dto.CartaCiteFiltroDto;
import bo.bosque.com.impexpap.model.CiteEmpleado;
import bo.bosque.com.impexpap.model.CiteGestion;
import bo.bosque.com.impexpap.model.CopiaArch;
import bo.bosque.com.impexpap.model.CopiaEncabezado;
import bo.bosque.com.impexpap.model.Documento;
import bo.bosque.com.impexpap.model.Remitente;
import bo.bosque.com.impexpap.utils.ApiResponse;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cartas CITE — redacción y archivo de la correspondencia numerada.
 *
 * <p>Migración del módulo JSF {@code web/Bosque/tcrDocumento} de Bosque v2.
 * Cada documento lleva un correlativo por (tipo, empresa, gestión) que se
 * imprime como {@code G.A./007/2025} y que, una vez emitido, no se reutiliza.
 *
 * <p><b>Persistencia:</b> sin JPA, todo por stored procedures
 * ({@code p_abm_tcr_*}, {@code p_list_tcr_Documento}) vía {@code SpHelper}.
 * Los SPs viejos del módulo JSF quedaron intactos y ambos sistemas conviven
 * sobre las mismas tablas {@code tcr_*}.
 *
 * <p><b>El correlativo lo asigna la base, no el cliente.</b> El endpoint
 * {@code /siguiente-cite} devuelve el número que <i>tocaría</i>, para mostrarlo
 * mientras se redacta; el definitivo se calcula dentro de la transacción del
 * alta y vuelve en el mensaje de la respuesta. Si entre una cosa y la otra
 * otro usuario guardó primero, el número real será el siguiente y no hay
 * duplicado.
 *
 * <p><b>Tipos de documento</b> (tabla {@code tcr_tipoDocumento}): 1 CARTA,
 * 2 MEMORANDO, 6 CERTF. TRABAJO, 7 COM. INTERNA, 8 INF. CONTROL INTERNO,
 * 9 COM. CI. Cada uno tiene su formato Jasper.
 */
@Slf4j
@RestController
@CrossOrigin(origins = "*", methods = {RequestMethod.POST, RequestMethod.GET})
@RequestMapping("/cartas-cite")
@PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
public class CartasCiteController {

    private static final String SUCCESS_MESSAGE = "Operación realizada exitosamente";

    /** idTipoDoc → nombre del jrxml. Es la misma tabla que tenía el bean JSF. */
    private static final Map<Long, String> REPORTES = new HashMap<>();
    /** idTipoDoc → prefijo del archivo descargado. */
    private static final Map<Long, String> NOMBRES_ARCHIVO = new HashMap<>();

    static {
        REPORTES.put(1L, "RptCarta");
        REPORTES.put(2L, "RptMemo");
        REPORTES.put(6L, "RptCertfTrab");
        REPORTES.put(7L, "RptComInterna");
        REPORTES.put(8L, "RptInformeCi");
        REPORTES.put(9L, "RptComCi");

        NOMBRES_ARCHIVO.put(1L, "CARTA");
        NOMBRES_ARCHIVO.put(2L, "MEMORANDO");
        NOMBRES_ARCHIVO.put(6L, "CERTF_TRABAJO");
        NOMBRES_ARCHIVO.put(7L, "COM_INTERNA");
        NOMBRES_ARCHIVO.put(8L, "INF_CONTROL_INTERNO");
        NOMBRES_ARCHIVO.put(9L, "COM_CI");
    }

    private final IDocumentoCite documentoDao;
    private final CartaCitePdfService pdfService;

    public CartasCiteController(IDocumentoCite documentoDao, CartaCitePdfService pdfService) {
        this.documentoDao = documentoDao;
        this.pdfService = pdfService;
    }

    // ══════════════════════════ CONSULTAS ══════════════════════════

    /**
     * Listado paginado. Cada fila trae {@code totalRegistros} con el total que
     * matchea el filtro, así el cliente pagina sin una segunda llamada.
     *
     * <p>Sin fechas, el SP toma los últimos tres meses: el módulo viejo
     * arrancaba con la fecha de hoy y mostraba la pantalla vacía, que a simple
     * vista parecía "no hay cartas".
     */
    @PostMapping("/listar")
    public ResponseEntity<ApiResponse<?>> listar(@RequestBody CartaCiteFiltroDto f) {
        List<Documento> lista = documentoDao.listarDocumentos(
                f.getFechaDesde(), f.getFechaHasta(), f.getIdTipoDoc(), f.getCodEmpresa(),
                f.getCodUsuario(), f.getBuscar(), f.getPagina(), f.getTamanoPagina());
        return procesarLista(lista, "No se encontraron documentos con esos filtros.");
    }

    /**
     * Un documento completo: cabecera más copias de archivo, destinatarios y
     * remitentes.
     *
     * <p>Va todo en una sola llamada y no en cuatro porque la pantalla de
     * edición los necesita juntos para poder pintar el formulario, y en móvil
     * cuatro viajes seguidos se notan.
     */
    @PostMapping("/obtener")
    public ResponseEntity<ApiResponse<?>> obtener(@RequestBody CartaCiteFiltroDto f) {
        Documento doc = documentoDao.obtenerDocumento(f.getIdDocumento());
        if (doc == null) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(new ApiResponse<>("El documento no existe.", null, HttpStatus.NO_CONTENT.value()));
        }

        doc.setCopiasArchivo(documentoDao.listarCopiasArch(doc.getIdDocumento()));
        doc.setDestinatarios(documentoDao.listarCopiasEncabezado(doc.getIdDocumento()));
        doc.setRemitentes(documentoDao.listarRemitentes(doc.getIdDocumento()));

        return ResponseEntity.ok(new ApiResponse<>(SUCCESS_MESSAGE, doc, HttpStatus.OK.value()));
    }

    /**
     * Número de CITE que tocaría para ese tipo y empresa, junto con la gestión
     * activa. Es previsualización, no reserva.
     */
    @PostMapping("/siguiente-cite")
    public ResponseEntity<ApiResponse<?>> siguienteCite(@RequestBody CartaCiteFiltroDto f) {
        CiteGestion g = documentoDao.obtenerSiguienteCite(f.getIdTipoDoc(), f.getCodEmpresa());
        if (g == null) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(new ApiResponse<>("No hay una gestión activa.", null, HttpStatus.NO_CONTENT.value()));
        }
        return ResponseEntity.ok(new ApiResponse<>(SUCCESS_MESSAGE, g, HttpStatus.OK.value()));
    }

    @PostMapping("/tipos-documento")
    public ResponseEntity<ApiResponse<?>> tiposDocumento() {
        return procesarLista(documentoDao.listarTiposDocumento(), "No hay tipos de documento configurados.");
    }

    @PostMapping("/areas")
    public ResponseEntity<ApiResponse<?>> areas(@RequestBody CartaCiteFiltroDto f) {
        return procesarLista(documentoDao.listarAreas(f.getCodEmpresa()),
                "La empresa no tiene áreas configuradas.");
    }

    /** Empleados activos, para el destinatario de memorandos y com. internas. */
    @PostMapping("/empleados")
    public ResponseEntity<ApiResponse<?>> empleados() {
        return procesarLista(documentoDao.listarEmpleados(), "No hay empleados activos.");
    }

    @PostMapping("/empleado")
    public ResponseEntity<ApiResponse<?>> empleado(@RequestBody CartaCiteFiltroDto f) {
        CiteEmpleado e = documentoDao.obtenerEmpleado(f.getCodEmpleado());
        if (e == null) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(new ApiResponse<>("El empleado no existe o no está activo.", null, HttpStatus.NO_CONTENT.value()));
        }
        return ResponseEntity.ok(new ApiResponse<>(SUCCESS_MESSAGE, e, HttpStatus.OK.value()));
    }

    /** Nombre y cargo del usuario logueado, para precargar el primer remitente. */
    @PostMapping("/firma-usuario")
    public ResponseEntity<ApiResponse<?>> firmaUsuario(@RequestBody CartaCiteFiltroDto f) {
        CiteEmpleado e = documentoDao.obtenerFirmaUsuario(f.getCodUsuario());
        if (e == null) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(new ApiResponse<>("El usuario no tiene un cargo vigente registrado.", null, HttpStatus.NO_CONTENT.value()));
        }
        return ResponseEntity.ok(new ApiResponse<>(SUCCESS_MESSAGE, e, HttpStatus.OK.value()));
    }

    @PostMapping("/gestiones")
    public ResponseEntity<ApiResponse<?>> gestiones() {
        return procesarLista(documentoDao.listarGestiones(), "No hay gestiones registradas.");
    }

    // ══════════════════════════ ESCRITURA ══════════════════════════

    /**
     * Deja activa la gestión del año en curso, creándola si falta.
     *
     * <p>El módulo JSF hacía esto cada vez que se abría el formulario. Acá es
     * un endpoint explícito que la pantalla llama al entrar: sin él, el primer
     * documento de enero se numeraría dentro del correlativo del año anterior.
     */
    @PostMapping("/preparar-gestion")
    @Transactional
    public ResponseEntity<ApiResponse<?>> prepararGestion(@RequestBody CartaCiteFiltroDto f) {
        Documento mb = new Documento();
        mb.setAudUsuario(f.getAudUsuario());
        RespuestaSp res = documentoDao.registrarDocumento(mb, "G");
        return ResponseEntity.ok(new ApiResponse<>(res.getErrormsg(), res.getIdGenerado(), HttpStatus.OK.value()));
    }

    /**
     * Guarda el documento completo: cabecera, copias de archivo, destinatarios
     * y remitentes, más las bajas que se hayan marcado en la pantalla.
     *
     * <p><b>Orden de operaciones.</b> Primero las bajas y después las altas,
     * y no al revés: el SP de remitentes rechaza el tercero, así que reemplazar
     * los dos existentes fallaría si se insertara antes de borrar.
     *
     * <p><b>Transaccional.</b> Si algo falla a mitad, se cae todo. Sin esto
     * quedaría un documento con la mitad de sus destinatarios — que es
     * exactamente lo que hacía el JSF, donde cada hijo se grababa suelto y un
     * error en el tercero dejaba los dos primeros escritos.
     *
     * @param payload documento con sus hijos anidados; {@code idDocumento == 0}
     *                es alta, mayor a 0 es modificación
     */
    @PostMapping("/guardar")
    @Transactional
    public ResponseEntity<ApiResponse<?>> guardar(@RequestBody Documento payload) {

        boolean esAlta = payload.getIdDocumento() == 0;

        // --- Cabecera ---
        RespuestaSp resDoc = documentoDao.registrarDocumento(payload, esAlta ? "I" : "U");
        ejecutar(resDoc, "Error guardando el documento");

        long idDocumento = resDoc.getIdGenerado() > 0 ? resDoc.getIdGenerado() : payload.getIdDocumento();

        // --- Bajas de hijos ---
        if (payload.getCopiasArchivoAEliminar() != null) {
            for (Long id : payload.getCopiasArchivoAEliminar()) {
                CopiaArch del = new CopiaArch();
                del.setIdCopiaArch(id);
                del.setAudUsuario(payload.getAudUsuario());
                ejecutar(documentoDao.registrarCopiaArch(del, "D"), "Error eliminando la copia de archivo " + id);
            }
        }
        if (payload.getDestinatariosAEliminar() != null) {
            for (Long id : payload.getDestinatariosAEliminar()) {
                CopiaEncabezado del = new CopiaEncabezado();
                del.setIdCopiaEncab(id);
                del.setAudUsuario(payload.getAudUsuario());
                ejecutar(documentoDao.registrarCopiaEncabezado(del, "D"), "Error eliminando el destinatario " + id);
            }
        }
        if (payload.getRemitentesAEliminar() != null) {
            for (Long id : payload.getRemitentesAEliminar()) {
                Remitente del = new Remitente();
                del.setIdRemitente(id);
                del.setAudUsuario(payload.getAudUsuario());
                ejecutar(documentoDao.registrarRemitente(del, "D"), "Error eliminando el remitente " + id);
            }
        }

        // --- Altas y modificaciones de hijos ---
        if (payload.getCopiasArchivo() != null) {
            for (CopiaArch cc : payload.getCopiasArchivo()) {
                if (esVacio(cc.getCopiaArch())) continue;
                cc.setIdDocumento(idDocumento);
                if (cc.getAudUsuario() == 0) cc.setAudUsuario(payload.getAudUsuario());
                /* En un duplicado los hijos llegan con el id del documento
                   original; el alta nueva tiene que insertarlos, no actualizar
                   los del documento que se copió. */
                String accion = (esAlta || cc.getIdCopiaArch() == 0) ? "I" : "U";
                ejecutar(documentoDao.registrarCopiaArch(cc, accion), "Error en la copia de archivo");
            }
        }
        if (payload.getDestinatarios() != null) {
            for (CopiaEncabezado ce : payload.getDestinatarios()) {
                if (esVacio(ce.getCopiaEnca())) continue;
                ce.setIdDocumento(idDocumento);
                if (ce.getAudUsuario() == 0) ce.setAudUsuario(payload.getAudUsuario());
                String accion = (esAlta || ce.getIdCopiaEncab() == 0) ? "I" : "U";
                ejecutar(documentoDao.registrarCopiaEncabezado(ce, accion), "Error en el destinatario " + ce.getCopiaEnca());
            }
        }
        if (payload.getRemitentes() != null) {
            for (Remitente rem : payload.getRemitentes()) {
                if (esVacio(rem.getRemitente())) continue;
                rem.setIdDocumento(idDocumento);
                if (rem.getAudUsuario() == 0) rem.setAudUsuario(payload.getAudUsuario());
                String accion = (esAlta || rem.getIdRemitente() == 0) ? "I" : "U";
                ejecutar(documentoDao.registrarRemitente(rem, accion), "Error en el remitente " + rem.getRemitente());
            }
        }

        // El mensaje del SP trae el número de CITE que quedó asignado.
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(resDoc.getErrormsg(), idDocumento, HttpStatus.CREATED.value()));
    }

    /**
     * Anula un documento. Es baja lógica: desaparece de los listados pero el
     * número de CITE queda consumido, porque pudo haber salido en papel y dos
     * documentos con el mismo número serían un problema de archivo.
     *
     * <p>La marca se guarda en {@code tcr_documentoAnulado} y no en la fila del
     * documento, para no alterar la numeración del módulo JSF.
     *
     * @param f idDocumento, motivo y audUsuario
     */
    @PostMapping("/anular")
    @Transactional
    public ResponseEntity<ApiResponse<?>> anular(@RequestBody CartaCiteFiltroDto f) {
        Documento mb = new Documento();
        mb.setIdDocumento(f.getIdDocumento());
        mb.setMotivo(f.getMotivo());
        mb.setAudUsuario(f.getAudUsuario());
        RespuestaSp res = documentoDao.registrarDocumento(mb, "D");
        ejecutar(res, "Error anulando el documento");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(res.getErrormsg(), f.getIdDocumento(), HttpStatus.CREATED.value()));
    }

    // ══════════════════════════ PDF ══════════════════════════

    /**
     * PDF del documento, con el mismo formato Jasper que imprimía el sistema
     * viejo. Marca el documento como exportado.
     *
     * <p>El parámetro {@code logo} sólo lo mira la carta (tipo 1), que se
     * imprime con o sin membrete según si el papel ya lo trae preimpreso.
     */
    @PostMapping("/generar-pdf")
    public ResponseEntity<byte[]> generarPdf(@RequestBody CartaCiteFiltroDto f) {
        Documento doc = documentoDao.obtenerDocumento(f.getIdDocumento());
        if (doc == null) throw new SpBusinessException("El documento no existe.");

        String reporte = REPORTES.get(doc.getIdTipoDoc());
        if (reporte == null) {
            throw new SpBusinessException("El tipo de documento " + doc.getIdTipoDoc() + " no tiene un formato de impresión definido.");
        }

        Map<String, Object> params = new HashMap<>();
        /* Los tres van casteados a int a propósito. En los jrxml están
           declarados como java.lang.Integer, y Jasper castea el valor al tipo
           declarado sin convertirlo: un Long entra y tira ClassCastException
           al armar el PreparedStatement. Documento los tiene como long, así
           que sin el casteo autoboxean a Long. */
        params.put("idDocumento", (int) doc.getIdDocumento());
        params.put("nroCite", doc.getNroCite());
        params.put("idTipoDoc", (int) doc.getIdTipoDoc());
        params.put("logo", esVacio(f.getLogo()) ? "SI" : f.getLogo());
        /* Ningún formato por documento declara codEmpresa; va igual porque es
           lo que usa el servicio para elegir el membrete. Jasper recorre los
           parámetros que declara el reporte, así que uno de más no molesta. */
        params.put("codEmpresa", (int) doc.getCodEmpresa());

        byte[] pdf = pdfService.generar(reporte, params);

        /* Se marca exportado después de generar: si el reporte falla, el
           documento no queda marcado como impreso. */
        try {
            Documento marca = new Documento();
            marca.setIdDocumento(doc.getIdDocumento());
            marca.setAudUsuario(f.getAudUsuario());
            documentoDao.registrarDocumento(marca, "X");
        } catch (Exception e) {
            // Que falle la marca no justifica no entregar el PDF ya generado.
            log.warn("No se pudo marcar como exportado el documento {}", doc.getIdDocumento(), e);
        }

        String nombre = NOMBRES_ARCHIVO.get(doc.getIdTipoDoc()) + "_" + doc.getNroCite() + ".pdf";
        return respuestaPdf(pdf, nombre);
    }

    /**
     * Reporte mensual de documentos emitidos ({@code RptCartaMensual}).
     *
     * @param f mes (0 = toda la gestión), anio, idTipoDoc y codEmpresa
     */
    @PostMapping("/reporte-mensual")
    public ResponseEntity<byte[]> reporteMensual(@RequestBody CartaCiteFiltroDto f) {
        if (f.getAnio() <= 0)      throw new SpBusinessException("Debe seleccionar la gestión.");
        if (f.getIdTipoDoc() <= 0) throw new SpBusinessException("Debe seleccionar el tipo de documento.");
        if (f.getCodEmpresa() <= 0) throw new SpBusinessException("Debe seleccionar la empresa.");

        Map<String, Object> params = new HashMap<>();
        params.put("mes", f.getMes());
        params.put("anio", f.getAnio());
        params.put("idTipoDoc", (int) f.getIdTipoDoc());
        params.put("codEmpresa", (int) f.getCodEmpresa());

        byte[] pdf = pdfService.generar("RptCartaMensual", params);
        String nombre = "REPORTE_MENSUAL_" + f.getMes() + "_" + f.getAnio() + ".pdf";
        return respuestaPdf(pdf, nombre);
    }

    // ══════════════════════════ AUXILIARES ══════════════════════════

    private void ejecutar(RespuestaSp res, String contexto) {
        if (res.getError() != 0) throw new SpBusinessException(contexto + ": " + res.getErrormsg());
    }

    private static boolean esVacio(String s) {
        return s == null || s.trim().isEmpty();
    }

    private ResponseEntity<byte[]> respuestaPdf(byte[] pdf, String nombreArchivo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentLength(pdf.length);
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + nombreArchivo + "\"");
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    private <T> ResponseEntity<ApiResponse<?>> procesarLista(List<T> lista, String mensajeVacio) {
        if (lista == null || lista.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(new ApiResponse<>(mensajeVacio, null, HttpStatus.NO_CONTENT.value()));
        }
        return ResponseEntity.ok(new ApiResponse<>(SUCCESS_MESSAGE, lista, HttpStatus.OK.value()));
    }
}
