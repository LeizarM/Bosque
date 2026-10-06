package bo.bosque.com.impexpap.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import bo.bosque.com.impexpap.commons.ClientesSapService;
import bo.bosque.com.impexpap.commons.MensajesClientesSap;
import bo.bosque.com.impexpap.utils.ApiResponse;

/**
 * "ACTUALIZAR DATOS SAP" de la pantalla de Cheques. Controlador aparte de {@link ChChequeController} (que fija
 * sus rutas contra {@code API_CHEQUES.md}); comparte el prefijo {@code /cheque}. Todas las reglas viven en
 * {@link ClientesSapService}.
 */
@RestController
@CrossOrigin(origins = "*", methods = {RequestMethod.POST, RequestMethod.GET})
@RequestMapping("/cheque/clientes")
@PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
public class ChClientesSapController {

    private final ClientesSapService servicio;

    public ChClientesSapController(ClientesSapService servicio) {
        this.servicio = servicio;
    }

    /**
     * Trae los clientes nuevos de SAP (de todas las empresas) y actualiza los ya cargados. Sin cuerpo. Boton
     * {@code btnNuevoCH}. Responde 200 con {@code data} nulo (el procedimiento no dice cuantos clientes trajo, asi que no hay
     * numero que dar) y {@code message} con la frase para el usuario.
     */
    @PostMapping("/actualizar-sap")
    public ResponseEntity<ApiResponse<?>> actualizarDesdeSap(Authentication auth) {
        servicio.actualizarDesdeSap(auth);
        return ResponseEntity.ok(new ApiResponse<>(MensajesClientesSap.ACTUALIZADO, null, HttpStatus.OK.value()));
    }
}
