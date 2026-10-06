package bo.bosque.com.impexpap.controller;

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
import bo.bosque.com.impexpap.commons.MensajesCheque;
import bo.bosque.com.impexpap.commons.NotaRemisionChequeService;
import bo.bosque.com.impexpap.commons.PostergacionChequeService;
import bo.bosque.com.impexpap.commons.PostergacionPdfService;
import bo.bosque.com.impexpap.commons.TransaccionChequeService;
import bo.bosque.com.impexpap.dto.ChequePdfSubidoDto;
import bo.bosque.com.impexpap.dto.ChequeReferenciaRequest;
import bo.bosque.com.impexpap.dto.NotaRemisionRequest;
import bo.bosque.com.impexpap.dto.PostergacionPdfRequest;
import bo.bosque.com.impexpap.dto.PostergacionRequest;
import bo.bosque.com.impexpap.dto.TransaccionBancariaRequest;
import bo.bosque.com.impexpap.utils.ApiResponse;

/**
 * Los tres paneles del detalle de un cheque ({@code cheque.xhtml}, {@code vistaActiva == 2}): <b>notas de remision</b>,
 * <b>transacciones bancarias</b> y <b>postergaciones</b> (con el PDF de cada postergacion). Hermano de
 * {@link ChChequeController} y con el mismo prefijo {@code /cheque}; el contrato, endpoint por endpoint, esta en la seccion
 * "Paneles del detalle" de {@code API_CHEQUES.md} del espacio de migracion.
 *
 * <p>Este controlador solo traduce HTTP: las reglas viven en los servicios, que es lo que se prueba. Todo es {@code POST} sobre
 * {@code ApiResponse}, como el resto de la casa; una lista vacia responde {@code 204}. El usuario de auditoria sale siempre del
 * token, nunca del cuerpo.
 */
@RestController
@CrossOrigin(origins = "*", methods = {RequestMethod.POST, RequestMethod.GET})
@RequestMapping("/cheque")
@PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
public class ChChequeSatelitesController {

    private static final String SUCCESS_MESSAGE = "Operación realizada exitosamente";

    private final NotaRemisionChequeService notas;
    private final TransaccionChequeService transacciones;
    private final PostergacionChequeService postergaciones;
    private final PostergacionPdfService pdfs;

    public ChChequeSatelitesController(NotaRemisionChequeService notas, TransaccionChequeService transacciones,
                                       PostergacionChequeService postergaciones, PostergacionPdfService pdfs) {
        this.notas = notas;
        this.transacciones = transacciones;
        this.postergaciones = postergaciones;
        this.pdfs = pdfs;
    }

    private static Integer codCheque(ChequeReferenciaRequest r) {
        return r == null ? null : r.getCodCheque();
    }

    // ===================================================================== //
    //                         NOTAS DE REMISION                             //
    // ===================================================================== //

    /** Las notas de remision del cheque. Solo exige {@code btnDetalleCH} (los paneles se alcanzan desde «Completar»): el cheque debe existir y su sucursal ser visible. */
    @PostMapping("/nota-remision/listar")
    public ResponseEntity<ApiResponse<?>> listarNotas(@RequestBody ChequeReferenciaRequest r, Authentication auth) {
        return lista(notas.listar(auth, codCheque(r)), "Este cheque no tiene notas de remisión.");
    }

    /** "Nuevo" del panel de notas de remision. Boton {@code btnNuevoNRCH}. {@code data} = filas insertadas (1). */
    @PostMapping("/nota-remision/registrar")
    public ResponseEntity<ApiResponse<?>> registrarNota(@RequestBody NotaRemisionRequest r, Authentication auth) {
        return creado("Nota de remisión registrada.", notas.registrar(auth, r));
    }

    /** "Eliminar" de una nota. Boton {@code btnEliminarNRCH}. {@code data} = filas eliminadas (mas de una si estaba repetida). */
    @PostMapping("/nota-remision/eliminar")
    public ResponseEntity<ApiResponse<?>> eliminarNota(@RequestBody NotaRemisionRequest r, Authentication auth) {
        int filas = notas.eliminar(auth, r);
        return creado(MensajesCheque.notaEliminada(filas, r.getNotaRemision()), filas);
    }

    // ===================================================================== //
    //                       TRANSACCIONES BANCARIAS                         //
    // ===================================================================== //

    /** Las transacciones bancarias del cheque. Solo exige {@code btnDetalleCH} (los paneles se alcanzan desde «Completar»): el cheque debe existir y su sucursal ser visible. */
    @PostMapping("/transaccion/listar")
    public ResponseEntity<ApiResponse<?>> listarTransacciones(@RequestBody ChequeReferenciaRequest r, Authentication auth) {
        return lista(transacciones.listar(auth, codCheque(r)), "Este cheque no tiene transacciones bancarias.");
    }

    /** "Nuevo" del panel de transacciones. Boton {@code btnNuevoNRCH}. {@code data} = filas insertadas (1). */
    @PostMapping("/transaccion/registrar")
    public ResponseEntity<ApiResponse<?>> registrarTransaccion(@RequestBody TransaccionBancariaRequest r, Authentication auth) {
        return creado("Transacción bancaria registrada.", transacciones.registrar(auth, r));
    }

