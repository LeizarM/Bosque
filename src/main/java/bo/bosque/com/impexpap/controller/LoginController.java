package bo.bosque.com.impexpap.controller;



import bo.bosque.com.impexpap.dao.ILoginDao;
import bo.bosque.com.impexpap.model.Producto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

import bo.bosque.com.impexpap.security.jwt.JwtProvider;
import bo.bosque.com.impexpap.security.model.Jwt;
import bo.bosque.com.impexpap.model.Login;




@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/auth")
@Slf4j
public class LoginController {

    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;
    private final ILoginDao ldao;

    public LoginController( PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, JwtProvider jwtProvider, ILoginDao ldao ){
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtProvider = jwtProvider;
        this.ldao = ldao;
    }


    /**
     * EndPoint para autenticar al usuario
     * @param login Objeto con las credenciales del usuario
     * @param bindingResult Objeto para la validación de campos
     * @return ResponseEntity con el token JWT o mensaje de error
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Login login, BindingResult bindingResult) {
        Map<String, Object> response = new HashMap<>();
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(
                RequestContextHolder.getRequestAttributes())).getRequest();

        // Validación de datos recibidos
        if (bindingResult.hasErrors()) {

            response.put("mensaje", "Error en los datos enviados");
            response.put("status", "error");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        // Validación de campos obligatorios
        if (login.getUsername() == null || login.getUsername().trim().isEmpty()) {
            response.put("mensaje", "El nombre de usuario es obligatorio");
            response.put("status", "error");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        if (login.getPassword2() == null || login.getPassword2().trim().isEmpty()) {
            response.put("mensaje", "La contraseña es obligatoria");
            response.put("status", "error");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        try {
            // Paso 1: Verificar si el usuario existe y obtener su información
            Login loginTemp = this.ldao.verifyUser(
                    login.getUsername(),
                    login.getPassword2(),
                    request.getRemoteAddr()
            );

            // Paso 2: Verificar diferentes estados de autenticación
            if (loginTemp.getCodUsuario() < 0) {
                // Usuario bloqueado (codUsuario negativo)
                response.put("mensaje", "Su cuenta ha sido bloqueada. Por favor, contacte al administrador del sistema.");
                response.put("status", "error");
                return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);

            } else if (loginTemp.getCodUsuario() == 0) {
                // Usuario no existe
                response.put("mensaje", "El usuario ingresado no existe en el sistema.");
                response.put("status", "error");
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }


            // Paso 3: El usuario existe, verificar la contraseña con Spring Security
            try {
                // Intentamos autenticar con Spring Security
                Authentication authentication = authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(login.getLogin(), login.getPassword2())
                );

                // La autenticación fue exitosa
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // IMPORTANTE: Registrar el intento exitoso en la bitácora
                this.ldao.registerSuccessfulLogin(login.getUsername(), request.getRemoteAddr());

                // Generación del token JWT
                String jwt = jwtProvider.generateToken(authentication, loginTemp);
                UserDetails userDetails = (UserDetails) authentication.getPrincipal();

                // Creación del objeto de respuesta con el token
                Jwt jwtT = new Jwt(
                        jwt,
                        loginTemp.getEmpleado().getPersona().getDatoPersona(),
                        loginTemp.getEmpleado().getEmpleadoCargo().getCargoSucursal().getCargo().getDescripcion(),
                        loginTemp.getTipoUsuario(),
                        loginTemp.getCodUsuario(),
                        loginTemp.getCodEmpleado(),
                        loginTemp.getCodEmpresa(),
                        loginTemp.getCodCiudad(),
                        login.getLogin(),
                        loginTemp.getVersionApp(),
                        loginTemp.getCodSucursal(),
                        userDetails.getAuthorities()
                );

                // Respuesta exitosa
                response.put("data", jwtT);
                response.put("mensaje", "Inicio de sesión exitoso");
                response.put("status", "success");
                return new ResponseEntity<>(response, HttpStatus.OK);

            } catch (BadCredentialsException e) {
                // Contraseña incorrecta - Registrar intento fallido
                log.error("Contraseña incorrecta: " + e.getMessage());

                // Registramos el intento fallido
                Login updatedLogin = this.ldao.registerFailedAttempt(login.getUsername(), request.getRemoteAddr());

                // Preparamos la respuesta
                response.put("mensaje", "Contraseña incorrecta. Por favor, verifique e intente nuevamente.");
                response.put("status", "error");

                // Si hay información de intentos fallidos
                if (updatedLogin != null) {

                    // Si la cuenta fue bloqueada debido a este intento
                    if (updatedLogin.getCodUsuario() < 0) {
                        response.put("mensaje", "Su cuenta ha sido bloqueada debido a múltiples intentos fallidos. Por favor, contacte al administrador.");
                        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
                    }
                }

                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            } catch (Exception e) {
                // Otros errores durante la autenticación
                log.error("Error durante la autenticación: " + e.getMessage());
                response.put("mensaje", "Error en el proceso de autenticación");
                response.put("status", "error");
                return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
            }

        } catch (Exception e) {
            // Error general en el proceso
            log.error("Error en el proceso de login: " + e.getMessage());
            response.put("mensaje", "Error en el servicio de autenticación");
            response.put("status", "error");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Registra un intento fallido de login y verifica si la cuenta debe ser bloqueada
     * @param username Nombre de usuario
     * @param ip Dirección IP del cliente
     * @return Login actualizado con información de intentos fallidos
     */
    private Login registerFailedAttempt(String username, String ip) {
        try {
            // Llamamos a un procedimiento almacenado específico para registrar el intento fallido
            // y verificar si debemos bloquear la cuenta
            return this.ldao.registerFailedAttempt(username, ip);
        } catch (Exception e) {
            log.error("Error al registrar intento fallido: " + e.getMessage());
            return null;
        }
    }

