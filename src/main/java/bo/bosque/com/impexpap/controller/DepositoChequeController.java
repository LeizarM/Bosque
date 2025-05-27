package bo.bosque.com.impexpap.controller;


import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

import bo.bosque.com.impexpap.commons.service.FileStorageService;
import bo.bosque.com.impexpap.commons.service.PdfGeneratorService;
import bo.bosque.com.impexpap.dao.*;
import bo.bosque.com.impexpap.model.*;
import bo.bosque.com.impexpap.utils.Utiles;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import bo.bosque.com.impexpap.utils.ApiResponse;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/deposito-cheque")
@CrossOrigin(origins = "*", methods = {RequestMethod.POST, RequestMethod.GET})
public class DepositoChequeController {

    private static final String SUCCESS_MESSAGE = "Operación realizada exitosamente";
    private static final String ERROR_MESSAGE = "Error en la solicitud";


    private final IDepositoCheque depositoChequeDao;
    private final IEmpresa empresaDao;
    private final ISocionegocio socioNegocioDao;
    private final IBancoXCuenta bancoXCuentaDao;

    private final FileStorageService fileStorageService;
    private final PdfGeneratorService pdfGeneratorService;
    private final INotaRemision notaRemisionDao;

    public DepositoChequeController(IDepositoCheque depositoChequeDao, IEmpresa empresaDao, ISocionegocio socioNegocioDao, IBancoXCuenta bancoXCuentaDao, FileStorageService fileStorageService, PdfGeneratorService pdfGeneratorService, INotaRemision notaRemisionDao) {
        this.depositoChequeDao = depositoChequeDao;
        this.empresaDao = empresaDao;
        this.socioNegocioDao = socioNegocioDao;
        this.bancoXCuentaDao = bancoXCuentaDao;
        this.fileStorageService = fileStorageService;
        this.pdfGeneratorService = pdfGeneratorService;
        this.notaRemisionDao = notaRemisionDao;
    }


    @PostMapping("/registro")
    public ResponseEntity<ApiResponse<?>> registrarDepositoCheque( @RequestParam(value = "file", required = false) MultipartFile file, @RequestParam("depositoCheque") String depositoChequeJson) {
        try {
            ObjectMapper mapper = JsonMapper.builder()
                    .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
                    .build();
            DepositoCheque mb = mapper.readValue(depositoChequeJson, DepositoCheque.class);

            // Registrar el depósito
            String accion = mb.getIdDeposito() == 0 ? "I" : "U";
            boolean operationSuccess = depositoChequeDao.registrarDepositoCheque(mb, accion);

            if (!operationSuccess) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, ERROR_MESSAGE);
            }

            // Solo procesamos el archivo si no es nulo y no está vacío
            if (file != null && !file.isEmpty()) {
                try {
                    // Verificar que el contenido sea una imagen real
                    String contentType = file.getContentType();
                    if (contentType != null && contentType.startsWith("image/")) {
                        // Obtener el ID del último registro y guardar la imagen
                        int lastID;

                        if("I".equals(accion) ) {
                            mb.setIdDeposito(  depositoChequeDao.obtenerUltimoId(mb.getAudUsuario()));
                        }
                        lastID = mb.getIdDeposito();
                        String fileName = fileStorageService.saveFile(file, (long) lastID);
                        // Actualizar el registro con el nombre del archivo
                        mb.setFotoPath(fileName);
                    } else {
                        // No es una imagen o no tiene tipo de contenido
                        System.out.println("El archivo enviado no es una imagen válida: " +
                                (contentType != null ? contentType : "tipo desconocido"));
                    }
                } catch (Exception e) {
                    // Capturar cualquier error al procesar el archivo, pero continuar con la operación
                    System.err.println("Error al procesar el archivo: " + e.getMessage());
                    // No interrumpimos la operación principal si hay un error con el archivo
                }
            } else {
                System.out.println("No se recibió ningún archivo o el archivo está vacío");
                // Continuar con el proceso normal sin intentar guardar el archivo
            }

