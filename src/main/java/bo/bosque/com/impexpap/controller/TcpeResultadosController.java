package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.commons.TcpePermisos;
import bo.bosque.com.impexpap.dao.ITcpeCalificacion;
import bo.bosque.com.impexpap.dao.ITcpeRendir;
import bo.bosque.com.impexpap.dao.ITcpeResultado;
import bo.bosque.com.impexpap.dto.TcpeCalificacionDto;
import bo.bosque.com.impexpap.dto.TcpeResultadoFiltroDto;
import bo.bosque.com.impexpap.security.jwt.DatosToken;
import bo.bosque.com.impexpap.utils.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * Resultados y calificación (contratos 3.5 y 7): exámenes del lote, revisión de abiertas, solucionario, historial
 * por CI, informe y la calificación del evaluador. Filtros, reglas y mensajes los decide el SP: sin el filtro que
 * pide cada acción la lista sale vacía (204). {@code audUsuario} sale del token.
 */
@RestController
@CrossOrigin(originPatterns = "${tcpe.cors.origenes:*}", methods = {RequestMethod.POST})
@RequestMapping("/examenes")
@PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
public class TcpeResultadosController {

    private static final Logger log = LoggerFactory.getLogger(TcpeResultadosController.class);

    private final ITcpeResultado resultados;
    private final ITcpeCalificacion calificacion;
    private final ITcpeRendir rendir;
    private final TcpePermisos permisos;

    public TcpeResultadosController(ITcpeResultado resultados, ITcpeCalificacion calificacion, ITcpeRendir rendir,
                                    TcpePermisos permisos) {
        this.resultados = resultados;
        this.calificacion = calificacion;
        this.rendir = rendir;
        this.permisos = permisos;
    }

    /**
     * Exámenes del lote; antes cierra vencidos y ausentes ({@code p_abm_tcpe_Rendir 'V'}) para que el estado salga al
     * día. Si eso falla, se registra y se lista igual. Sin {@code @Transactional}: el SP hace COMMIT de cada cierre.
     */
    @PostMapping("/resultados/listar")
    public ResponseEntity<ApiResponse<?>> listar(@Valid @RequestBody TcpeResultadoFiltroDto f, Authentication auth) {
        permisos.exigirAlguna(auth, TcpePermisos.RESULTADOS, TcpePermisos.LOTES);
        Long codRolExamen = f.getCodRolExamen();
        try {
            rendir.entregarVencidos(codRolExamen, (long) DatosToken.codUsuarioDe(auth));
        } catch (RuntimeException e) {
            log.error("No se pudieron cerrar los exámenes vencidos del lote {}: {}", codRolExamen, e.getMessage(), e);
        }
        return TcpeRespuestas.listado(resultados.listar(codRolExamen));
    }

    @PostMapping("/resultados/revision")
    public ResponseEntity<ApiResponse<?>> revision(@Valid @RequestBody TcpeResultadoFiltroDto f, Authentication auth) {
        permisos.exigirAlguna(auth, TcpePermisos.RESULTADOS);
        return TcpeRespuestas.listado(resultados.revision(f.getCodExamen()));
    }

    /** Solucionario de un examen ({@code codExamen}) o de todo el lote ({@code codRolExamen}). */
    @PostMapping("/resultados/solucionario")
    public ResponseEntity<ApiResponse<?>> solucionario(@Valid @RequestBody TcpeResultadoFiltroDto f,
                                                       Authentication auth) {
        permisos.exigirAlguna(auth, TcpePermisos.LOTES, TcpePermisos.RESULTADOS);
        return TcpeRespuestas.listado(resultados.solucionario(f.getCodExamen(), f.getCodRolExamen()));
    }

    @PostMapping("/resultados/historial")
    public ResponseEntity<ApiResponse<?>> historial(@Valid @RequestBody TcpeResultadoFiltroDto f, Authentication auth) {
        permisos.exigirAlguna(auth, TcpePermisos.INFORMES, TcpePermisos.RESULTADOS);
        return TcpeRespuestas.listado(resultados.historial(f.getCi()));
    }

    @PostMapping("/resultados/informe")
    public ResponseEntity<ApiResponse<?>> informe(@Valid @RequestBody(required = false) TcpeResultadoFiltroDto f,
                                                  Authentication auth) {
        permisos.exigirAlguna(auth, TcpePermisos.INFORMES);
        TcpeResultadoFiltroDto filtro = f != null ? f : new TcpeResultadoFiltroDto();
        return TcpeRespuestas.listado(
                resultados.informe(filtro.getCodClasificacion(), filtro.getFechaDesde(), filtro.getFechaHasta()));
    }

    @PostMapping("/calificacion/marcar")
    public ResponseEntity<ApiResponse<?>> marcar(@Valid @RequestBody TcpeCalificacionDto dto, Authentication auth) {
        permisos.exigirAlguna(auth, TcpePermisos.RESULTADOS);
        return TcpeRespuestas.escritura(calificacion.marcar(dto.getCodExamen(), dto.getCodRespuestaH(),
                dto.getEsCorrecta(), DatosToken.codUsuarioDe(auth)));
    }

    @PostMapping("/calificacion/cerrar")
    public ResponseEntity<ApiResponse<?>> cerrar(@Valid @RequestBody TcpeCalificacionDto dto, Authentication auth) {
        permisos.exigirAlguna(auth, TcpePermisos.RESULTADOS);
        return TcpeRespuestas.escritura(calificacion.cerrar(dto.getCodExamen(), DatosToken.codUsuarioDe(auth)));
    }

    /** Nota manual de un escrito; se carga desde Resultados o desde el detalle del lote (contrato 7.7). */
    @PostMapping("/calificacion/nota")
    public ResponseEntity<ApiResponse<?>> nota(@Valid @RequestBody TcpeCalificacionDto dto, Authentication auth) {
        permisos.exigirAlguna(auth, TcpePermisos.RESULTADOS, TcpePermisos.LOTES);
        return TcpeRespuestas.escritura(
                calificacion.nota(dto.getCodExamen(), dto.getResultado(), DatosToken.codUsuarioDe(auth)));
    }
}
