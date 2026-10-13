package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.commons.AccesoPantalla;
import bo.bosque.com.impexpap.dao.ICoche;
import bo.bosque.com.impexpap.model.Coche;
import bo.bosque.com.impexpap.security.jwt.DatosToken;
import bo.bosque.com.impexpap.utils.ApiResponse;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Gestion de coches (vehiculos por sucursal, tabla {@code tac_coche}).
 *
 * <p>El permiso es la vista {@value #PANTALLA} (hija de "Entregas", en DISTRIBUCION): quien no la tiene
 * en su menu recibe 403 en los tres endpoints, aunque tenga sesion.
 *
 * <p>No es el flujo de {@code tacTareas/Coches} ("Revision de Autos", llegadas del dia): ese vive
 * en {@code TareasRutinariasController}. Los endpoints viejos {@code /tareas-rutinarias/*-coche}
 * siguen ahi por compatibilidad y exigen esta misma vista; esta pantalla no depende de ellos.
 *
 * <p>Todos los endpoints son POST, como el resto del backend. {@code audUsuario} sale del token,
 * nunca del cuerpo.
 */
@RestController
@CrossOrigin(origins = "*", methods = {RequestMethod.POST, RequestMethod.GET})
@RequestMapping("/coches")
@PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
public class CocheController {

    /** {@code tb_vista.direccion} de la pantalla; el literal esta tambien en el frontend (rutaCatalogoCoches). */
    public static final String PANTALLA = "tacCoche/Coche";

    private static final String SUCCESS_MESSAGE = "Operación realizada exitosamente";

    private final ICoche cocheDao;
    private final AccesoPantalla pantallas;

    public CocheController(ICoche cocheDao, AccesoPantalla pantallas) {
        this.cocheDao = cocheDao;
        this.pantallas = pantallas;
    }

    /** Lista coches con el nombre de su sucursal. Cuerpo vacio o {@code {}} = todos. */
    @PostMapping("/listar")
    public ResponseEntity<ApiResponse<?>> listar(@RequestBody(required = false) Coche filtro,
                                                 Authentication auth) {
        pantallas.exigir(auth, PANTALLA);
        List<Coche> lista = cocheDao.listarGestion(filtro);
        if (lista == null || lista.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(new ApiResponse<>("No se encontraron coches.", null, HttpStatus.NO_CONTENT.value()));
        }
        return ResponseEntity.ok(new ApiResponse<>(SUCCESS_MESSAGE, lista, HttpStatus.OK.value()));
    }

    /** Alta si {@code idCoche == 0}, edicion si es mayor. Devuelve el id en {@code data}. */
    @PostMapping("/registrar")
    public ResponseEntity<ApiResponse<?>> registrar(@RequestBody Coche mb, Authentication auth) {
        pantallas.exigir(auth, PANTALLA);
        mb.setAudUsuario(DatosToken.codUsuarioDe(auth));
        return escritura(cocheDao.registrar(mb, mb.getIdCoche() == 0 ? "I" : "U"));
    }

    /**
     * Elimina por id. El SP rechaza (400) al coche con kilometraje, combustible, llegadas o
     * prestamos: para dejar de usarlo se edita con {@code estado = 0}.
     */
    @PostMapping("/eliminar")
    public ResponseEntity<ApiResponse<?>> eliminar(@RequestBody Coche mb, Authentication auth) {
        pantallas.exigir(auth, PANTALLA);
        // Solo el id viaja al SP: el resto del cuerpo no se usa en una baja.
        Coche baja = new Coche();
        baja.setIdCoche(mb.getIdCoche());
        baja.setAudUsuario(DatosToken.codUsuarioDe(auth));
        return escritura(cocheDao.registrar(baja, "D"));
    }

    /** SpHelper ya lanza SpBusinessException (400) si error != 0; aqui solo llega el exito. */
    private ResponseEntity<ApiResponse<?>> escritura(RespuestaSp res) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(res.getErrormsg(), res.getIdGenerado(), HttpStatus.CREATED.value()));
    }
}
