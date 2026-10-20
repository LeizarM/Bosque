package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.commons.TcpeEscritoService;
import bo.bosque.com.impexpap.commons.TcpeLoteService;
import bo.bosque.com.impexpap.commons.TcpePermisos;
import bo.bosque.com.impexpap.dao.ITcpeLote;
import bo.bosque.com.impexpap.dto.TcpeEscritoPdfDto;
import bo.bosque.com.impexpap.dto.TcpeLoteAgregarDto;
import bo.bosque.com.impexpap.dto.TcpeLoteFiltroDto;
import bo.bosque.com.impexpap.dto.TcpeLoteGuardarDto;
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
 * Lotes de examen (contratos 3.3, 7 y 10). Generar y agregar validan primero con {@code p_abm_tcpe_Lote 'V'} y
 * hashean después, fuera de la transacción ({@link TcpeLoteService#pedido}). Las claves en claro viajan solo en esa
 * respuesta y solo para los externos en línea; un escrito no lleva credenciales, sino sus PDF.
 */
@RestController
@CrossOrigin(originPatterns = "${tcpe.cors.origenes:*}", methods = {RequestMethod.POST})
@RequestMapping("/examenes/lotes")
@PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
public class TcpeLotesController {

    private final ITcpeLote dao;
    private final TcpeLoteService lotes;
    private final TcpeEscritoService escritos;
    private final TcpePermisos permisos;

    public TcpeLotesController(ITcpeLote dao, TcpeLoteService lotes, TcpeEscritoService escritos,
                               TcpePermisos permisos) {
        this.dao = dao;
        this.lotes = lotes;
        this.escritos = escritos;
        this.permisos = permisos;
    }

    @PostMapping("/listar")
    public ResponseEntity<ApiResponse<?>> listar(@Valid @RequestBody(required = false) TcpeLoteFiltroDto filtro,
                                                 Authentication auth) {
        permisos.exigirAlguna(auth, TcpePermisos.LOTES, TcpePermisos.RESULTADOS, TcpePermisos.INFORMES,
                TcpePermisos.EN_VIVO);
        return TcpeRespuestas.listado(dao.listar(filtro));
    }

    @PostMapping("/disponibles")
    public ResponseEntity<ApiResponse<?>> disponibles(@Valid @RequestBody TcpeLoteFiltroDto filtro,
                                                      Authentication auth) {
        permisos.exigirAlguna(auth, TcpePermisos.LOTES);
        return TcpeRespuestas.listado(dao.disponibles(filtro.getCodClasificacion()));
    }

    /** Para elegir empleados al generar o agregar (contrato 14): con CI, cargo actual y si se les puede asignar. */
    @PostMapping("/empleados")
    public ResponseEntity<ApiResponse<?>> empleados(@Valid @RequestBody(required = false) TcpeLoteFiltroDto filtro,
                                                    Authentication auth) {
        permisos.exigirAlguna(auth, TcpePermisos.LOTES);
        return TcpeRespuestas.listado(dao.empleados(filtro == null ? null : filtro.getCodRolExamen()));
    }

    @PostMapping("/generar")
    public ResponseEntity<ApiResponse<?>> generar(@Valid @RequestBody TcpeLoteGuardarDto dto, Authentication auth) {
        permisos.exigirAlguna(auth, TcpePermisos.LOTES);
        long aud = DatosToken.codUsuarioDe(auth);
        // Lote mixto: cantidad, personas y empleados pueden llegar juntos; V decide con los tres conteos.
        TcpeLoteService.Pedido pedido = lotes.pedido(dto);
        return TcpeRespuestas.creado(lotes.generar(dto, pedido, aud));
    }

    @PostMapping("/agregar")
    public ResponseEntity<ApiResponse<?>> agregar(@Valid @RequestBody TcpeLoteAgregarDto dto, Authentication auth) {
        permisos.exigirAlguna(auth, TcpePermisos.LOTES);
        long aud = DatosToken.codUsuarioDe(auth);
        // V con el codRolExamen: sin lote, el SP responde con su propio mensaje.
        TcpeLoteService.Pedido pedido = lotes.pedido(dto);
        return TcpeRespuestas.creado(lotes.agregar(dto.getCodRolExamen(), pedido, aud));
    }

    @PostMapping("/actualizar")
    public ResponseEntity<ApiResponse<?>> actualizar(@Valid @RequestBody TcpeLoteGuardarDto dto, Authentication auth) {
        permisos.exigirAlguna(auth, TcpePermisos.LOTES);
        return TcpeRespuestas.escritura(dao.actualizar(dto, DatosToken.codUsuarioDe(auth)));
    }

    @PostMapping("/eliminar")
    public ResponseEntity<ApiResponse<?>> eliminar(@RequestBody TcpeLoteFiltroDto dto, Authentication auth) {
        permisos.exigirAlguna(auth, TcpePermisos.LOTES);
        return TcpeRespuestas.escritura(dao.eliminar(dto.getCodRolExamen(), DatosToken.codUsuarioDe(auth)));
    }

    /** PDF para imprimir: cabecera con nombre y CI (o líneas), preguntas con a), b)... y líneas en las abiertas. */
    @PostMapping("/examen-escrito-pdf")
    public ResponseEntity<byte[]> examenEscritoPdf(@Valid @RequestBody TcpeEscritoPdfDto dto, Authentication auth) {
        permisos.exigirAlguna(auth, TcpePermisos.LOTES, TcpePermisos.RESULTADOS);
        return TcpeRespuestas.pdf(escritos.pdf(dto.getCodRolExamen(), dto.getCodExamen(), false),
                "examen-" + dto.getCodRolExamen() + ".pdf");
    }

    /** El mismo PDF con las opciones correctas marcadas. */
    @PostMapping("/solucionario-pdf")
    public ResponseEntity<byte[]> solucionarioPdf(@Valid @RequestBody TcpeEscritoPdfDto dto, Authentication auth) {
        permisos.exigirAlguna(auth, TcpePermisos.LOTES, TcpePermisos.RESULTADOS);
        return TcpeRespuestas.pdf(escritos.pdf(dto.getCodRolExamen(), dto.getCodExamen(), true),
                "solucionario-" + dto.getCodRolExamen() + ".pdf");
    }
}
