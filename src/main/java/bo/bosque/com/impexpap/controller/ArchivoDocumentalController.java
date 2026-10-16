package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.commons.AccesoPantalla;
import bo.bosque.com.impexpap.commons.FotosCajaService;
import bo.bosque.com.impexpap.commons.ReporteArchivoService;
import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.dao.IArchivoDocumental;
import bo.bosque.com.impexpap.dao.IEmpresa;
import bo.bosque.com.impexpap.dto.FiltroReporteArchivo;
import bo.bosque.com.impexpap.dto.PedidoEntrega;
import bo.bosque.com.impexpap.dto.PedidoMovimientoCaja;
import bo.bosque.com.impexpap.model.ArchivoFisico;
import bo.bosque.com.impexpap.model.CajaArchivo;
import bo.bosque.com.impexpap.model.ContenidoCaja;
import bo.bosque.com.impexpap.model.DestruccionDocumento;
import bo.bosque.com.impexpap.model.EntregaDocumento;
import bo.bosque.com.impexpap.model.FotoCaja;
import bo.bosque.com.impexpap.model.PrestamoDocumento;
import bo.bosque.com.impexpap.model.TipoDocumentoArchivo;
import bo.bosque.com.impexpap.security.jwt.DatosToken;
import bo.bosque.com.impexpap.utils.ApiResponse;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import org.springframework.http.CacheControl;
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

import java.util.List;

/**
 * Archivo de documentos (tablas {@code tarc_*}): archivos con su responsable, tipos, cajas, su contenido, hasta 3 fotos
 * o PDF por caja, entregas a empleados, la bitacora de destruccion y reportes. Todo exige la vista {@value #PANTALLA};
 * {@code audUsuario} sale del token. Alta si el id es 0, edicion si es mayor.
 */
@RestController
@CrossOrigin(origins = "*", methods = {RequestMethod.POST, RequestMethod.GET})
@RequestMapping("/archivo-documental")
@PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
public class ArchivoDocumentalController {

    /** {@code tb_vista.direccion} de "Almacenaje de Documentos" (vista 95, de 2020, bajo Facturas Manuales);
     *  el mismo literal es AppConstants.rutaArchivoDocumental en Flutter. */
    public static final String PANTALLA = "tmtoDocumento/almacenar";

    private static final String EXITO = "Operación realizada exitosamente";

    private final IArchivoDocumental dao;
    private final FotosCajaService fotos;
    private final IEmpresa empresas;
    private final AccesoPantalla pantallas;
    private final ReporteArchivoService reportes;

    public ArchivoDocumentalController(IArchivoDocumental dao, FotosCajaService fotos, IEmpresa empresas,
                                       AccesoPantalla pantallas, ReporteArchivoService reportes) {
        this.dao = dao;
        this.fotos = fotos;
        this.empresas = empresas;
        this.pantallas = pantallas;
        this.reportes = reportes;
    }

    // ------------------------------------------------------------------ archivos

    @PostMapping("/archivos/listar")
    public ResponseEntity<ApiResponse<?>> listarArchivos(Authentication auth) {
        pantallas.exigir(auth, PANTALLA);
        return listado(dao.listarArchivos(), "No hay archivos registrados.");
    }

    @PostMapping("/archivos/registrar")
    public ResponseEntity<ApiResponse<?>> registrarArchivo(@RequestBody ArchivoFisico mb, Authentication auth) {
        pantallas.exigir(auth, PANTALLA);
        mb.setAudUsuario(DatosToken.codUsuarioDe(auth));
        return escritura(dao.registrarArchivo(mb, mb.getCodArchivo() == 0 ? "I" : "U"));
    }

    @PostMapping("/archivos/eliminar")
    public ResponseEntity<ApiResponse<?>> eliminarArchivo(@RequestBody ArchivoFisico mb, Authentication auth) {
        pantallas.exigir(auth, PANTALLA);
        ArchivoFisico baja = new ArchivoFisico();
        baja.setCodArchivo(mb.getCodArchivo());
        baja.setAudUsuario(DatosToken.codUsuarioDe(auth));
        return escritura(dao.registrarArchivo(baja, "D"));
    }

    /** Sucursales para el combo del archivo: tb_sucursal se repite por empresa, por eso viaja su nombre. */
    @PostMapping("/sucursales")
    public ResponseEntity<ApiResponse<?>> listarSucursales(Authentication auth) {
        pantallas.exigir(auth, PANTALLA);
        return listado(dao.listarSucursales(), "No hay sucursales registradas.");
    }

    /** Empleados activos (y los responsables actuales) para elegir responsable o a quien se presta. */
    @PostMapping("/empleados")
    public ResponseEntity<ApiResponse<?>> listarEmpleados(Authentication auth) {
        pantallas.exigir(auth, PANTALLA);
        return listado(dao.listarEmpleados(), "No hay empleados activos.");
    }

