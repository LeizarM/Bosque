package bo.bosque.com.impexpap.controller;


import bo.bosque.com.impexpap.dao.IControCombustibleMaquinaMontacarga;

import bo.bosque.com.impexpap.dao.IMaquinaMontacarga;
import bo.bosque.com.impexpap.model.ControCombustibleMaquinaMontacarga;
import bo.bosque.com.impexpap.model.MaquinaMontacarga;
import bo.bosque.com.impexpap.utils.ApiResponse;
import bo.bosque.com.impexpap.utils.Utiles;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/gasolinaMaquina")
@CrossOrigin(origins = "*", methods = {RequestMethod.POST, RequestMethod.GET})
public class ControlCombustibleMaquinaMontacargaController {

    private static final String SUCCESS_MESSAGE = "Operación realizada exitosamente";
    private static final String ERROR_MESSAGE = "Error en la solicitud";

    private final IControCombustibleMaquinaMontacarga combustibleMaquinaMontacargaDao;
    private final IMaquinaMontacarga maquinaMontacargaDao;


    public ControlCombustibleMaquinaMontacargaController(IControCombustibleMaquinaMontacarga combustibleMaquinaMontacargaDao, IMaquinaMontacarga maquinaMontacargaDao) {
        this.combustibleMaquinaMontacargaDao = combustibleMaquinaMontacargaDao;
        this.maquinaMontacargaDao = maquinaMontacargaDao;
    }




    @PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
    @PostMapping("/registrarMaquina")
    public ResponseEntity<?> registrarGasolina(@RequestBody ControCombustibleMaquinaMontacarga mb) {
        try {

            // Manejar otros campos que podrían ser null
            if (mb.getFecha() != null) {
                mb.setFecha(new Utiles().fechaJ_a_Sql(mb.getFecha()));
            }

            String acc = mb.getIdCM() == 0 ? "I" : "U";

            boolean operationSuccess = this.combustibleMaquinaMontacargaDao.registrarControlCombustible(mb, acc);

            if (!operationSuccess) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, ERROR_MESSAGE);
            }

            HttpStatus status = HttpStatus.CREATED;
            return buildSuccessResponse(status, SUCCESS_MESSAGE);

        } catch (Exception e) {
            e.printStackTrace(); // Esto ayudará a identificar el error exacto
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }



    /**
     * Obtener los almacenes registrados
     * @return
     */
    @PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
    @PostMapping("/lst-almacenes")
    public ResponseEntity<?> obtenerAlmacenes() {
        try {
            List<ControCombustibleMaquinaMontacarga> ccmm = this.combustibleMaquinaMontacargaDao.lstAlmacenes();

            if (ccmm.isEmpty()) {
                return buildSuccessResponse(HttpStatus.NO_CONTENT, "No se encontraron los almacenes encontrados.");
            }

            return ResponseEntity.ok()
                    .body(new ApiResponse<>(SUCCESS_MESSAGE, ccmm, HttpStatus.OK.value()));

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }





    /**
     * Obtener los maquinas o montacargas registradas
     * @return
     */
    @PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
    @PostMapping("/lst-maqmontacarga")
    public ResponseEntity<?> obtenerMaquinaMontacarga() {
        try {
            List<MaquinaMontacarga> mm = this.maquinaMontacargaDao.listMaquinaMontacargas(); //Listado de vehiculos, bidones, montacargas

            if (mm.isEmpty()) {
                return buildSuccessResponse(HttpStatus.NO_CONTENT, "No se encontraron las maquinas o montacargas encontrados.");
            }

            return ResponseEntity.ok()
                    .body(new ApiResponse<>(SUCCESS_MESSAGE, mm, HttpStatus.OK.value()));

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }


    /**
     * Obtener los registros de bidones por tipo de transacción
     * @return
     */
    @PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
    @PostMapping("/lstMovBidones")
    public ResponseEntity<?> listMovimientosBidones( @RequestBody ControCombustibleMaquinaMontacarga mb  ) {
        try {
            List<ControCombustibleMaquinaMontacarga> temp = this.combustibleMaquinaMontacargaDao.lstRptMovBidonesXTipoTransaccion( mb.getFechaInicio(), mb.getFechaFin() ); //Listado de vehiculos, bidones, montacargas

            if (temp.isEmpty()) {
                return buildSuccessResponse(HttpStatus.NO_CONTENT, "No se encontraron el reporte de bidones entre fechas.");
            }

            return ResponseEntity.ok()
                    .body(new ApiResponse<>(SUCCESS_MESSAGE, temp, HttpStatus.OK.value()));

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * Obtener los ultimos movimientos de bidones ( ingresos )
     * @return
     */
    @PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
    @PostMapping("/lstUltimoMovBidones")
    public ResponseEntity<?> listSaldosBidonesUltimosMov( ) {
        try {
            List<ControCombustibleMaquinaMontacarga> temp = this.combustibleMaquinaMontacargaDao.lstUltimosMovBidones( ); //Listado de vehiculos, bidones, montacargas

            if (temp.isEmpty()) {
                return buildSuccessResponse(HttpStatus.NO_CONTENT, "No se encontraron el el reporte de bidones los ultimos movimientos.");
            }

            return ResponseEntity.ok().body(new ApiResponse<>(SUCCESS_MESSAGE, temp, HttpStatus.OK.value()));

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * Obtener los almacenes registrados
     * @return
     */
    @PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
    @PostMapping("/lstSaldosBidones")
    public ResponseEntity<?> listSaldosBidones( ) {
        try {
            List<ControCombustibleMaquinaMontacarga> temp = this.combustibleMaquinaMontacargaDao.saldoActualCombustinbleXSucursal( ); //Listado de vehiculos, bidones, montacargas

            if (temp.isEmpty()) {
                return buildSuccessResponse(HttpStatus.NO_CONTENT, "No se encontraron el reporte de saldo por litros por sucursales.");
            }

            return ResponseEntity.ok()
                    .body(new ApiResponse<>(SUCCESS_MESSAGE, temp, HttpStatus.OK.value()));

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