    /** "Eliminar" de una transaccion. Sin boton propio, como hoy (solo exige {@code btnDetalleCH}). {@code data} = filas eliminadas. */
    @PostMapping("/transaccion/eliminar")
    public ResponseEntity<ApiResponse<?>> eliminarTransaccion(@RequestBody TransaccionBancariaRequest r, Authentication auth) {
        int filas = transacciones.eliminar(auth, r);
        return creado(MensajesCheque.transaccionEliminada(filas, r.getNroTransaccion()), filas);
    }

    // ===================================================================== //
    //                            POSTERGACIONES                             //
    // ===================================================================== //

    /** Las postergaciones del cheque, con {@code tienePdf}. Solo exige {@code btnDetalleCH} (los paneles se alcanzan desde «Completar»): el cheque debe existir y su sucursal ser visible. */
    @PostMapping("/postergacion/listar")
    public ResponseEntity<ApiResponse<?>> listarPostergaciones(@RequestBody ChequeReferenciaRequest r, Authentication auth) {
        return lista(postergaciones.listar(auth, codCheque(r)), "Este cheque no tiene postergaciones.");
    }

    /** "Nuevo" del panel de postergaciones. Boton {@code btnNuevoNRCH}. {@code data} = el {@code codPostergacion} nuevo. */
    @PostMapping("/postergacion/registrar")
    public ResponseEntity<ApiResponse<?>> registrarPostergacion(@RequestBody PostergacionRequest r, Authentication auth) {
        return creado("Postergación registrada.", postergaciones.registrar(auth, r));
    }

    /**
     * "Eliminar" de una postergacion. Sin boton propio, como hoy (solo exige {@code btnDetalleCH}); la postergacion tiene que ser del cheque indicado. No borra
     * el PDF (el legacy tampoco). {@code data} = el {@code codPostergacion} eliminado.
     */
    @PostMapping("/postergacion/eliminar")
    public ResponseEntity<ApiResponse<?>> eliminarPostergacion(@RequestBody PostergacionRequest r, Authentication auth) {
        return creado("Postergación eliminada.", postergaciones.eliminar(auth, r));
    }

    // ===================================================================== //
    //                     DOCUMENTO PDF DE LA POSTERGACION                  //
    // ===================================================================== //

    /**
     * Si la postergacion ya tiene su documento PDF. Sin boton propio (como los botones de la fila del legacy; solo exige {@code btnDetalleCH}); la postergacion y
     * su cheque tienen que existir y la sucursal ser visible. {@code data} = {@code {existe, nombreArchivo, tamanoBytes, fechaModificacion}}.
     */
    @PostMapping("/postergacion/pdf/estado")
    public ResponseEntity<ApiResponse<?>> pdfEstado(@RequestBody PostergacionPdfRequest r, Authentication auth) {
        return ok(pdfs.estado(auth, r == null ? null : r.getCodPostergacion()));
    }

    /**
     * "Cargar PDF": {@code multipart/form-data} con la parte de texto {@code codPostergacion} y el archivo en {@code archivo}.
     * Reemplaza el que hubiera. Las dos partes son opcionales para Spring a proposito: si faltan, el servicio responde un
     * mensaje explicito en vez del error generico de una parte ausente. {@code data} = {@code {nombreArchivo, tamanoBytes, reemplazo}}.
     */
    @PostMapping("/postergacion/pdf/subir")
    public ResponseEntity<ApiResponse<?>> pdfSubir(@RequestParam(value = "codPostergacion", required = false) String codPostergacion,
                                                   @RequestParam(value = "archivo", required = false) MultipartFile archivo,
                                                   Authentication auth) {
        ChequePdfSubidoDto subido = pdfs.subir(auth, codPostergacion, archivo);
        return ResponseEntity.ok(new ApiResponse<>(
                subido.isReemplazo() ? "PDF de la postergación reemplazado." : "PDF de la postergación guardado.", subido,
                HttpStatus.OK.value()));
    }

    /** "Descargar PDF": los bytes del archivo, como {@code Posterg_<codPostergacion>_.pdf}. Si no existe, error explicito (no un 200 vacio). */
    @PostMapping("/postergacion/pdf/descargar")
    public ResponseEntity<byte[]> pdfDescargar(@RequestBody PostergacionPdfRequest r, Authentication auth) {
        ChequePdfService.Descarga d = pdfs.descargar(auth, r == null ? null : r.getCodPostergacion());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentLength(d.getContenido().length);
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment().filename(d.getNombreDescarga()).build());
        return new ResponseEntity<>(d.getContenido(), headers, HttpStatus.OK);
    }

    // ===================================================================== //
    //                              APOYO                                    //
    // ===================================================================== //

    private ResponseEntity<ApiResponse<?>> ok(Object data) {
        return ResponseEntity.ok(new ApiResponse<>(SUCCESS_MESSAGE, data, HttpStatus.OK.value()));
    }

    private ResponseEntity<ApiResponse<?>> creado(String mensaje, long dato) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(mensaje, dato, HttpStatus.CREATED.value()));
    }

    private <T> ResponseEntity<ApiResponse<?>> lista(List<T> lista, String mensajeVacio) {
        return lista == null || lista.isEmpty()
                ? ResponseEntity.status(HttpStatus.NO_CONTENT).body(new ApiResponse<>(mensajeVacio, null, HttpStatus.NO_CONTENT.value()))
                : ok(lista);
    }
}