    // ------------------------------------------------------------------ tipos de documento

    @PostMapping("/tipos/listar")
    public ResponseEntity<ApiResponse<?>> listarTipos(Authentication auth) {
        pantallas.exigir(auth, PANTALLA);
        return listado(dao.listarTipos(), "No hay tipos de documento registrados.");
    }

    @PostMapping("/tipos/registrar")
    public ResponseEntity<ApiResponse<?>> registrarTipo(@RequestBody TipoDocumentoArchivo mb, Authentication auth) {
        pantallas.exigir(auth, PANTALLA);
        mb.setAudUsuario(DatosToken.codUsuarioDe(auth));
        return escritura(dao.registrarTipo(mb, mb.getCodTipoDocumento() == 0 ? "I" : "U"));
    }

    @PostMapping("/tipos/eliminar")
    public ResponseEntity<ApiResponse<?>> eliminarTipo(@RequestBody TipoDocumentoArchivo mb, Authentication auth) {
        pantallas.exigir(auth, PANTALLA);
        TipoDocumentoArchivo baja = new TipoDocumentoArchivo();
        baja.setCodTipoDocumento(mb.getCodTipoDocumento());
        baja.setAudUsuario(DatosToken.codUsuarioDe(auth));
        return escritura(dao.registrarTipo(baja, "D"));
    }

    // ------------------------------------------------------------------ cajas

    /** Cuerpo vacio = todas las cajas; {@code codArchivo} acota a un archivo. */
    @PostMapping("/cajas/listar")
    public ResponseEntity<ApiResponse<?>> listarCajas(@RequestBody(required = false) CajaArchivo filtro,
                                                      Authentication auth) {
        pantallas.exigir(auth, PANTALLA);
        return listado(dao.listarCajas(filtro == null ? null : filtro.getCodArchivo()), "No hay cajas registradas.");
    }

    /** {@code data} = el numero que sigue en el archivo, para proponerlo en el alta. */
    @PostMapping("/cajas/siguiente-numero")
    public ResponseEntity<ApiResponse<?>> siguienteNumero(@RequestBody CajaArchivo filtro, Authentication auth) {
        pantallas.exigir(auth, PANTALLA);
        long codArchivo = filtro == null || filtro.getCodArchivo() == null ? 0 : filtro.getCodArchivo();
        return ResponseEntity.ok(new ApiResponse<>(EXITO, dao.siguienteNumero(codArchivo), HttpStatus.OK.value()));
    }

    @PostMapping("/cajas/registrar")
    public ResponseEntity<ApiResponse<?>> registrarCaja(@RequestBody CajaArchivo mb, Authentication auth) {
        pantallas.exigir(auth, PANTALLA);
        mb.setAudUsuario(DatosToken.codUsuarioDe(auth));
        return escritura(dao.registrarCaja(mb, mb.getCodCaja() == 0 ? "I" : "U"));
    }

    /** El SP rechaza la caja con contenido; si procede, el servicio borra tambien los archivos de sus fotos. */
    @PostMapping("/cajas/eliminar")
    public ResponseEntity<ApiResponse<?>> eliminarCaja(@RequestBody CajaArchivo mb, Authentication auth) {
        pantallas.exigir(auth, PANTALLA);
        return escritura(fotos.eliminarCaja(mb.getCodCaja(), DatosToken.codUsuarioDe(auth)));
    }

    // ------------------------------------------------------------------ contenido

    /** Cuerpo vacio = todo el contenido (la app busca del lado del cliente); {@code codCaja} acota a una caja. */
    @PostMapping("/contenido/listar")
    public ResponseEntity<ApiResponse<?>> listarContenido(@RequestBody(required = false) ContenidoCaja filtro,
                                                          Authentication auth) {
        pantallas.exigir(auth, PANTALLA);
        return listado(dao.listarContenido(filtro == null ? null : filtro.getCodCaja()), "No hay contenido registrado.");
    }

    @PostMapping("/contenido/registrar")
    public ResponseEntity<ApiResponse<?>> registrarContenido(@RequestBody ContenidoCaja mb, Authentication auth) {
        pantallas.exigir(auth, PANTALLA);
        mb.setAudUsuario(DatosToken.codUsuarioDe(auth));
        return escritura(dao.registrarContenido(mb, mb.getCodContenido() == 0 ? "I" : "U"));
    }

    @PostMapping("/contenido/eliminar")
    public ResponseEntity<ApiResponse<?>> eliminarContenido(@RequestBody ContenidoCaja mb, Authentication auth) {
        pantallas.exigir(auth, PANTALLA);
        ContenidoCaja baja = new ContenidoCaja();
        baja.setCodContenido(mb.getCodContenido());
        baja.setAudUsuario(DatosToken.codUsuarioDe(auth));
        return escritura(dao.registrarContenido(baja, "D"));
    }

