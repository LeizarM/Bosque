package bo.bosque.com.impexpap.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import bo.bosque.com.impexpap.dao.IRelEmpEmpr;
import bo.bosque.com.impexpap.utils.ApiResponse;

/**
 * Agenda de empleados del Dashboard: nombre, cargo, sucursal y teléfonos corporativos de quienes
 * tienen relación laboral vigente. La ven todos los usuarios con sesión; la ficha completa sigue
 * dependiendo de la pantalla de empleados del menú.
 */
@RestController
@CrossOrigin(originPatterns = "${tcpe.cors.origenes:*}", methods = {RequestMethod.POST})
@RequestMapping("/agenda")
@PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
public class AgendaController {

    private final IRelEmpEmpr relaciones;

    public AgendaController(IRelEmpEmpr relaciones) {
        this.relaciones = relaciones;
    }

    @PostMapping("/empleados")
    public ResponseEntity<ApiResponse<?>> empleados() {
        return ParametrosRespuestas.listado(relaciones.agenda());
    }
}
