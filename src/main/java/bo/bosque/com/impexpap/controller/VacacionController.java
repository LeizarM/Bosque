package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.dao.IPermiso;
import bo.bosque.com.impexpap.model.Permiso;
import bo.bosque.com.impexpap.utils.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin ("*")
@RequestMapping ("/vacacion")
public class VacacionController {
    private JdbcTemplate jdbcTemplate;

    private final IPermiso pDao;

    public VacacionController(JdbcTemplate jdbcTemplate
        ,IPermiso pDao){
        this.jdbcTemplate = jdbcTemplate;
        this.pDao = pDao;
    }
    private static final String SUCCESS_MESSAGE = "Operación realizada exitosamente";
    /**
     * OBTENER TOTAL DIAS DISPONIBLES DE VACACION EMPLEADO
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/diasDisponibles")
    public ResponseEntity<ApiResponse<?>> diasDisponibles(@RequestBody Permiso p) {
        return procesarListaCambios(pDao.diasDisponibles(p));
    }
    /**
     * Metodo auxiliar para procesar listas de cambios.
     * Devuelve 204 si no hay registros, 200 si hay.
     */
    private <T> ResponseEntity<ApiResponse<?>> procesarListaCambios(List<T> lista) {
        if (lista == null || lista.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(new ApiResponse<>("No se encontraron registros.", null,
                            HttpStatus.NO_CONTENT.value()));
        }
        return ResponseEntity.ok(new ApiResponse<>(SUCCESS_MESSAGE, lista, HttpStatus.OK.value()));
    }
}
