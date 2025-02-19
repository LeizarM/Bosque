package bo.bosque.com.impexpap.controller;


import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

import bo.bosque.com.impexpap.commons.service.FileStorageService;
import bo.bosque.com.impexpap.commons.service.PdfGeneratorService;
import bo.bosque.com.impexpap.dao.IChBanco;
import bo.bosque.com.impexpap.dao.IEmpresa;
import bo.bosque.com.impexpap.dao.ISocionegocio;
import bo.bosque.com.impexpap.model.ChBanco;
import bo.bosque.com.impexpap.model.Empresa;
import bo.bosque.com.impexpap.model.SocioNegocio;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import bo.bosque.com.impexpap.dao.IDepositoCheque;
import bo.bosque.com.impexpap.model.DepositoCheque;
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
    private final IChBanco chBancoDao;
    private final FileStorageService fileStorageService;
    private final PdfGeneratorService pdfGeneratorService;

    public DepositoChequeController(IDepositoCheque depositoChequeDao, IEmpresa empresaDao, ISocionegocio socioNegocioDao, IChBanco chBancoDao, FileStorageService fileStorageService, PdfGeneratorService pdfGeneratorService) {
        this.depositoChequeDao = depositoChequeDao;
        this.empresaDao = empresaDao;
        this.socioNegocioDao = socioNegocioDao;
        this.chBancoDao = chBancoDao;
        this.fileStorageService = fileStorageService;
        this.pdfGeneratorService = pdfGeneratorService;
    }


    /**
     * Para el registro de un nuevo deposito

     */
    @PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
    @PostMapping("/registro")
    public ResponseEntity<ApiResponse<?>> registrarDepositoCheque( @RequestParam("file") MultipartFile file, @RequestParam("depositoCheque") String depositoChequeJson) {

        try {
            ObjectMapper mapper = new ObjectMapper();
            DepositoCheque mb = mapper.readValue(depositoChequeJson, DepositoCheque.class);

            System.out.println(mb.toString());

            int existe = depositoChequeDao.existeRegistro( mb );

            if(existe > 0){
                // Registrar el depósito
                String accion = mb.getIdDeposito() == 0 ? "I" : "U";
                boolean operationSuccess = depositoChequeDao.registrarDepositoCheque(mb, accion);

                if (!operationSuccess) {
                    return buildErrorResponse(HttpStatus.BAD_REQUEST, ERROR_MESSAGE);
                }

                // Obtener el ID del último registro y guardar la imagen
                int lastId = depositoChequeDao.obtenerUltimoId( mb.getAudUsuario() );

                String fileName = fileStorageService.saveFile(file, (long) lastId);

                // Actualizar el registro con el nombre del archivo
                mb.setFotoPath(fileName);


                HttpStatus status = accion.equals("I") ? HttpStatus.CREATED : HttpStatus.OK;
                return buildSuccessResponse(status, SUCCESS_MESSAGE);
            }else{

                return buildErrorResponse(HttpStatus.CONFLICT, "No existe el registro en el SAP para esa empresa, verifique los datos como: Empresa, Codigo Cliente, Num Documento y Num. Factura");
            }



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
    public ResponseEntity<?> obtenerBanco() {
        try {
            List<ChBanco> bancos = chBancoDao.listBancos();

            if (bancos.isEmpty()) {
                return buildSuccessResponse(HttpStatus.NO_CONTENT, "No se encontraron empresas");
            }

            return ResponseEntity.ok()
                    .body(new ApiResponse<>(SUCCESS_MESSAGE, bancos, HttpStatus.OK.value()));

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
    public ResponseEntity<ApiResponse<?>> listarDepositos() {
        try {
            List<DepositoCheque> depositos = depositoChequeDao.listarDepositosCheque();

            if (depositos.isEmpty()) {
                return buildSuccessResponse(HttpStatus.NO_CONTENT, "No se encontraron depósitos");
            }

            return ResponseEntity.ok()
                    .body(new ApiResponse<>(SUCCESS_MESSAGE, depositos, HttpStatus.OK.value()));

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
    @PostMapping("/listar-reconciliados")
    public ResponseEntity<ApiResponse<?>> listarDepositosReconciliados( @RequestBody DepositoCheque mb)  {

        System.out.println(mb.toString());

        try {
            List<DepositoCheque> depositos = depositoChequeDao.listarDepositosChequeReconciliado( mb.getDocNum(), mb.getNumFact() );

            if (depositos.isEmpty()) {
                return buildSuccessResponse(HttpStatus.NO_CONTENT, "No se encontraron depósitos reconciliados");
            }

            return ResponseEntity.ok()
                    .body(new ApiResponse<>(SUCCESS_MESSAGE, depositos, HttpStatus.OK.value()));

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

            System.out.println(resource);

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