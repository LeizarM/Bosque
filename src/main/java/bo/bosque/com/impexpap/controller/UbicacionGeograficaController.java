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
import bo.bosque.com.impexpap.dao.IUbicacionGeografica;
import bo.bosque.com.impexpap.dto.FiltroIdDto;
import bo.bosque.com.impexpap.model.Ciudad;
import bo.bosque.com.impexpap.model.Pais;
import bo.bosque.com.impexpap.model.Zona;
import bo.bosque.com.impexpap.security.jwt.DatosToken;
import bo.bosque.com.impexpap.utils.ApiResponse;

/**
 * Países, ciudades y zonas, pantalla {@value #PANTALLA} del legacy (vista 26). Los tres niveles comparten
 * los botones del ACL de esa vista. Reglas y mensajes los ponen los {@code p_abm_*}; las lecturas de la
 * ficha ({@code /rrhh/paises}, {@code /rrhh/ciudadxPais}, {@code /rrhh/zonas}) no cambian.
 */
@RestController
@CrossOrigin(originPatterns = "${tcpe.cors.origenes:*}", methods = {RequestMethod.POST})
@RequestMapping("/ubicacion-geografica")
@PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
public class UbicacionGeograficaController {

    static final String PANTALLA = "trhUbicacionGeografica/ubicacionGeografica";
    /** {@code tb_vista.codVista} de "Ubicaciones Geograficos": dueña de los botones del ACL. */
    static final int VISTA = 26;
    static final String BTN_NUEVO = "btnNewU";
    static final String BTN_EDITAR = "btnEditU";
    static final String BTN_ELIMINAR = "btnEliminarU";

    private final IUbicacionGeografica dao;
    private final AccesoPantalla pantallas;
    private final AccesoModuloHelper acceso;

    public UbicacionGeograficaController(IUbicacionGeografica dao, AccesoPantalla pantallas,
                                         AccesoModuloHelper acceso) {
        this.dao = dao;
        this.pantallas = pantallas;
        this.acceso = acceso;
    }

    // ------------------------------------------------------------------ lecturas

    @PostMapping("/paises/listar")
    public ResponseEntity<ApiResponse<?>> listarPaises(Authentication auth) {
        pantallas.exigir(auth, PANTALLA);
        return ParametrosRespuestas.listado(dao.listarPaises());
    }

    /** Cuerpo {@code {id: codPais}}; 0 trae las ciudades de todos los países. */
    @PostMapping("/ciudades/listar")
    public ResponseEntity<ApiResponse<?>> listarCiudades(@RequestBody FiltroIdDto filtro, Authentication auth) {
        pantallas.exigir(auth, PANTALLA);
        return ParametrosRespuestas.listado(dao.listarCiudades(filtro.getId()));
    }

    /** Cuerpo {@code {id: codCiudad}}; 0 trae las zonas de todas las ciudades. */
    @PostMapping("/zonas/listar")
    public ResponseEntity<ApiResponse<?>> listarZonas(@RequestBody FiltroIdDto filtro, Authentication auth) {
        pantallas.exigir(auth, PANTALLA);
        return ParametrosRespuestas.listado(dao.listarZonas(filtro.getId()));
    }

    // ------------------------------------------------------------------ escrituras

    @PostMapping("/paises/registrar")
    public ResponseEntity<ApiResponse<?>> registrarPais(@Valid @RequestBody Pais pais, Authentication auth) {
        exigirAltaOEdicion(auth, pais.getCodPais());
        pais.setAudUsuario(DatosToken.codUsuarioDe(auth));
        return ParametrosRespuestas.escritura(dao.registrarPais(pais));
    }

    @PostMapping("/ciudades/registrar")
    public ResponseEntity<ApiResponse<?>> registrarCiudad(@Valid @RequestBody Ciudad ciudad, Authentication auth) {
        exigirAltaOEdicion(auth, ciudad.getCodCiudad());
        ciudad.setAudUsuario(DatosToken.codUsuarioDe(auth));
        return ParametrosRespuestas.escritura(dao.registrarCiudad(ciudad));
    }

    @PostMapping("/zonas/registrar")
    public ResponseEntity<ApiResponse<?>> registrarZona(@Valid @RequestBody Zona zona, Authentication auth) {
        exigirAltaOEdicion(auth, zona.getCodZona());
        zona.setAudUsuario(DatosToken.codUsuarioDe(auth));
        return ParametrosRespuestas.escritura(dao.registrarZona(zona));
    }

    @PostMapping("/paises/eliminar")
    public ResponseEntity<ApiResponse<?>> eliminarPais(@RequestBody FiltroIdDto filtro, Authentication auth) {
        acceso.exigirBoton(auth, VISTA, BTN_ELIMINAR);
        return ParametrosRespuestas.escritura(dao.eliminarPais(filtro.getId(), DatosToken.codUsuarioDe(auth)));
    }

    @PostMapping("/ciudades/eliminar")
    public ResponseEntity<ApiResponse<?>> eliminarCiudad(@RequestBody FiltroIdDto filtro, Authentication auth) {
        acceso.exigirBoton(auth, VISTA, BTN_ELIMINAR);
        return ParametrosRespuestas.escritura(dao.eliminarCiudad(filtro.getId(), DatosToken.codUsuarioDe(auth)));
    }

    @PostMapping("/zonas/eliminar")
    public ResponseEntity<ApiResponse<?>> eliminarZona(@RequestBody FiltroIdDto filtro, Authentication auth) {
        acceso.exigirBoton(auth, VISTA, BTN_ELIMINAR);
        return ParametrosRespuestas.escritura(dao.eliminarZona(filtro.getId(), DatosToken.codUsuarioDe(auth)));
    }

    /** Alta si el id es 0, edición si es mayor: cada una con su botón. */
    private void exigirAltaOEdicion(Authentication auth, long id) {
        acceso.exigirBoton(auth, VISTA, id == 0 ? BTN_NUEVO : BTN_EDITAR);
    }
}