    /**
     * Serv. para cambiar la contraseña
     * @param login
     * @return
     */
    @PostMapping("/changePassword")
    public ResponseEntity<?>login(@RequestBody Login login) {
        Map<String, Object> response = new HashMap<>();
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();  // extraemos la ip de donde se esta logueando

        Login loginTemp = this.ldao.verifyUser(login.getUsername(),  passwordEncoder.encode( login.getPassword2() ), request.getRemoteAddr());

        if( loginTemp.getCodUsuario() <= 0 ){
            response.put("error", "Error al reconocer el usuario");
            response.put("ok", false);
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken( login.getLogin(), login.getPassword2() ) );
        SecurityContextHolder.getContext().setAuthentication( authentication );
        if( !authentication.isAuthenticated() ){
            response.put("error", "Error En las credenciales");
            response.put("ok", false);
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        loginTemp.setPassword2(passwordEncoder.encode( login.getNpassword() ));
        if(!this.ldao.abmLogin(loginTemp,"Q")){
            response.put("error", "Error En las credenciales");
            response.put("ok", false);
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        response.put("msg", "Constraseña actualizada correctamente");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);

    }


    /**
     * Serv. para cambiar la contraseña que aparece por default
     * @param datos
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/changePasswordDefault")
    public ResponseEntity<?> changePasswordDefault(@RequestBody Map<String, Object> datos) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Extraer valores del mapa
            Integer codUsuario = (Integer) datos.get("codUsuario");
            String npassword = (String) datos.get("npassword");

            if (codUsuario == null || npassword == null) {
                response.put("error", "Datos incompletos");
                response.put("ok", false);
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            // Crear objeto Login con información mínima necesaria
            Login login = new Login();
            login.setCodUsuario(codUsuario);
            login.setPassword2(passwordEncoder.encode(npassword));



            if(!this.ldao.abmLogin(login, "Q")) {
                response.put("error", "Error al actualizar las credenciales");
                response.put("ok", false);
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            response.put("msg", "Contraseña actualizada correctamente");
            response.put("ok", true);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (Exception e) {
            log.error("Error al cambiar contraseña: ", e);
            response.put("error", "Error interno del servidor");
            response.put("mensaje", e.getMessage());
            response.put("ok", false);
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/lstUsers")
    public List<Login> lstUsers(){

        List<Login> lstTemp = this.ldao.getAllUsers();
        if( lstTemp.size() == 0 ) return new ArrayList<Login>();
        return lstTemp;

    }

}