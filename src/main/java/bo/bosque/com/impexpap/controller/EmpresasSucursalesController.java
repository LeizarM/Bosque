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
import bo.bosque.com.impexpap.dao.IEmpresasSucursales;
import bo.bosque.com.impexpap.dao.IUbicacionGeografica;
import bo.bosque.com.impexpap.dto.FiltroIdDto;
import bo.bosque.com.impexpap.model.Empresa;
import bo.bosque.com.impexpap.model.Sucursal;
import bo.bosque.com.impexpap.security.jwt.DatosToken;
import bo.bosque.com.impexpap.utils.ApiResponse;

/**
 * Empresas y sucursales, el ABM que le faltaba a la pantalla {@value #PANTALLA} (vista 12). Usa los botones
 * de esa vista: btnNuevoEO/btnEditarEO/btnEliminarEO para empresas y btnNuevoSucXEmp/btnEditSucXEmp/
 * btnDelSucXEmp para sucursales. Reglas y mensajes los ponen {@code p_abm_Empresa} y {@code p_abm_Sucursal}.
 */
@RestController
@CrossOrigin(originPatterns = "${tcpe.cors.origenes:*}", methods = {RequestMethod.POST})
@RequestMapping("/empresas-sucursales")
@PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
public class EmpresasSucursalesController {

    static final String PANTALLA = "trhEstructuraOrganizacional/estructuraOrganizacional";
    /** {@code tb_vista.codVista} de "Estructura Organizacional": dueña de los botones del ACL. */
    static final int VISTA = 12;

    private final IEmpresasSucursales dao;
    private final IUbicacionGeografica ubicaciones;
    private final AccesoPantalla pantallas;
    private final AccesoModuloHelper acceso;

    public EmpresasSucursalesController(IEmpresasSucursales dao, IUbicacionGeografica ubicaciones,
                                        AccesoPantalla pantallas, AccesoModuloHelper acceso) {
        this.dao = dao;
        this.ubicaciones = ubicaciones;
        this.pantallas = pantallas;
        this.acceso = acceso;
    }

    @PostMapping("/empresas/listar")
    public ResponseEntity<ApiResponse<?>> listarEmpresas(Authentication auth) {
        pantallas.exigir(auth, PANTALLA);
        return ParametrosRespuestas.listado(dao.listarEmpresas());
    }

    /** Cuerpo {@code {id: codEmpresa}}; 0 trae las sucursales de todas las empresas. */
    @PostMapping("/sucursales/listar")
    public ResponseEntity<ApiResponse<?>> listarSucursales(@RequestBody FiltroIdDto filtro, Authentication auth) {
        pantallas.exigir(auth, PANTALLA);
        return ParametrosRespuestas.listado(dao.listarSucursales(filtro.getId()));
    }

    /** Las ciudades para el formulario de sucursal (la misma lectura que la pantalla de ubicaciones). */
    @PostMapping("/ciudades")
    public ResponseEntity<ApiResponse<?>> ciudades(Authentication auth) {
        pantallas.exigir(auth, PANTALLA);
        return ParametrosRespuestas.listado(ubicaciones.listarCiudades(0));
    }

    @PostMapping("/empresas/registrar")
    public ResponseEntity<ApiResponse<?>> registrarEmpresa(@Valid @RequestBody Empresa empresa, Authentication auth) {
        acceso.exigirBoton(auth, VISTA, empresa.getCodEmpresa() == 0 ? "btnNuevoEO" : "btnEditarEO");
        empresa.setAudUsuario(DatosToken.codUsuarioDe(auth));
        return ParametrosRespuestas.escritura(dao.registrarEmpresa(empresa));
    }

    @PostMapping("/sucursales/registrar")
    public ResponseEntity<ApiResponse<?>> registrarSucursal(@Valid @RequestBody Sucursal sucursal, Authentication auth) {
        acceso.exigirBoton(auth, VISTA, sucursal.getCodSucursal() == 0 ? "btnNuevoSucXEmp" : "btnEditSucXEmp");
        sucursal.setAudUsuarioI(DatosToken.codUsuarioDe(auth));
        return ParametrosRespuestas.escritura(dao.registrarSucursal(sucursal));
    }

    @PostMapping("/empresas/eliminar")
    public ResponseEntity<ApiResponse<?>> eliminarEmpresa(@RequestBody FiltroIdDto filtro, Authentication auth) {
        acceso.exigirBoton(auth, VISTA, "btnEliminarEO");
        return ParametrosRespuestas.escritura(dao.eliminarEmpresa(filtro.getId(), DatosToken.codUsuarioDe(auth)));
    }

    @PostMapping("/sucursales/eliminar")
    public ResponseEntity<ApiResponse<?>> eliminarSucursal(@RequestBody FiltroIdDto filtro, Authentication auth) {
        acceso.exigirBoton(auth, VISTA, "btnDelSucXEmp");
        return ParametrosRespuestas.escritura(dao.eliminarSucursal(filtro.getId(), DatosToken.codUsuarioDe(auth)));
    }
}
