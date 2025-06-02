package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.dao.*;
import bo.bosque.com.impexpap.model.*;
import bo.bosque.com.impexpap.utils.ApiResponse;
import bo.bosque.com.impexpap.utils.Utiles;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@CrossOrigin(origins = "*", methods = {RequestMethod.POST, RequestMethod.GET})
@RequestMapping("/prestamo-coches")
public class PrestamoCochesController {


    private static final String SUCCESS_MESSAGE = "Operación realizada exitosamente";
    private static final String ERROR_MESSAGE = "Error en la solicitud";


    private final ISolicitudChofer solicitudChoferDao;
    private final IEstadoChofer estadoChoferDao;
    private final IPrestamoChofer prestamoChoferDao;
    private final IPrestamoEstado prestamoEstadoDao;
    private final ITipoSolicitud tipoSolicitudDao;


    public PrestamoCochesController(ISolicitudChofer solicitudChoferDao, IEstadoChofer estadoChoferDao, IPrestamoChofer prestamoChoferDao, IPrestamoEstado prestamoEstadoDao, ITipoSolicitud tipoSolicitudDao) {

        this.solicitudChoferDao = solicitudChoferDao;
        this.estadoChoferDao = estadoChoferDao;
        this.prestamoChoferDao = prestamoChoferDao;
        this.prestamoEstadoDao = prestamoEstadoDao;
        this.tipoSolicitudDao = tipoSolicitudDao;
    }

