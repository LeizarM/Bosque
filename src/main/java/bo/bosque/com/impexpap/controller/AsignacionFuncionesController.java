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
import bo.bosque.com.impexpap.model.AsigFunModXEmpl;
import bo.bosque.com.impexpap.security.jwt.DatosToken;
import bo.bosque.com.impexpap.utils.ApiResponse;

/**
 * Asignación de funciones a empleados ({@code tb_asigFunModXEmpl}), pantalla {@value #PANTALLA}
 * (vista 56). Como en el legacy no hay botones en el ACL ni baja: se asigna y se activa o desactiva.
 * Reglas y mensajes los pone {@code p_abm_AsigFunModXEmpl}.
 */
@RestController
@CrossOrigin(originPatterns = "${tcpe.cors.origenes:*}", methods = {RequestMethod.POST})
@RequestMapping("/asignacion-funciones")
@PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
public class AsignacionFuncionesController {

    static final String PANTALLA = "tbAsignaFuncXMod/AsignarFuncionPorModulo";

    private final IFuncionesModulo dao;
    private final AccesoPantalla pantallas;

    public AsignacionFuncionesController(IFuncionesModulo dao, AccesoPantalla pantallas) {
        this.dao = dao;
        this.pantallas = pantallas;
    }

    /** Los filtros van en el cuerpo: {@code codModulo}, {@code codFuncMod}, {@code codSucursal}, {@code estado}. */
    @PostMapping("/listar")
    public ResponseEntity<ApiResponse<?>> listar(@RequestBody AsigFunModXEmpl filtro, Authentication auth) {
        pantallas.exigir(auth, PANTALLA);
        return ParametrosRespuestas.listado(dao.listarAsignaciones(
                filtro.getCodModulo(), filtro.getCodFuncMod(), filtro.getCodSucursal(), filtro.getEstado()));
    }

    @PostMapping("/modulos")
    public ResponseEntity<ApiResponse<?>> modulos(Authentication auth) {
        pantallas.exigir(auth, PANTALLA);
        return ParametrosRespuestas.listado(dao.modulos());
    }

    /** Cuerpo {@code {id: codModulo}}: las funciones del módulo para el combo. */
    @PostMapping("/funciones")
    public ResponseEntity<ApiResponse<?>> funciones(@RequestBody FiltroIdDto filtro, Authentication auth) {
        pantallas.exigir(auth, PANTALLA);
        return ParametrosRespuestas.listado(dao.listarFunciones(filtro.getId()));
    }

    @PostMapping("/sucursales")
    public ResponseEntity<ApiResponse<?>> sucursales(Authentication auth) {
        pantallas.exigir(auth, PANTALLA);
        return ParametrosRespuestas.listado(dao.sucursales());
    }

    /** Cuerpo {@code {id: codSucursal}}: los empleados con relación vigente de esa sucursal. */
    @PostMapping("/empleados")
    public ResponseEntity<ApiResponse<?>> empleados(@RequestBody FiltroIdDto filtro, Authentication auth) {
        pantallas.exigir(auth, PANTALLA);
        return ParametrosRespuestas.listado(dao.empleadosDeSucursal(filtro.getId()));
    }

    /** Alta si {@code codAsignacion} es 0; si es mayor solo cambia el estado. */
    @PostMapping("/registrar")
    public ResponseEntity<ApiResponse<?>> registrar(@Valid @RequestBody AsigFunModXEmpl asignacion, Authentication auth) {
        pantallas.exigir(auth, PANTALLA);
        asignacion.setAudUsuario(DatosToken.codUsuarioDe(auth));
        return ParametrosRespuestas.escritura(dao.registrarAsignacion(asignacion));
    }
}
