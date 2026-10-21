package bo.bosque.com.impexpap.controller;

import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import bo.bosque.com.impexpap.commons.AccesoModuloHelper;
import bo.bosque.com.impexpap.commons.AccesoPantalla;
import bo.bosque.com.impexpap.dao.INivelJerarquico;
import bo.bosque.com.impexpap.dto.FiltroIdDto;
import bo.bosque.com.impexpap.model.NivelJerarquico;
import bo.bosque.com.impexpap.security.jwt.DatosToken;
import bo.bosque.com.impexpap.utils.ApiResponse;

/**
 * Escalas de niveles jerárquicos ({@code tb_nivelJerarquico}), pantalla {@value #PANTALLA} (vista 15).
 * Leer exige la pantalla y escribir el botón del ACL; reglas y mensajes los pone {@code p_abm_NivelJerarquico}.
 * {@code /rrhh/lstNivelesJerarquicos} sigue siendo el combo de cargos y no cambia.
 */
@RestController
@CrossOrigin(originPatterns = "${tcpe.cors.origenes:*}", methods = {RequestMethod.POST})
@RequestMapping("/nivel-jerarquico")
@PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
public class NivelJerarquicoController {

    static final String PANTALLA = "tbNivelJerarquico/nivelJerarquico";
    /** {@code tb_vista.codVista} de "Niveles Jerarquicos": dueña de los botones del ACL. */
    static final int VISTA = 15;
    static final String BTN_NUEVO = "btnNewNJ";
    static final String BTN_EDITAR = "btnEditNJ";
    static final String BTN_ELIMINAR = "btnEliminarNJ";

    private final INivelJerarquico dao;
    private final AccesoPantalla pantallas;
    private final AccesoModuloHelper acceso;

    public NivelJerarquicoController(INivelJerarquico dao, AccesoPantalla pantallas, AccesoModuloHelper acceso) {
        this.dao = dao;
        this.pantallas = pantallas;
        this.acceso = acceso;
    }

    @PostMapping("/listar")
    public ResponseEntity<ApiResponse<?>> listar(Authentication auth) {
        pantallas.exigir(auth, PANTALLA);
        return ParametrosRespuestas.listado(dao.listar());
    }

    /** Alta si {@code codNivel} es 0 (también una nueva vigencia de un nivel), edición si es mayor. */
    @PostMapping("/registrar")
    public ResponseEntity<ApiResponse<?>> registrar(@Valid @RequestBody NivelJerarquico escala, Authentication auth) {
        acceso.exigirBoton(auth, VISTA, escala.getCodNivel() == 0 ? BTN_NUEVO : BTN_EDITAR);
        escala.setAudUsuario(DatosToken.codUsuarioDe(auth));
        return ParametrosRespuestas.escritura(dao.registrar(escala));
    }

    @PostMapping("/eliminar")
    public ResponseEntity<ApiResponse<?>> eliminar(@Valid @RequestBody FiltroIdDto filtro, Authentication auth) {
        acceso.exigirBoton(auth, VISTA, BTN_ELIMINAR);
        return ParametrosRespuestas.escritura(dao.eliminar((int) filtro.getId(), DatosToken.codUsuarioDe(auth)));
    }
}