    /**
     * Registrar las facturas
     * @param mb
     * @return
     */
    @PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
    @PostMapping("/registroSolicitud")
    public ResponseEntity<?> registrarSolicitud( @RequestBody SolicitudChofer mb ) {

        System.out.println(mb.toString());

        try {

            String acc = "U";
            if( mb.getIdSolicitud() == 0){
                acc = "I";
            }

            boolean operationSuccess = this.solicitudChoferDao.registrarSolicitudChofer( mb, acc );

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
     * Obtiene las las solicitudes por empleado
     * @param mb
     * @return
     */
    @PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
    @PostMapping("/solicitudes")
    public ResponseEntity<ApiResponse<?>> obtenerSolicitudesPorEmpleado( @RequestBody SolicitudChofer mb ){


        try {
            List<SolicitudChofer> lstTemp = solicitudChoferDao.lstSolicitudesXEmpleado(mb.getCodEmpSoli());

            if (lstTemp.isEmpty()) {
                return buildSuccessResponse(HttpStatus.NO_CONTENT, "No se encontraron solicitudes");
            }

            return ResponseEntity.ok()
                    .body(new ApiResponse<>(SUCCESS_MESSAGE, lstTemp, HttpStatus.OK.value()));

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }

    }

    /**
     * Obtiene la lista de coches para el préstamo
     * @return
     */
    @PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
    @PostMapping("/coches")
    public ResponseEntity<ApiResponse<?>> obtenerCoche() {
        try {
            List<SolicitudChofer> lstTemp = solicitudChoferDao.lstCoches();

            if (lstTemp.isEmpty()) {
                return buildSuccessResponse(HttpStatus.NO_CONTENT, "No se encontraron coches disponibles");
            }

            return ResponseEntity.ok()
                    .body(new ApiResponse<>(SUCCESS_MESSAGE, lstTemp, HttpStatus.OK.value()));

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * Obtiene los estados de los coches
     * @return
     */
    @PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
    @PostMapping("/estados")
    public ResponseEntity<ApiResponse<?>> obtenerEstados() {
        try {
            List<EstadoChofer> lstTemp = estadoChoferDao.listarEstadoChofer();

            if (lstTemp.isEmpty()) {
                return buildSuccessResponse(HttpStatus.NO_CONTENT, "No se encontraron estados");
            }

            return ResponseEntity.ok()
                    .body(new ApiResponse<>(SUCCESS_MESSAGE, lstTemp, HttpStatus.OK.value()));

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }


    /**
     * Para registrar los préstamos de los coches
     * @param mb
     * @return
     */
    @PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
    @PostMapping("/registroPrestamo")
    public ResponseEntity<ApiResponse<?>> registrarPrestamo(@RequestBody PrestamoChofer mb) {

        try {
            PrestamoEstado prestamoEstado = new PrestamoEstado();

            String acc = "A";
            if (mb.getIdPrestamo() == 0) {
                acc = "I";
            }

            if (mb.getIdPrestamo() == 0) {
                mb.setEstadoLateralesEntrega(1); //Key 1
                mb.setEstadoInteriorEntrega(2);
                mb.setEstadoDelanteraEntrega(3);
                mb.setEstadoTraseraEntrega(4);
                mb.setEstadoCapoteEntrega(5);
            } else {
                mb.setEstadoLateralRecepcion(1); //Key 1
                mb.setEstadoInteriorRecepcion(2);
                mb.setEstadoDelanteraRecepcion(3);
                mb.setEstadoTraseraRecepcion(4);
                mb.setEstadoCapoteRecepcion(5);
            }

            if (!prestamoChoferDao.registrarPrestamoChofer(mb, acc)) {
                return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Error al Registrar El Registro de Prestamo de Chofer");
            }

            if (mb.getIdPrestamo() == 0) {
                List<Integer> numeros = Utiles.extraerNumeros(mb.getEstadoLateralesEntregaAux());

                for (int numero : numeros) {
                    prestamoEstado.setIdPE(mb.getEstadoLateralesEntrega());
                    prestamoEstado.setIdEst(numero);
                    prestamoEstado.setAudUsuario(mb.getAudUsuario());

                    this.prestamoEstadoDao.registrarPrestamoEstado(prestamoEstado, "A");
                }

                numeros = Utiles.extraerNumeros(mb.getEstadoInteriorEntregaAux());

                for (int numero : numeros) {
                    prestamoEstado.setIdPE(mb.getEstadoInteriorEntrega());
                    prestamoEstado.setIdEst(numero);
                    prestamoEstado.setAudUsuario(mb.getAudUsuario());

                    this.prestamoEstadoDao.registrarPrestamoEstado(prestamoEstado, "A");
                }

                numeros = Utiles.extraerNumeros(mb.getEstadoDelanteraEntregaAux());

                for (int numero : numeros) {
                    prestamoEstado.setIdPE(mb.getEstadoDelanteraEntrega());
                    prestamoEstado.setIdEst(numero);
                    prestamoEstado.setAudUsuario(mb.getAudUsuario());

                    this.prestamoEstadoDao.registrarPrestamoEstado(prestamoEstado, "A");
                }

                numeros = Utiles.extraerNumeros(mb.getEstadoTraseraEntregaAux());

                for (int numero : numeros) {
                    prestamoEstado.setIdPE(mb.getEstadoTraseraEntrega());
                    prestamoEstado.setIdEst(numero);
                    prestamoEstado.setAudUsuario(mb.getAudUsuario());

                    this.prestamoEstadoDao.registrarPrestamoEstado(prestamoEstado, "A");
                }

                numeros = Utiles.extraerNumeros(mb.getEstadoCapoteEntregaAux());

                for (int numero : numeros) {
                    prestamoEstado.setIdPE(mb.getEstadoCapoteEntrega());
                    prestamoEstado.setIdEst(numero);
                    prestamoEstado.setAudUsuario(mb.getAudUsuario());

                    this.prestamoEstadoDao.registrarPrestamoEstado(prestamoEstado, "A");
                }

            } else {

                List<Integer> numeros = Utiles.extraerNumeros(mb.getEstadoLateralRecepcionAux());

                for (int numero : numeros) {
                    prestamoEstado.setIdPE(mb.getEstadoLateralRecepcion());
                    prestamoEstado.setIdPrestamo(mb.getIdPrestamo());
                    prestamoEstado.setIdEst(numero);
                    prestamoEstado.setAudUsuario(mb.getAudUsuario());

                    this.prestamoEstadoDao.registrarPrestamoEstado(prestamoEstado, "B");
                }

                numeros = Utiles.extraerNumeros(mb.getEstadoInteriorRecepcionAux());

                for (int numero : numeros) {
                    prestamoEstado.setIdPE(mb.getEstadoInteriorRecepcion());
                    prestamoEstado.setIdPrestamo(mb.getIdPrestamo());
                    prestamoEstado.setIdEst(numero);
                    prestamoEstado.setAudUsuario(mb.getAudUsuario());

                    this.prestamoEstadoDao.registrarPrestamoEstado(prestamoEstado, "B");
                }

                numeros = Utiles.extraerNumeros(mb.getEstadoDelanteraRecepcionAux());

                for (int numero : numeros) {
                    prestamoEstado.setIdPE(mb.getEstadoDelanteraRecepcion());
                    prestamoEstado.setIdPrestamo(mb.getIdPrestamo());
                    prestamoEstado.setIdEst(numero);
                    prestamoEstado.setAudUsuario(mb.getAudUsuario());

                    this.prestamoEstadoDao.registrarPrestamoEstado(prestamoEstado, "B");
                }

                numeros = Utiles.extraerNumeros(mb.getEstadoTraseraRecepcionAux());

                for (int numero : numeros) {
                    prestamoEstado.setIdPE(mb.getEstadoTraseraRecepcion());
                    prestamoEstado.setIdPrestamo(mb.getIdPrestamo());
                    prestamoEstado.setIdEst(numero);
                    prestamoEstado.setAudUsuario(mb.getAudUsuario());

                    this.prestamoEstadoDao.registrarPrestamoEstado(prestamoEstado, "B");
                }

                numeros = Utiles.extraerNumeros(mb.getEstadoCapoteRecepcionAux());

                for (int numero : numeros) {
                    prestamoEstado.setIdPE(mb.getEstadoCapoteRecepcion());
                    prestamoEstado.setIdPrestamo(mb.getIdPrestamo());
                    prestamoEstado.setIdEst(numero);
                    prestamoEstado.setAudUsuario(mb.getAudUsuario());

                    this.prestamoEstadoDao.registrarPrestamoEstado(prestamoEstado, "B");
                }
            }

            HttpStatus status = acc.equals("I") ? HttpStatus.CREATED : HttpStatus.OK;
            return buildSuccessResponse(status, SUCCESS_MESSAGE);

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }


    /**
     * Obtiene las solicitudes de préstamo por sucursal
     * @param mb
     * @return
     */
    @PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
    @PostMapping("/solicitudesPrestamo")
    public ResponseEntity<ApiResponse<?>> obtenerSolicitudesPrestamo(@RequestBody PrestamoChofer mb) {
        try {
            List<PrestamoChofer> lstTemp = prestamoChoferDao.lstSolicitudes( mb.getCodSucursal(), mb.getCodEmpEntregadoPor());

            if (lstTemp.isEmpty()) {
                return buildSuccessResponse(HttpStatus.NO_CONTENT, "No se encontraron solicitudes de préstamo");
            }

            return ResponseEntity.ok()
                    .body(new ApiResponse<>(SUCCESS_MESSAGE, lstTemp, HttpStatus.OK.value()));

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }


    /**
     * Para actualizar el estado de la solicitud de préstamo
     * @param mb
     * @return
     */
    @PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
    @PostMapping("/actualizarSolicitud")
    public ResponseEntity<ApiResponse<?>> actualizarSolicitud(@RequestBody SolicitudChofer mb) {
        try {
            boolean operationSuccess = solicitudChoferDao.registrarSolicitudChofer(mb, "A");

            if (!operationSuccess) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, "Error al actualizar el estado de la solicitud");
            }

            return buildSuccessResponse(HttpStatus.OK, SUCCESS_MESSAGE);

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * Para obtener los tipos de solicitud
     * @return
     */
    @PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
    @PostMapping("/tipoSolicitudes")
    public ResponseEntity<ApiResponse<?>> obtenerTipoSolicitud() {
        try {
            List<TipoSolicitud> lstTemp = tipoSolicitudDao.obtenerTipoSolicitudes();

            if (lstTemp.isEmpty()) {
                return buildSuccessResponse(HttpStatus.NO_CONTENT, "No se encontraron tipos de solicitud");
            }

            return ResponseEntity.ok()
                    .body(new ApiResponse<>(SUCCESS_MESSAGE, lstTemp, HttpStatus.OK.value()));

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
