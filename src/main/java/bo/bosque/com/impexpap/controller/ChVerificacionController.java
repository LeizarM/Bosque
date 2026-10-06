package bo.bosque.com.impexpap.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import bo.bosque.com.impexpap.commons.MensajesVerificacion;
import bo.bosque.com.impexpap.commons.VerificacionChequeService;
import bo.bosque.com.impexpap.dto.ChequePendienteFiltroDto;
import bo.bosque.com.impexpap.dto.FiltroIdDto;
import bo.bosque.com.impexpap.dto.VerificacionFiltroDto;
import bo.bosque.com.impexpap.dto.VerificacionRegistroDto;
import bo.bosque.com.impexpap.utils.ApiResponse;

/**
 * Verificar Cheques (vista 77, tabla {@code tch_verificacionDeposito}).
 *
 * <p>Reemplaza {@code tchCheque/verificarDepositos.xhtml} de Bosque v2 (bean {@code wVerificacionDeposito}). El contrato,
 * endpoint por endpoint, esta en la seccion "Verificar Cheques (vista 77)" de {@code API_CHEQUES.md} del espacio de
 * migracion.
 *
 * <p>Solo traduce HTTP: las reglas viven en {@link VerificacionChequeService}, que es lo que se prueba. Todo es
 * {@code POST} sobre {@code ApiResponse}, como el resto de la casa. Esta vista no tiene botones en {@code tb_vistaBtn}:
 * se exige JWT y rol ({@code ROLE_ADM} o {@code ROLE_LIM}) y nada mas, igual que el legacy. El usuario de auditoria
 * sale siempre del token, nunca del cuerpo.
 */
@RestController
@CrossOrigin(origins = "*", methods = {RequestMethod.POST, RequestMethod.GET})
@RequestMapping("/cheque/verificacion")
@PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
public class ChVerificacionController {

    private static final String SUCCESS_MESSAGE = "Operación realizada exitosamente";

    private final VerificacionChequeService servicio;

    public ChVerificacionController(VerificacionChequeService servicio) {
        this.servicio = servicio;
    }

    /** La lista principal: las verificaciones de un dia (o todas), una pagina. Siempre 200: sin filas, la lista va vacia. */
    @PostMapping("/listar")
    public ResponseEntity<ApiResponse<?>> listar(@RequestBody(required = false) VerificacionFiltroDto filtro) {
        return ok(servicio.listar(filtro));
    }

    /** El modal "Cheques Pendientes Sin Regularizar": los cheques sin verificacion valida, una pagina. */
    @PostMapping("/pendientes")
    public ResponseEntity<ApiResponse<?>> pendientes(@RequestBody(required = false) ChequePendienteFiltroDto filtro) {
        return ok(servicio.pendientes(filtro));
    }

    /** "Seleccionar": relee el cheque ({@code id}) y dice si todavia se puede verificar. */
    @PostMapping("/preparar")
    public ResponseEntity<ApiResponse<?>> preparar(@RequestBody FiltroIdDto filtro) {
        return ok(servicio.preparar(filtro == null ? null : filtro.getId()));
    }

    /** Alta (codvd 0/ausente) o edicion de una verificacion. {@code data} = codvd. */
    @PostMapping("/registrar")
    public ResponseEntity<ApiResponse<?>> registrar(@RequestBody VerificacionRegistroDto dto, Authentication auth) {
        long id = servicio.registrar(auth, dto);
        return creado(dto != null && dto.esAlta() ? "Verificación registrada." : "Verificación actualizada.", id);
    }

    /** "Cancelar": anula la verificacion ({@code id}), no la borra. {@code data} = codvd. */
    @PostMapping("/anular")
    public ResponseEntity<ApiResponse<?>> anular(@RequestBody FiltroIdDto filtro, Authentication auth) {
        long id = filtro == null ? 0 : filtro.getId();
        boolean anulada = servicio.anular(auth, id);
        return creado(anulada ? "Verificación anulada." : MensajesVerificacion.YA_ESTABA_ANULADA, id);
    }

    // ------------------------------------------------------------------ apoyo

    private ResponseEntity<ApiResponse<?>> ok(Object data) {
        return ResponseEntity.ok(new ApiResponse<>(SUCCESS_MESSAGE, data, HttpStatus.OK.value()));
    }

    private ResponseEntity<ApiResponse<?>> creado(String mensaje, long id) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(mensaje, id, HttpStatus.CREATED.value()));
    }
}
