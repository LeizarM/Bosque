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
import bo.bosque.com.impexpap.dao.ITipoTelefono;
import bo.bosque.com.impexpap.dto.FiltroIdDto;
import bo.bosque.com.impexpap.model.TipoTelefono;
import bo.bosque.com.impexpap.security.jwt.DatosToken;
import bo.bosque.com.impexpap.utils.ApiResponse;

/**
 * Tipos de teléfono ({@code trh_tipoTelefono}), pantalla {@value #PANTALLA} del legacy (vista 28).
 * Leer exige la pantalla y escribir el botón del ACL; reglas y mensajes los pone {@code p_abm_TipoTelefono}.
 * {@code /rrhh/tipoTelefono} sigue siendo el combo de la ficha y no cambia.
 */
@RestController
@CrossOrigin(originPatterns = "${tcpe.cors.origenes:*}", methods = {RequestMethod.POST})
@RequestMapping("/tipo-telefono")
@PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
public class TipoTelefonoController {

    static final String PANTALLA = "trhtipoTelefono/tipoTelefono";
    /** {@code tb_vista.codVista} de "Tipos de Telefono": dueña de los botones del ACL. */
    static final int VISTA = 28;
    static final String BTN_NUEVO = "btnNewTF";
    static final String BTN_EDITAR = "btnEditTF";
    static final String BTN_ELIMINAR = "btnEliminarTF";

    private final ITipoTelefono dao;
    private final AccesoPantalla pantallas;
    private final AccesoModuloHelper acceso;

    public TipoTelefonoController(ITipoTelefono dao, AccesoPantalla pantallas, AccesoModuloHelper acceso) {
        this.dao = dao;
        this.pantallas = pantallas;
        this.acceso = acceso;
    }

    @PostMapping("/listar")
    public ResponseEntity<ApiResponse<?>> listar(Authentication auth) {
        pantallas.exigir(auth, PANTALLA);
        return ParametrosRespuestas.listado(dao.listar());
    }

    /** Alta si {@code codTipoTel} es 0, edición si es mayor. */
    @PostMapping("/registrar")
    public ResponseEntity<ApiResponse<?>> registrar(@Valid @RequestBody TipoTelefono tipo, Authentication auth) {
        acceso.exigirBoton(auth, VISTA, tipo.getCodTipoTel() == 0 ? BTN_NUEVO : BTN_EDITAR);
        tipo.setAudUsuario(DatosToken.codUsuarioDe(auth));
        return ParametrosRespuestas.escritura(dao.registrar(tipo));
    }

    @PostMapping("/eliminar")
    public ResponseEntity<ApiResponse<?>> eliminar(@Valid @RequestBody FiltroIdDto filtro, Authentication auth) {
        acceso.exigirBoton(auth, VISTA, BTN_ELIMINAR);
        return ParametrosRespuestas.escritura(dao.eliminar(filtro.getId(), DatosToken.codUsuarioDe(auth)));
    }
}
