package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.commons.TcpeCatalogo;
import bo.bosque.com.impexpap.commons.TcpePermisos;
import bo.bosque.com.impexpap.utils.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Validator;
import java.util.Map;

/**
 * Catálogo de convocatorias, postulaciones y capacitaciones: las 22 tablas {@code tcpe_} restantes (hoy casi todas
 * vacías; existen por la regla N:N). Ver {@link TcpeCatalogoBase}.
 */
@RestController
@CrossOrigin(originPatterns = "${tcpe.cors.origenes:*}", methods = {RequestMethod.POST})
@RequestMapping("/examenes/catalogo")
@PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
public class TcpeCatalogoPostulacionController extends TcpeCatalogoBase {

    static final String ENTIDADES = "areaPost|capacitacion|capacitador|conocimientosPost|conocimientos"
            + "|convocatoria|curso|dpto|experienciaLab|fases|formacion|historialPost|inscripcion"
            + "|otrosConocimientos|personaPost|postulacion|reqAcademico|reqAcademicoPost|reqExpLabPost"
            + "|seleccionFase|temario|tipo";

    public TcpeCatalogoPostulacionController(TcpeCatalogo catalogo, TcpePermisos permisos, ObjectMapper mapper,
                                             Validator validator) {
        super(catalogo, permisos, mapper, validator);
    }

    @PostMapping("/{entidad:" + ENTIDADES + "}/listar")
    public ResponseEntity<ApiResponse<?>> listar(@PathVariable String entidad,
                                                 @RequestBody(required = false) Map<String, Object> cuerpo,
                                                 Authentication auth) {
        return super.listar(entidad, cuerpo, auth);
    }

    @PostMapping("/{entidad:" + ENTIDADES + "}/guardar")
    public ResponseEntity<ApiResponse<?>> guardar(@PathVariable String entidad,
                                                  @RequestBody(required = false) Map<String, Object> cuerpo,
                                                  Authentication auth) {
        return super.guardar(entidad, cuerpo, auth);
    }

    @PostMapping("/{entidad:" + ENTIDADES + "}/eliminar")
    public ResponseEntity<ApiResponse<?>> eliminar(@PathVariable String entidad,
                                                   @RequestBody(required = false) Map<String, Object> cuerpo,
                                                   Authentication auth) {
        return super.eliminar(entidad, cuerpo, auth);
    }
}
