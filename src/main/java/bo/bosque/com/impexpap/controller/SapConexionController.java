package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.commons.AccesoPantalla;
import bo.bosque.com.impexpap.commons.SapConexiones;
import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.dto.SapConexionDto;
import bo.bosque.com.impexpap.model.SapConexion;
import bo.bosque.com.impexpap.security.jwt.DatosToken;
import bo.bosque.com.impexpap.utils.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Las conexiones a SAP que comparten los módulos (Administración &gt; Conexiones SAP).
 *
 * <p>Es lo único que ve URL, usuario y si hay contraseña, así que todo exige esa pantalla. Los módulos
 * no pasan por aquí: ofrecen las conexiones en su propio endpoint, detrás de su pantalla y sin esos datos
 * ({@link SapConexiones#opciones()}).
 */
@RestController
@RequestMapping("/sap/conexiones")
@CrossOrigin(origins = "*", methods = {RequestMethod.POST})
public class SapConexionController {

    private final AccesoPantalla pantallas;
    private final SapConexiones conexiones;

    public SapConexionController(AccesoPantalla pantallas, SapConexiones conexiones) {
        this.pantallas = pantallas;
        this.conexiones = conexiones;
    }

    /** Todas; de la contraseña solo se dice si está cargada. */
    @PostMapping("/listar")
    public ResponseEntity<ApiResponse<?>> listar(Authentication auth) {
        pantallas.exigir(auth, SapConexiones.PANTALLA);
        return ResponseEntity.ok(new ApiResponse<>("Conexiones SAP.", conexiones.paraPantalla(), HttpStatus.OK.value()));
    }

    /** Alta o cambio. {@code data} es el id. No toca la contraseña, que tiene su propio endpoint. */
    @PostMapping("/guardar")
    public ResponseEntity<ApiResponse<?>> guardar(@RequestBody SapConexion c, Authentication auth) {
        pantallas.exigir(auth, SapConexiones.PANTALLA);
        long id = conexiones.guardar(c, DatosToken.codUsuarioDe(auth));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("Conexión guardada. Rige desde el próximo uso.", id, HttpStatus.CREATED.value()));
    }

    /** La contraseña del Service Layer; se guarda cifrada y nunca vuelve a la app. */
    @PostMapping("/clave")
    public ResponseEntity<ApiResponse<?>> guardarClave(@RequestBody SapConexionDto dto, Authentication auth) {
        pantallas.exigir(auth, SapConexiones.PANTALLA);
        conexiones.guardarClave(conexion(dto), dto.getClave(), DatosToken.codUsuarioDe(auth));
        return respuesta("Contraseña guardada.");
    }

    /** Falla si algún módulo la usa. */
    @PostMapping("/eliminar")
    public ResponseEntity<ApiResponse<?>> eliminar(@RequestBody SapConexionDto dto, Authentication auth) {
        pantallas.exigir(auth, SapConexiones.PANTALLA);
        conexiones.eliminar(conexion(dto), DatosToken.codUsuarioDe(auth));
        return respuesta("Conexión eliminada.");
    }

    /** Inicia y cierra sesión en SAP con lo guardado, para comprobar usuario, contraseña y certificado. */
    @PostMapping("/probar")
    public ResponseEntity<ApiResponse<?>> probar(@RequestBody SapConexionDto dto, Authentication auth) {
        pantallas.exigir(auth, SapConexiones.PANTALLA);
        return respuesta(conexiones.probar(conexion(dto)));
    }

    private static int conexion(SapConexionDto dto) {
        if (dto == null || dto.getIdConexion() == null) throw new SpBusinessException("Elige la conexión.");
        return dto.getIdConexion();
    }

    private static ResponseEntity<ApiResponse<?>> respuesta(String mensaje) {
        return ResponseEntity.ok(new ApiResponse<>(mensaje, null, HttpStatus.OK.value()));
    }
}
