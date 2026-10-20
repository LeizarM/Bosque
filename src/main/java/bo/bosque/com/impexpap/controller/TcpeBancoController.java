package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.commons.TcpePermisos;
import bo.bosque.com.impexpap.dao.ITcpeBanco;
import bo.bosque.com.impexpap.dto.TcpeBancoAccionDto;
import bo.bosque.com.impexpap.dto.TcpeBancoDuplicadosFiltroDto;
import bo.bosque.com.impexpap.dto.TcpeBancoFiltroDto;
import bo.bosque.com.impexpap.dto.TcpeBancoGuardarDto;
import bo.bosque.com.impexpap.dto.TcpeBancoImportarDto;
import bo.bosque.com.impexpap.security.jwt.DatosToken;
import bo.bosque.com.impexpap.utils.ApiResponse;
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
 * Banco de preguntas (contrato 3.2): la pregunta con su respuesta y sus opciones se guarda en una sola operación
 * ({@code p_abm_tcpe_Banco}), con el XML de opciones armado en {@code TcpeBancoDao}. Exige la pantalla
 * {@value TcpePermisos#BANCO}.
 */
@RestController
@CrossOrigin(originPatterns = "${tcpe.cors.origenes:*}", methods = {RequestMethod.POST})
@RequestMapping("/examenes/banco")
@PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
public class TcpeBancoController {

    private final ITcpeBanco dao;
    private final TcpePermisos permisos;

    public TcpeBancoController(ITcpeBanco dao, TcpePermisos permisos) {
        this.dao = dao;
        this.permisos = permisos;
    }

    @PostMapping("/listar")
    public ResponseEntity<ApiResponse<?>> listar(@Valid @RequestBody(required = false) TcpeBancoFiltroDto filtro,
                                                 Authentication auth) {
        permisos.exigirAlguna(auth, TcpePermisos.BANCO);
        return TcpeRespuestas.listado(dao.listar(filtro));
    }

    /** Opciones de una pregunta ({@code codPregunta}) o de toda una categoría; sin ninguno de los dos, vacía. */
    @PostMapping("/opciones")
    public ResponseEntity<ApiResponse<?>> opciones(@Valid @RequestBody TcpeBancoFiltroDto filtro, Authentication auth) {
        permisos.exigirAlguna(auth, TcpePermisos.BANCO);
        return TcpeRespuestas.listado(dao.opciones(filtro.getCodPregunta(), filtro.getCodClasificacion()));
    }

    @PostMapping("/guardar")
    public ResponseEntity<ApiResponse<?>> guardar(@Valid @RequestBody TcpeBancoGuardarDto dto, Authentication auth) {
        permisos.exigirAlguna(auth, TcpePermisos.BANCO);
        return TcpeRespuestas.escritura(dao.guardar(dto, DatosToken.codUsuarioDe(auth)));
    }

    @PostMapping("/eliminar")
    public ResponseEntity<ApiResponse<?>> eliminar(@Valid @RequestBody TcpeBancoAccionDto dto, Authentication auth) {
        permisos.exigirAlguna(auth, TcpePermisos.BANCO);
        return TcpeRespuestas.escritura(dao.eliminar(dto.getCodPregunta(), DatosToken.codUsuarioDe(auth)));
    }

    @PostMapping("/activar")
    public ResponseEntity<ApiResponse<?>> activar(@Valid @RequestBody TcpeBancoAccionDto dto, Authentication auth) {
        permisos.exigirAlguna(auth, TcpePermisos.BANCO);
        return TcpeRespuestas.escritura(
                dao.activar(dto.getCodPregunta(), dto.getActivo(), DatosToken.codUsuarioDe(auth)));
    }

    /** Mientras se escribe (contrato 9.2): las iguales de la categoría ({@code igual}) y hasta 5 parecidas. */
    @PostMapping("/duplicados")
    public ResponseEntity<ApiResponse<?>> duplicados(@RequestBody TcpeBancoDuplicadosFiltroDto filtro,
                                                     Authentication auth) {
        permisos.exigirAlguna(auth, TcpePermisos.BANCO);
        return TcpeRespuestas.listado(dao.duplicados(filtro.getCodClasificacion(), filtro.getTexto()));
    }

    /** Vista previa de una importación (9.3): una fila por pregunta con el estado que decide el SP; no escribe. */
    @PostMapping("/importar/validar")
    public ResponseEntity<ApiResponse<?>> validarImportacion(@Valid @RequestBody TcpeBancoImportarDto dto,
                                                             Authentication auth) {
        permisos.exigirAlguna(auth, TcpePermisos.BANCO);
        return TcpeRespuestas.listado(dao.validarLote(dto));
    }

    /** Inserta solo las filas OK: {@code data} = cantidad importada y el mensaje del SP resume las omitidas. */
    @PostMapping("/importar")
    public ResponseEntity<ApiResponse<?>> importar(@Valid @RequestBody TcpeBancoImportarDto dto, Authentication auth) {
        permisos.exigirAlguna(auth, TcpePermisos.BANCO);
        return TcpeRespuestas.escritura(dao.importarLote(dto, DatosToken.codUsuarioDe(auth)));
    }
}
