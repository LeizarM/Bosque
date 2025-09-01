package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.dao.IUsuarioBloqueado;
import bo.bosque.com.impexpap.model.Email;
import bo.bosque.com.impexpap.model.Empleado;
import bo.bosque.com.impexpap.model.UsuarioBloqueado;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin ("*")
@RequestMapping ("/bloqueo")
public class UsuarioBloqueadoController {
    private final IUsuarioBloqueado ubDao;

    public UsuarioBloqueadoController (IUsuarioBloqueado ubDao){
        this.ubDao = ubDao;
    }
    /**
     * Procedimiento para registrar o actualizar advertencia/bloqueo de usuario
     * @param ub UsuarioBloqueado
     * @return ResponseEntity
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/advertencia")
    public ResponseEntity<?> registrarAdvertencia(@RequestBody UsuarioBloqueado ub) {

        Map<String, Object> response = new HashMap<>();

        // Verifica si ya existe advertencia para el usuario
        UsuarioBloqueado existente = this.ubDao.obtenerPorUsuario(ub.getCodUsuario());

        String acc = "U";
        if (existente == null) {
            acc = "I";
        }

        if (!this.ubDao.registrarAdvertencia(ub, acc)) {
            response.put("msg", "Error al registrar/actualizar advertencia o bloqueo");
            response.put("error", "ok");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        response.put("msg", "Advertencia o bloqueo registrado/actualizado");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    /**
     * Procedimiento para eliminar advertencia/bloqueo de usuario
     * @param codUsuario
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @DeleteMapping("/desbloqueo/{codUsuario}")
    public ResponseEntity<Map<String, Object>> desbloquearUsuario(@PathVariable int codUsuario) {
        System.out.println(">>> Entrando a desbloquearUsuario con codUsuario: " + codUsuario); // Log para depuración

        Map<String, Object> response = new HashMap<>();
        boolean eliminado = ubDao.eliminarAdvertencia(codUsuario);

        if (!eliminado) {
            System.out.println("❌ Error al desbloquear usuario: " + codUsuario); // Log de error
            response.put("msg", "Error al desbloquear usuario.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        System.out.println("✅ Usuario desbloqueado correctamente: " + codUsuario); // Log de éxito
        response.put("msg", "Usuario desbloqueado correctamente.");
        return ResponseEntity.ok(response);
    }
    /**
     * Procedimiento para verificar si un usuario esta bloqueado
     * @param
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/usuarioBloqueado")
    public UsuarioBloqueado obtenerUbloqueado (@RequestBody  UsuarioBloqueado ub ){


        UsuarioBloqueado temp = this.ubDao.obtenerPorUsuario( ub.getCodUsuario() );
        if(temp == null) return new UsuarioBloqueado();
        //System.out.println(temp.toString());
        return temp;
    }

}
