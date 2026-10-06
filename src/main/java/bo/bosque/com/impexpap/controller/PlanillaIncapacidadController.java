package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.dao.IPlanillaIncapacidad;
import bo.bosque.com.impexpap.model.PlanillaIncapacidad;
import bo.bosque.com.impexpap.security.jwt.DatosToken;
import bo.bosque.com.impexpap.utils.ApiResponse;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Revisión de la Planilla de Incapacidad (vista 166 'tacTareas/PlanillaIncapacidad').
 * audUsuario sale del token, no del cuerpo.
 */
@RestController
@RequestMapping("/planillaIncapacidad")
@CrossOrigin(origins = "*", methods = {RequestMethod.POST})
@PreAuthorize("hasAnyRole('ROLE_ADM','ROLE_LIM')")
public class PlanillaIncapacidadController {

    private static final String SUCCESS_MESSAGE = "OPERACION REALIZADA EXITOSAMENTE";

    private final IPlanillaIncapacidad planillaIncapacidadDao;

    public PlanillaIncapacidadController(IPlanillaIncapacidad planillaIncapacidadDao) {
        this.planillaIncapacidadDao = planillaIncapacidadDao;
    }

    /** Bajas cuyo inicio cae en [desde, hasta]. Antes de listar, el SP carga las bajas nuevas. */
    @PostMapping("/listarPlanillaIncapacidad")
    public ResponseEntity<ApiResponse<?>> listarPlanillaIncapacidad(@RequestBody PlanillaIncapacidad filtro, Authentication auth) {
        if (filtro.getDesde() == null || filtro.getHasta() == null) {
            throw new SpBusinessException("Elige el rango de fechas.");
        }
        if (filtro.getDesde().after(filtro.getHasta())) {
            throw new SpBusinessException("La fecha inicial no puede ser posterior a la final.");
        }

        List<PlanillaIncapacidad> lista = planillaIncapacidadDao.listarRevision(
                filtro.getDesde(), filtro.getHasta(), DatosToken.codUsuarioDe(auth));
        if (lista.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(new ApiResponse<>("No hay bajas que empiecen en ese rango.", null, HttpStatus.NO_CONTENT.value()));
        }
        return ResponseEntity.ok(new ApiResponse<>(SUCCESS_MESSAGE, lista, HttpStatus.OK.value()));
    }

    /** Marca (1) o desmarca (0) una baja como revisada. */
    @PostMapping("/revisarPlanillaIncapacidad")
    public ResponseEntity<ApiResponse<?>> revisarPlanillaIncapacidad(@RequestBody PlanillaIncapacidad p, Authentication auth) {
        if (p.getIdPIT() == null || p.getIdPIT() <= 0) {
            throw new SpBusinessException("No se indicó qué baja revisar.");
        }
        if (p.getFueRevisado() == null || (p.getFueRevisado() != 0 && p.getFueRevisado() != 1)) {
            throw new SpBusinessException("Indica si la baja fue revisada o no.");
        }

        RespuestaSp res = planillaIncapacidadDao.marcarRevisado(
                p.getIdPIT(), p.getFueRevisado(), DatosToken.codUsuarioDe(auth));
        return ResponseEntity.ok(new ApiResponse<>(SUCCESS_MESSAGE, res.getIdGenerado(), HttpStatus.OK.value()));
    }
}
