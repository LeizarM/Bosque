package bo.bosque.com.impexpap.controller;


import bo.bosque.com.impexpap.dao.ICombustibleControl;
import bo.bosque.com.impexpap.model.CombustibleControl;
import bo.bosque.com.impexpap.model.ControCombustibleMaquinaMontacarga;
import bo.bosque.com.impexpap.model.Empresa;
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
@RequestMapping("/gasolina")
@CrossOrigin(origins = "*", methods = {RequestMethod.POST, RequestMethod.GET})
public class GasolinaController {

    private static final String SUCCESS_MESSAGE = "Operación realizada exitosamente";
    private static final String ERROR_MESSAGE = "Error en la solicitud";


    private final ICombustibleControl combustibleControlDao;

    public GasolinaController( ICombustibleControl combustibleControlDao ) {
        this.combustibleControlDao = combustibleControlDao;
    }


    /**
     * Para el registro de una nueva Nota de Remision
     * @param mb
     * @return
     */
    @PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
    @PostMapping("/registrar-gasolina")
    public ResponseEntity<?> registrarGasolina(@RequestBody CombustibleControl mb) {
        try {

            // Manejar otros campos que podrían ser null
            if (mb.getFecha() != null) {
                mb.setFecha(new Utiles().fechaJ_a_Sql(mb.getFecha()));
            }

            String acc = mb.getIdC() == 0 ? "I" : "U";

            boolean operationSuccess = this.combustibleControlDao.registrarCombustibleControl(mb, acc);

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
     * Obtener los coches registrados
     * @return
     */
    @PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
    @PostMapping("/lst-coches")
    public ResponseEntity<?> obtenerCoches() {
        try {
            List<CombustibleControl> coches = combustibleControlDao.listarCoches();

            if (coches.isEmpty()) {
                return buildSuccessResponse(HttpStatus.NO_CONTENT, "No se encontraron los coches registrados.");
            }

            return ResponseEntity.ok()
                    .body(new ApiResponse<>(SUCCESS_MESSAGE, coches, HttpStatus.OK.value()));

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }


    /**
     * Obtener los registros por coches
     * @return
     */
    @PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
    @PostMapping("/lst-kilometraje")
    public ResponseEntity<?> obtenerKilometrajesXCoche( @RequestBody CombustibleControl mb ) {
        try {
            List<CombustibleControl> coches = combustibleControlDao.listarCochesKilometraje( mb.getIdCoche() );

            if (coches.isEmpty()) {
                return buildSuccessResponse(HttpStatus.NO_CONTENT, "No se encontraron resultados.");
            }

            return ResponseEntity.ok()
                    .body(new ApiResponse<>(SUCCESS_MESSAGE, coches, HttpStatus.OK.value()));

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * Obtener el consumo de gasolina por coche de acuerdo a su kilometraje
     * @param mb
     * @return
     */
    @PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
    @PostMapping("/obtenerConsumo")
    public ResponseEntity<?> listConsumo( @RequestBody CombustibleControl mb ) {



        try {
            List<CombustibleControl> temp = this.combustibleControlDao.esConsumoBajo( mb.getKilometraje(), mb.getIdCoche() ); //Listado de vehiculos, bidones, montacargas

            if (temp.isEmpty()) {
                return buildSuccessResponse(HttpStatus.NO_CONTENT, "No se encontro el consumo por coche.");
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
