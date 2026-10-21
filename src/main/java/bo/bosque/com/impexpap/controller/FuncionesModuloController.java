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

import bo.bosque.com.impexpap.commons.AccesoPantalla;
import bo.bosque.com.impexpap.dao.IFuncionesModulo;
import bo.bosque.com.impexpap.dto.FiltroIdDto;
import bo.bosque.com.impexpap.model.FuncionesXModulo;
import bo.bosque.com.impexpap.security.jwt.DatosToken;
import bo.bosque.com.impexpap.utils.ApiResponse;

/**
 * Funciones por módulo ({@code tb_funcionesXModulo}), pantalla {@value #PANTALLA} del legacy (vista 57).
 * La vista no tiene botones en {@code tb_vistaBtn}: como en el legacy, quien tiene la pantalla puede
 * escribir. Reglas y mensajes los pone {@code p_abm_FuncionesXModulo}.
 */
@RestController
@CrossOrigin(originPatterns = "${tcpe.cors.origenes:*}", methods = {RequestMethod.POST})
@RequestMapping("/funciones-modulo")
@PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
public class FuncionesModuloController {

    static final String PANTALLA = "tbFuncionModulo/FuncionesPorModulo";

    private final IFuncionesModulo dao;
    private final AccesoPantalla pantallas;

    public FuncionesModuloController(IFuncionesModulo dao, AccesoPantalla pantallas) {
        this.dao = dao;
        this.pantallas = pantallas;
    }

    @PostMapping("/modulos")
    public ResponseEntity<ApiResponse<?>> modulos(Authentication auth) {
        pantallas.exigir(auth, PANTALLA);
        return ParametrosRespuestas.listado(dao.modulos());
    }

    /** Cuerpo {@code {id: codModulo}}; 0 trae las funciones de todos los módulos. */
    @PostMapping("/listar")
    public ResponseEntity<ApiResponse<?>> listar(@RequestBody FiltroIdDto filtro, Authentication auth) {
        pantallas.exigir(auth, PANTALLA);
        return ParametrosRespuestas.listado(dao.listarFunciones(filtro.getId()));
    }

    /** Alta si {@code codFuncMod} es 0, edición si es mayor. */
    @PostMapping("/registrar")
    public ResponseEntity<ApiResponse<?>> registrar(@Valid @RequestBody FuncionesXModulo funcion, Authentication auth) {
        pantallas.exigir(auth, PANTALLA);
        funcion.setAudUsuario(DatosToken.codUsuarioDe(auth));
        return ParametrosRespuestas.escritura(dao.registrarFuncion(funcion));
    }

    @PostMapping("/eliminar")
    public ResponseEntity<ApiResponse<?>> eliminar(@RequestBody FiltroIdDto filtro, Authentication auth) {
        pantallas.exigir(auth, PANTALLA);
        return ParametrosRespuestas.escritura(dao.eliminarFuncion(filtro.getId(), DatosToken.codUsuarioDe(auth)));
    }
}