            HttpStatus status = accion.equals("I") ? HttpStatus.CREATED : HttpStatus.OK;
            return buildSuccessResponse(status, SUCCESS_MESSAGE);

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }


    /**
     * Obtiene todos las empresas registradas en el sistema.
     * @return
     */
    @PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
    @PostMapping("/lst-empresas")
    public ResponseEntity<?> obtenerEmpresas() {
        try {
            List<Empresa> empresas = empresaDao.obtenerEmpresas();

            if (empresas.isEmpty()) {
                return buildSuccessResponse(HttpStatus.NO_CONTENT, "No se encontraron empresas");
            }

            return ResponseEntity.ok()
                    .body(new ApiResponse<>(SUCCESS_MESSAGE, empresas, HttpStatus.OK.value()));

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * Obtiene todos los bancos registradas en el sistema.
     * @return
     */
    @PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
    @PostMapping("/lst-banco")
    public ResponseEntity<?> obtenerBanco( @RequestBody BancoXCuenta mb ) {
        try {
            List<BancoXCuenta> bancos = bancoXCuentaDao.listarBancosXCuentas( mb.getCodEmpresa() );

            if (bancos.isEmpty()) {
                return buildSuccessResponse(HttpStatus.NO_CONTENT, "No se encontraron Bancos");
            }

            return ResponseEntity.ok()
                    .body(new ApiResponse<>(SUCCESS_MESSAGE, bancos, HttpStatus.OK.value()));

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }



    @PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
    @PostMapping("/lst-notaRemision")
    public ResponseEntity<?> obtenerNotaRemision( @RequestBody NotaRemision mb ) {
        try {
            List<NotaRemision> nr = notaRemisionDao.listarNotasRemisiones( mb );

            if (nr.isEmpty()) {
                return buildSuccessResponse(HttpStatus.NO_CONTENT, "No se encontraron Notas de Remision");
            }

            return ResponseEntity.ok()
                    .body(new ApiResponse<>(SUCCESS_MESSAGE, nr, HttpStatus.OK.value()));

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * Para el registro de una nueva Nota de Remision
     * @param mb
     * @return
     */
    @PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
    @PostMapping("/registrar-nota-remision")
    public ResponseEntity<?> registrarNotaRemision( @RequestBody NotaRemision mb ){


        mb.setFecha( new Utiles().fechaJ_a_Sql( mb.getFecha() ));


        try {

            String acc = mb.getIdNR() == 0 ? "I" : "U";

            boolean operationSuccess = this.notaRemisionDao.registrarNotaRemision( mb, acc );

            if (!operationSuccess) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, ERROR_MESSAGE);
            }

            HttpStatus status = HttpStatus.CREATED;
            return buildSuccessResponse(status, SUCCESS_MESSAGE);

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }




    /**
     * Obtiene los socios de negocio registrados en el sistema por empresa
     * @return
     */
    @PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
    @PostMapping("/lst-socios-negocio")

    public ResponseEntity<?> obtenerSocioNegocio( @RequestBody DepositoCheque mb) {
        try {
            List<SocioNegocio> lstTemp = socioNegocioDao.obtenerSocioNegocio( mb.getCodEmpresa() );

            if (lstTemp.isEmpty()) {
                return buildSuccessResponse(HttpStatus.NO_CONTENT, "No se encontraron socios de negocio");
            }

            return ResponseEntity.ok()
                    .body(new ApiResponse<>(SUCCESS_MESSAGE, lstTemp, HttpStatus.OK.value()));

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }


    @PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
    @PostMapping("/listar")
    public ResponseEntity<ApiResponse<?>> listarDepositos( @RequestBody DepositoCheque mb ) {



        mb.setFechaInicio( new Utiles().fechaJ_a_Sql( mb.getFechaInicio() ) );
        mb.setFechaFin( new Utiles().fechaJ_a_Sql( mb.getFechaFin() ) );



        try {
            List<DepositoCheque> depositos = depositoChequeDao.listarDepositosChequeReconciliado( mb.getCodEmpresa(),  mb.getIdBxC(), mb.getFechaInicio(), mb.getFechaFin(), mb.getCodCliente(), mb.getEstadoFiltro() );

            if (depositos.isEmpty()) {
                return buildSuccessResponse(HttpStatus.NO_CONTENT, "No se encontraron registros...");
            }

            return ResponseEntity.ok()
                    .body(new ApiResponse<>(SUCCESS_MESSAGE, depositos, HttpStatus.OK.value()));

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * Para listar los depositos para identificar
     * @param mb
     * @return
     */
    @PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
    @PostMapping("/listar-dep-identificar")
    public ResponseEntity<ApiResponse<?>> listarDepositosPorIdentificar( @RequestBody DepositoCheque mb ) {



        mb.setFechaInicio( new Utiles().fechaJ_a_Sql( mb.getFechaInicio() ) );
        mb.setFechaFin( new Utiles().fechaJ_a_Sql( mb.getFechaFin() ) );



        try {
            List<DepositoCheque> depositos = depositoChequeDao.lstDepositxIdentificar( mb.getIdBxC(), mb.getFechaInicio(), mb.getFechaFin(), mb.getCodCliente() );

            if (depositos.isEmpty()) {
                return buildSuccessResponse(HttpStatus.NO_CONTENT, "No se encontraron registros...");
            }

            return ResponseEntity.ok()
                    .body(new ApiResponse<>(SUCCESS_MESSAGE, depositos, HttpStatus.OK.value()));

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }





    /**
     * Para el registro de una nueva Nota de Remision
     * @param mb
     * @return
     */
    @PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
    @PostMapping("/registrar-nroTransaccion")
    public ResponseEntity<?> registrarNroTransaccion( @RequestBody DepositoCheque mb ){

        try {

            String acc = "A";

            boolean operationSuccess = this.depositoChequeDao.registrarDepositoCheque( mb, acc );

            if (!operationSuccess) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, ERROR_MESSAGE);
            }

            HttpStatus status = HttpStatus.CREATED;
            return buildSuccessResponse(status, SUCCESS_MESSAGE);

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }


    /**
     * Para el registro de una nueva Nota de Remision
     * @param mb
     * @return
     */
    @PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
    @PostMapping("/rechazar-deposito")
    public ResponseEntity<?> rechazarDeposito( @RequestBody DepositoCheque mb ){

        try {

            String acc = "B";

            boolean operationSuccess = this.depositoChequeDao.registrarDepositoCheque( mb, acc );

            if (!operationSuccess) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, ERROR_MESSAGE);
            }

            HttpStatus status = HttpStatus.CREATED;
            return buildSuccessResponse(status, SUCCESS_MESSAGE);

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }


    /**
     *
     * Obtiene un solo registro de depósito por ID
     * @param idDeposito
     * @return
     */
    @PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
    @GetMapping("/descargar/{idDeposito}")
    public ResponseEntity<?> descargarImagen(@PathVariable int idDeposito) {


        try {
            Resource resource = fileStorageService.obtenerArchivo(idDeposito);

            // Determinar el tipo de contenido
            String contentType = null;
            contentType = Files.probeContentType(Paths.get(resource.getFile().getAbsolutePath()));

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + resource.getFilename() + "\"")
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .body(resource);

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @PostMapping("/pdf/{idDeposito}")
    @PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
    public ResponseEntity<?> generarPdf(
            @PathVariable int idDeposito,
            @RequestBody DepositoCheque deposito) {
        try {
            byte[] pdfBytes = pdfGeneratorService.generarPdfDeposito(deposito);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "application/pdf")
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"deposito_" + idDeposito + ".pdf\"")
                    .body(pdfBytes);

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }







    private ResponseEntity<ApiResponse<?>> buildErrorResponse(BindingResult result) {
        String errorMsg = Objects.requireNonNull(result.getFieldError()).getDefaultMessage();
        return ResponseEntity.badRequest()
                .body(new ApiResponse<>(ERROR_MESSAGE, errorMsg, HttpStatus.BAD_REQUEST.value()));
    }

    private ResponseEntity<ApiResponse<?>> buildErrorResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(new ApiResponse<>(message, null, status.value()));
    }

    private ResponseEntity<ApiResponse<?>> buildSuccessResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(new ApiResponse<>(message, null, status.value()));
    }
}