    // ------------------------------------------------------------------ fotos

    @PostMapping("/fotos/listar")
    public ResponseEntity<ApiResponse<?>> listarFotos(@RequestBody FotoCaja filtro, Authentication auth) {
        pantallas.exigir(auth, PANTALLA);
        long codCaja = filtro == null || filtro.getCodCaja() == null ? 0 : filtro.getCodCaja();
        return listado(dao.listarFotos(codCaja), "La caja no tiene fotos.");
    }

    /** multipart: parte de texto {@code codCaja} y la imagen o el PDF en {@code archivo}. {@code data} = codFoto. */
    @PostMapping("/fotos/subir")
    public ResponseEntity<ApiResponse<?>> subirFoto(@RequestParam(value = "codCaja", required = false) String codCaja,
                                                    @RequestParam(value = "archivo", required = false) MultipartFile archivo,
                                                    Authentication auth) {
        pantallas.exigir(auth, PANTALLA);
        long id = fotos.subir(idDeTexto(codCaja, "Falta indicar la caja de la foto o PDF."), archivo,
                DatosToken.codUsuarioDe(auth));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("Agregado a la caja.", id, HttpStatus.CREATED.value()));
    }

    /** multipart: parte de texto {@code codFoto} y la imagen o el PDF nuevo en {@code archivo}. */
    @PostMapping("/fotos/reemplazar")
    public ResponseEntity<ApiResponse<?>> reemplazarFoto(@RequestParam(value = "codFoto", required = false) String codFoto,
                                                         @RequestParam(value = "archivo", required = false) MultipartFile archivo,
                                                         Authentication auth) {
        pantallas.exigir(auth, PANTALLA);
        long id = idDeTexto(codFoto, "Falta indicar qué foto o PDF se reemplaza.");
        fotos.reemplazar(id, archivo, DatosToken.codUsuarioDe(auth));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("Reemplazado.", id, HttpStatus.CREATED.value()));
    }

    @PostMapping("/fotos/eliminar")
    public ResponseEntity<ApiResponse<?>> eliminarFoto(@RequestBody FotoCaja mb, Authentication auth) {
        pantallas.exigir(auth, PANTALLA);
        fotos.eliminar(mb.getCodFoto(), DatosToken.codUsuarioDe(auth));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("Eliminado.", mb.getCodFoto(), HttpStatus.CREATED.value()));
    }

    /** Los bytes de la imagen o del PDF. Si falta el archivo responde 400 con el motivo, no un 200 vacio. */
    @PostMapping("/fotos/ver")
    public ResponseEntity<byte[]> verFoto(@RequestBody FotoCaja mb, Authentication auth) {
        pantallas.exigir(auth, PANTALLA);
        FotosCajaService.Imagen imagen = fotos.leer(mb.getCodFoto());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(imagen.getTipo()));
        headers.setContentLength(imagen.getContenido().length);
        headers.setCacheControl(CacheControl.noStore());
        return new ResponseEntity<>(imagen.getContenido(), headers, HttpStatus.OK);
    }

    // ------------------------------------------------------------------ entregas y sus renglones (prestamos)

    @PostMapping("/entregas/listar")
    public ResponseEntity<ApiResponse<?>> listarEntregas(Authentication auth) {
        pantallas.exigir(auth, PANTALLA);
        return listado(dao.listarEntregas(), "No hay entregas registradas.");
    }

    /** Cajas completas y documentos de cualquier caja a un empleado, en un solo paso; la hora la pone el servidor. */
    @PostMapping("/entregas/registrar")
    public ResponseEntity<ApiResponse<?>> registrarEntrega(@RequestBody PedidoEntrega pedido, Authentication auth) {
        pantallas.exigir(auth, PANTALLA);
        return escritura(dao.registrarEntrega(pedido, DatosToken.codUsuarioDe(auth)));
    }

    /** {@code codEntrega} y, si se quiere, la {@code observacion} de la devolucion: vuelve todo lo que sigue afuera. */
    @PostMapping("/entregas/devolver")
    public ResponseEntity<ApiResponse<?>> devolverEntrega(@RequestBody EntregaDocumento mb, Authentication auth) {
        pantallas.exigir(auth, PANTALLA);
        return escritura(dao.devolverEntrega(mb.getCodEntrega(), mb.getObservacion(), DatosToken.codUsuarioDe(auth)));
    }

    @PostMapping("/entregas/anular")
    public ResponseEntity<ApiResponse<?>> anularEntrega(@RequestBody EntregaDocumento mb, Authentication auth) {
        pantallas.exigir(auth, PANTALLA);
        return escritura(dao.anularEntrega(mb.getCodEntrega(), DatosToken.codUsuarioDe(auth)));
    }

    /** Cuerpo vacio = los renglones de todas las cajas (lo que sigue afuera primero); {@code codCaja} acota a una. */
    @PostMapping("/prestamos/listar")
    public ResponseEntity<ApiResponse<?>> listarPrestamos(@RequestBody(required = false) PrestamoDocumento filtro,
                                                          Authentication auth) {
        pantallas.exigir(auth, PANTALLA);
        return listado(dao.listarPrestamos(filtro == null ? null : filtro.getCodCaja()), "No hay prestamos registrados.");
    }

    /** {@code codPrestamo} y, si se quiere, la {@code observacionDevolucion}: vuelve ese renglon solo. */
    @PostMapping("/prestamos/devolver")
    public ResponseEntity<ApiResponse<?>> devolver(@RequestBody PrestamoDocumento mb, Authentication auth) {
        pantallas.exigir(auth, PANTALLA);
        return escritura(dao.devolver(mb.getCodPrestamo(), mb.getObservacionDevolucion(), DatosToken.codUsuarioDe(auth)));
    }

    @PostMapping("/prestamos/anular")
    public ResponseEntity<ApiResponse<?>> anularPrestamo(@RequestBody PrestamoDocumento mb, Authentication auth) {
        pantallas.exigir(auth, PANTALLA);
        return escritura(dao.anularPrestamo(mb.getCodPrestamo(), DatosToken.codUsuarioDe(auth)));
    }

    // ------------------------------------------------------------------ bitacora de destruccion

    @PostMapping("/destrucciones/listar")
    public ResponseEntity<ApiResponse<?>> listarDestrucciones(@RequestBody(required = false) DestruccionDocumento filtro,
                                                              Authentication auth) {
        pantallas.exigir(auth, PANTALLA);
        return listado(dao.listarDestrucciones(filtro == null ? null : filtro.getCodCaja()),
                "No hay destrucciones registradas.");
    }

    /** Las lineas del pedido, o la caja completa si no trae lineas; la hora la pone el servidor. */
    @PostMapping("/destrucciones/registrar")
    public ResponseEntity<ApiResponse<?>> destruir(@RequestBody PedidoMovimientoCaja pedido, Authentication auth) {
        pantallas.exigir(auth, PANTALLA);
        return escritura(dao.destruir(pedido, DatosToken.codUsuarioDe(auth)));
    }

    @PostMapping("/destrucciones/anular")
    public ResponseEntity<ApiResponse<?>> anularDestruccion(@RequestBody DestruccionDocumento mb, Authentication auth) {
        pantallas.exigir(auth, PANTALLA);
        return escritura(dao.anularDestruccion(mb.getCodDestruccion(), DatosToken.codUsuarioDe(auth)));
    }

    // ------------------------------------------------------------------ reportes

    /**
     * El PDF del reporte pedido ({@code reporte}: POR_SUCURSAL, POR_TIPO, DETALLE o POR_CAJA con {@code codCaja}).
     * Sin Content-Disposition: el nombre del archivo lo pone la app. Sin filas responde 400 con el motivo.
     */
    @PostMapping("/reporte")
    public ResponseEntity<byte[]> reporte(@RequestBody FiltroReporteArchivo filtro, Authentication auth) {
        pantallas.exigir(auth, PANTALLA);
        byte[] pdf = reportes.generar(filtro, DatosToken.de(auth).getNombreCompleto());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentLength(pdf.length);
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    // ------------------------------------------------------------------ empresas (tb_empresa, solo lectura)

    @PostMapping("/empresas")
    public ResponseEntity<ApiResponse<?>> listarEmpresas(Authentication auth) {
        pantallas.exigir(auth, PANTALLA);
        return listado(empresas.obtenerEmpresas(), "No hay empresas registradas.");
    }

    // ------------------------------------------------------------------ apoyo

    private static ResponseEntity<ApiResponse<?>> listado(List<?> filas, String vacio) {
        if (filas == null || filas.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(new ApiResponse<>(vacio, null, HttpStatus.NO_CONTENT.value()));
        }
        return ResponseEntity.ok(new ApiResponse<>(EXITO, filas, HttpStatus.OK.value()));
    }

    /** SpHelper ya lanza SpBusinessException (400) si error != 0; aqui solo llega el exito. */
    private static ResponseEntity<ApiResponse<?>> escritura(RespuestaSp res) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(res.getErrormsg(), res.getIdGenerado(), HttpStatus.CREATED.value()));
    }

    /** Un id que llega como parte de texto de un multipart: solo digitos y mayor que cero. */
    static long idDeTexto(String texto, String siFalta) {
        String t = texto == null ? "" : texto.trim();
        if (!t.matches("\\d{1,18}") || Long.parseLong(t) <= 0) throw new SpBusinessException(siFalta);
        return Long.parseLong(t);
    }
}
