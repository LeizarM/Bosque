package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.dao.IDiaNoLaborableAdmin;
import bo.bosque.com.impexpap.model.DiaNoLaborableAdmin;
import bo.bosque.com.impexpap.utils.ApiResponse;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para el ABM de Dias No Laborables (feriados internos, no SAP).
 * <p>
 * Reemplaza al modulo JSF/PrimeFaces legacy ({@code tbDiaNoLaborable}). Un Dia No
 * Laborable puede ser GLOBAL (aplica a toda la empresa) o restringido a un conjunto
 * de sucursales; esa distincion se resuelve enteramente dentro del SP
 * {@code p_abm_rrhh_DiaNoLaborable}, que hace el ABM de cabecera + sucursales en
 * una unica transaccion (a diferencia del legacy, que hacia N llamadas separadas
 * desde Java).
 * <p>
 * <b>Convencion de SPs:</b>
 * <ul>
 *   <li>ABM: {@code p_abm_rrhh_DiaNoLaborable} — ACCION: "I"=Insert, "U"=Update, "D"=Delete</li>
 *   <li>Listado: {@code p_list_rrhh_DiaNoLaborable} — ACCION "L" (grilla por gestion/id)</li>
 *   <li>Matriz de sucursales: {@code p_list_rrhh_DiaNoLaborable_sucursal} — ACCION "S"</li>
 * </ul>
 * <p>
 * La lectura de feriados de UN empleado especifico para su calendario (SP
 * {@code p_list_DiaNoLaborable} ACCION 'F', via {@code FeriadoDao}) es un modulo
 * distinto y no se duplica aqui.
 */
@RestController
@CrossOrigin(origins = "*", methods = {RequestMethod.POST, RequestMethod.GET})
@RequestMapping("/dias-no-laborables")
@PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
public class DiaNoLaborableAdminController {

    private static final String SUCCESS_MESSAGE = "Operación realizada exitosamente";

    private final IDiaNoLaborableAdmin diaNoLaborableAdminDao;

    public DiaNoLaborableAdminController(IDiaNoLaborableAdmin diaNoLaborableAdminDao) {
        this.diaNoLaborableAdminDao = diaNoLaborableAdminDao;
    }

    /** Registra o actualiza un dia no laborable (cabecera + sucursales). Si idDiaNoLaborable == 0 → INSERT, si > 0 → UPDATE. */
    @PostMapping("/registrar-dia-no-laborable")
    public ResponseEntity<ApiResponse<?>> registrarDiaNoLaborable(@RequestBody DiaNoLaborableAdmin mb) {
        return respuestaEscritura(diaNoLaborableAdminDao.registrarDiaNoLaborable(mb, mb.getIdDiaNoLaborable() == 0 ? "I" : "U"));
    }

    /** Elimina un dia no laborable (cabecera + sucursales) por su ID. */
    @PostMapping("/eliminar-dia-no-laborable")
    public ResponseEntity<ApiResponse<?>> eliminarDiaNoLaborable(@RequestBody DiaNoLaborableAdmin mb) {
        return respuestaEscritura(diaNoLaborableAdminDao.registrarDiaNoLaborable(mb, "D"));
    }

    /** Obtiene la grilla de dias no laborables. idDiaNoLaborable/gestion en 0 → sin ese filtro. */
    @PostMapping("/obtener-dias-no-laborables")
    public ResponseEntity<ApiResponse<?>> obtenerDiasNoLaborables(@RequestBody DiaNoLaborableAdmin mb) {
        return procesarLista(
                diaNoLaborableAdminDao.obtenerDiasNoLaborables(mb.getIdDiaNoLaborable(), mb.getGestion()),
                "No se encontraron dias no laborables.");
    }

    /** Obtiene la matriz de sucursales (todas, marcando seleccionado) para el modal ABM. idDiaNoLaborable == 0 → registro nuevo, todas en 0. */
    @PostMapping("/obtener-sucursales-dia-no-laborable")
    public ResponseEntity<ApiResponse<?>> obtenerSucursalesDiaNoLaborable(@RequestBody DiaNoLaborableAdmin mb) {
        return procesarLista(
                diaNoLaborableAdminDao.obtenerSucursales(mb.getIdDiaNoLaborable()),
                "No se encontraron sucursales.");
    }

    // ==================== HELPERS ====================

    private ResponseEntity<ApiResponse<?>> respuestaEscritura(RespuestaSp res) {
        HttpStatus status = res.getError() == 0 ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(new ApiResponse<>(res.getErrormsg(), res.getIdGenerado(), status.value()));
    }

    private <T> ResponseEntity<ApiResponse<?>> procesarLista(List<T> lista, String mensajeVacio) {
        if (lista == null || lista.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body(new ApiResponse<>(mensajeVacio, null, HttpStatus.NO_CONTENT.value()));
        }
        return ResponseEntity.ok(new ApiResponse<>(SUCCESS_MESSAGE, lista, HttpStatus.OK.value()));
    }
}
