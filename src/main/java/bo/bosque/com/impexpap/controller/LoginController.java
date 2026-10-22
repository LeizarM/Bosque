package bo.bosque.com.impexpap.controller;



import bo.bosque.com.impexpap.commons.SesionService;
import bo.bosque.com.impexpap.dao.IEmpleado;
import bo.bosque.com.impexpap.dao.ILoginDao;
import bo.bosque.com.impexpap.dao.IRelEmpEmpr;
import bo.bosque.com.impexpap.dao.IUsuarioBtn;
import bo.bosque.com.impexpap.dao.IVistaUsuario;
import bo.bosque.com.impexpap.model.Empleado;
import bo.bosque.com.impexpap.model.UsuarioBtn;
import bo.bosque.com.impexpap.model.VistaUsuario;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
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
import java.util.regex.Pattern;

import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.dto.CambioClaveDto;
import bo.bosque.com.impexpap.utils.ApiResponse;

import bo.bosque.com.impexpap.security.ClienteIp;
import bo.bosque.com.impexpap.security.jwt.DatosToken;
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
    private final IVistaUsuario vistaUsuarioDao;
    private final IEmpleado empleadoDao;
    private final IUsuarioBtn usuarioBtnDao;
    private final ClienteIp clienteIp;
    private final SesionService sesiones;
    private final IRelEmpEmpr relaciones;

    static final String MSG_CREDENCIALES =
            "Usuario o contraseña incorrectos. Después de 3 intentos fallidos en una hora el usuario se bloquea.";
    // Público porque JwtTokenFilter da el mismo motivo al cerrar la sesión abierta de un ex empleado.
    public static final String MSG_INACTIVO = "Usuario inactivo. Si crees que es un error, comunícate con Recursos Humanos.";
    public static final String MSG_BLOQUEADO =
            "Usuario bloqueado. Desbloquéalo con «¿Olvidaste tu contraseña?» o comunícate con un administrador.";
    static final String MSG_CLAVE_AJENA = "Solo un administrador puede cambiar la contraseña de otro usuario.";
    static final String MSG_SIN_BASE = "No se pudo conectar con la base de datos. Intenta de nuevo en unos minutos.";
    // La app los manda en el login y en el chequeo de sesión, para la pantalla Sesiones.
    static final String ENCABEZADO_PLATAFORMA = "X-Plataforma";
    static final String ENCABEZADO_VERSION = "X-Version-App";

    public LoginController(PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, JwtProvider jwtProvider, ILoginDao ldao, IVistaUsuario vistaUsuarioDao, IEmpleado empleadoDao, IUsuarioBtn usuarioBtnDao, ClienteIp clienteIp, SesionService sesiones, IRelEmpEmpr relaciones){
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtProvider = jwtProvider;
        this.ldao = ldao;
        this.vistaUsuarioDao = vistaUsuarioDao;
        this.empleadoDao = empleadoDao;
        this.usuarioBtnDao = usuarioBtnDao;
        this.clienteIp = clienteIp;
        this.sesiones = sesiones;
        this.relaciones = relaciones;
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
            // Paso 1: Verificar si el usuario existe y obtener su información.
            // Ya NO se le manda la contraseña al SP: no la usaba y viajaba en claro.
            Login loginTemp = this.ldao.verifyUser(
                    login.getUsername(),
                    clienteIp.de(request)
            );

            // Paso 2: Verificar diferentes estados de autenticación.
            if (loginTemp.getCodUsuario() == 0) {
                log.warn("Login rechazado (usuario inexistente): '{}' desde {}",
                        login.getUsername(), clienteIp.de(request));
                return credencialesInvalidas(response);
            }

            // Inactivo y bloqueado se informan sin validar la contraseña, por decisión del negocio:
            // a cambio, quien pruebe logins puede saber cuáles existen y en qué estado están.
            if (!loginTemp.isRelacionActiva()) {
                log.warn("Login rechazado (relación laboral inactiva): usuario '{}' desde {}",
                        login.getUsername(), clienteIp.de(request));
                return accesoDenegado(response, MSG_INACTIVO);
            }
            if (loginTemp.getCodUsuario() < 0) {
                log.warn("Login rechazado (cuenta bloqueada): usuario '{}' desde {}",
                        login.getUsername(), clienteIp.de(request));
                return accesoDenegado(response, MSG_BLOQUEADO);
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
                this.ldao.registerSuccessfulLogin(login.getUsername(), clienteIp.de(request));

                // La sesión (trec_sesion) viaja en el token; si no se pudo registrar, el token sale sin ella.
                long idSesion = sesiones.iniciar(loginTemp.getCodUsuario(), request.getHeader(ENCABEZADO_PLATAFORMA),
                        request.getHeader(ENCABEZADO_VERSION), clienteIp.de(request));
                String jwt = jwtProvider.generateToken(authentication, loginTemp, idSesion);
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
                        // Antes el login no mandaba el nombre de la sucursal (solo el
                        // código) — el panel de contexto de Arqueo/Coches/Caja Fuerte
                        // (Encargado/Cargo/Sucursal/Fecha) quedaba con "Sucursal" en
                        // blanco.
                        //
                        // OJO: no sirve bajar por getEmpleado()...getSucursal(): en
                        // Login todos los objetos anidados nacen con `= new X()`, así
                        // que esa rama es no-nula pero VACÍA y devuelve "" sin fallar.
                        // LoginDaoImpl.verifyUser mapea la columna 5 del SP a
                        // login.nombreSucursal (plano), no al grafo de objetos — solo
                        // el cargo (columna 8) va al grafo. Se lee de donde el DAO
                        // realmente escribe.
                        loginTemp.getNombreSucursal(),
                        userDetails.getAuthorities()
                );
                jwtT.setIniciales(inicialesDe(loginTemp.getCodEmpleado()));

                // Respuesta exitosa
                response.put("data", jwtT);
                response.put("mensaje", "Inicio de sesión exitoso");
                response.put("status", "success");
                return new ResponseEntity<>(response, HttpStatus.OK);

            } catch (BadCredentialsException e) {
                // Contraseña incorrecta - Registrar intento fallido
                log.warn("Login rechazado (contraseña incorrecta): usuario '{}' desde {}",
                        login.getUsername(), clienteIp.de(request));

                // Si este intento bloqueó la cuenta se avisa ya, como se haría en el siguiente.
                Login updatedLogin = this.ldao.registerFailedAttempt(login.getUsername(), clienteIp.de(request));
                if (updatedLogin != null && updatedLogin.getCodUsuario() < 0) {
                    log.warn("La cuenta '{}' quedó bloqueada por intentos fallidos", login.getUsername());
                    return accesoDenegado(response, MSG_BLOQUEADO);
                }

                return credencialesInvalidas(response);
            } catch (DisabledException | LockedException e) {
                // El estado cambió entre la verificación del paso 1 y esta (RR. HH. o un administrador, justo ahora).
                return accesoDenegado(response, e instanceof DisabledException ? MSG_INACTIVO : MSG_BLOQUEADO);
            } catch (Exception e) {
                if (esFallaDeBase(e)) return sinBase(response, e);
                // Otros errores durante la autenticación
                log.error("Error durante la autenticación: " + e.getMessage());
                response.put("mensaje", "Error en el proceso de autenticación");
                response.put("status", "error");
                return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
            }

        } catch (Exception e) {
            if (esFallaDeBase(e)) return sinBase(response, e);
            // Error general en el proceso
            log.error("Error en el proceso de login: " + e.getMessage());
            response.put("mensaje", "Error en el servicio de autenticación");
            response.put("status", "error");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /** Base caída o lenta, también cuando Spring Security la envuelve (InternalAuthenticationServiceException). */
    private static boolean esFallaDeBase(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof DataAccessResourceFailureException || t instanceof TransientDataAccessException) return true;
        }
        return false;
    }

    /** 503 en vez de culpar a la contraseña o dar un error genérico. */
    private ResponseEntity<?> sinBase(Map<String, Object> response, Exception e) {
        log.error("Login sin base de datos: {}", e.getMessage());
        response.put("mensaje", MSG_SIN_BASE);
        response.put("status", "error");
        return new ResponseEntity<>(response, HttpStatus.SERVICE_UNAVAILABLE);
    }

    /** Mismo 401 para usuario inexistente y contraseña errada: entre esos dos no se distingue. */
    private ResponseEntity<?> credencialesInvalidas(Map<String, Object> response) {
        response.put("mensaje", MSG_CREDENCIALES);
        response.put("status", "error");
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    /** Cuenta inactiva o bloqueada: 403 con el motivo, que el frontend muestra tal cual. */
    private ResponseEntity<?> accesoDenegado(Map<String, Object> response, String mensaje) {
        response.put("mensaje", mensaje);
        response.put("status", "error");
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    /** Las iniciales del avatar no deben tumbar el login: si fallan, la app las toma del nombre. */
    private String inicialesDe(int codEmpleado) {
        if (codEmpleado <= 0) return "";
        try {
            String iniciales = relaciones.inicialesDe(codEmpleado);
            return iniciales == null ? "" : iniciales;
        } catch (RuntimeException e) {
            log.warn("Login sin iniciales del empleado {}: {}", codEmpleado, e.getMessage());
            return "";
        }
    }

    /**
     * Chequeo periódico de la app: JwtTokenFilter responde 401 SESION_CERRADA si la sesión ya no vale; si vale, aquí
     * se anota la señal. Es público (/auth/**) para que un token vencido o una falla de la base no cierren sesiones.
     */
    @PostMapping("/sesion")
    public ResponseEntity<Void> verificarSesion(Authentication auth, HttpServletRequest request) {
        sesiones.senal(DatosToken.deONull(auth), request.getHeader(ENCABEZADO_PLATAFORMA), request.getHeader(ENCABEZADO_VERSION),
                clienteIp.de(request));
        return ResponseEntity.noContent().build();
    }

    /** «Cerrar sesión» en la app: la sesión queda cerrada en el servidor y su token ya no sirve. */
    @PostMapping("/salir")
    public ResponseEntity<Void> salir(Authentication auth) {
        sesiones.salir(DatosToken.deONull(auth));
        return ResponseEntity.noContent().build();
    }

    /** Si otro (un administrador) le cambió la clave, se cierran todas sus sesiones; si la cambió él, salvo la actual. */
    private void cerrarSesionesTrasCambioDeClave(long codUsuario, DatosToken quien) {
        if (quien == null) {
            sesiones.cerrarTodas(codUsuario, SesionService.MOTIVO_CLAVE, null);
            return;
        }
        long excepto = quien.getCodUsuario() == codUsuario ? quien.getIdSesion() : 0;
        sesiones.cerrarTodas(codUsuario, SesionService.MOTIVO_CLAVE, Long.valueOf(quien.getCodUsuario()), excepto);
    }

    /**
     * Serv. para cambiar la contraseña
     * @param login
     * @return
     */
    @PostMapping("/changePassword")
    public ResponseEntity<?>login(@RequestBody Login login, Authentication auth) {
        Map<String, Object> response = new HashMap<>();
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();  // extraemos la ip de donde se esta logueando
        String ip = clienteIp.de(request);

        // Antes esta llamada mandaba passwordEncoder.encode(...) como @password2 del
        // SP: un hash BCrypt recién generado, que no coincide con nada guardado. Que
        // este endpoint funcionara igual es la prueba de que el SP nunca verificó ese
        // parámetro. Ahora directamente no se manda. Quien verifica es el
        // authenticationManager de tres líneas más abajo, como siempre.
        Login loginTemp = this.ldao.verifyUser(login.getUsername(), ip);

        if( loginTemp.getCodUsuario() == 0 ){
            response.put("error", "Error al reconocer el usuario");
            response.put("ok", false);
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        // Sin esto, authenticate lanzaba DisabledException y el ex empleado veía un error 500.
        if (!loginTemp.isRelacionActiva()) {
            response.put("error", MSG_INACTIVO);
            response.put("ok", false);
            return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
        }
        if (loginTemp.getCodUsuario() < 0) {
            response.put("error", MSG_BLOQUEADO);
            response.put("ok", false);
            return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
        }
        // Es público: una clave errada cuenta como intento fallido, igual que en el login (3 y se bloquea).
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken( login.getLogin(), login.getPassword2() ) );
        } catch (BadCredentialsException e) {
            Login tras = this.ldao.registerFailedAttempt(login.getUsername(), ip);
            boolean bloqueado = tras != null && tras.getCodUsuario() < 0;
            response.put("error", bloqueado ? MSG_BLOQUEADO : MSG_CREDENCIALES);
            response.put("ok", false);
            return new ResponseEntity<>(response, bloqueado ? HttpStatus.FORBIDDEN : HttpStatus.UNAUTHORIZED);
        } catch (DisabledException | LockedException e) {
            response.put("error", e instanceof DisabledException ? MSG_INACTIVO : MSG_BLOQUEADO);
            response.put("ok", false);
            return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
        }
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
        cerrarSesionesTrasCambioDeClave(loginTemp.getCodUsuario(), DatosToken.deONull(auth));
        response.put("msg", "Constraseña actualizada correctamente");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);

    }


    /** La regla de la pantalla tbUsuario/cpassword del legacy: 6 a 15 de estos caracteres. */
    private static final Pattern CLAVE_VALIDA =
            Pattern.compile("^[a-zA-Z0-9ñÑ '@(){}\\[\\]_<>~]{6,15}$");

    /**
     * Cambio de la propia contraseña desde el Dashboard (tbUsuario/cpassword). El usuario sale del token y
     * una clave actual errada responde 400, no 401: la app cierra la sesión ante cualquier 401. Los intentos
     * fallidos se cuentan como en el login y, al cambiarla, se cierran las demás sesiones.
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/cambiarClave")
    public ResponseEntity<ApiResponse<?>> cambiarClave(
            @RequestBody CambioClaveDto datos, Authentication auth,
            HttpServletRequest request) {
        String nueva = datos.getNueva() == null ? "" : datos.getNueva();
        if (!nueva.equals(datos.getConfirmacion())) {
            throw new SpBusinessException(
                    "La nueva contraseña y su confirmación no coinciden.");
        }
        if (!CLAVE_VALIDA.matcher(nueva).matches()) {
            throw new SpBusinessException(
                    "La nueva contraseña debe tener entre 6 y 15 caracteres: letras, números, espacio o @ ( ) { } [ ] _ < > ~ '.");
        }
        if (nueva.equals(datos.getActual())) {
            throw new SpBusinessException(
                    "La nueva contraseña debe ser distinta de la actual.");
        }

        String login = auth.getName();
        String ip = clienteIp.de(request);
        Login usuario = this.ldao.verifyUser(login, ip);
        if (usuario == null || usuario.getCodUsuario() == 0) {
            throw new SpBusinessException(
                    "No se pudo identificar su usuario. Cierre sesión y vuelva a entrar.");
        }
        if (usuario.getCodUsuario() < 0) throw new SpBusinessException(MSG_BLOQUEADO);
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(login, datos.getActual()));
        } catch (BadCredentialsException e) {
            Login tras = this.ldao.registerFailedAttempt(login, ip);
            boolean bloqueado = tras != null && tras.getCodUsuario() < 0;
            throw new SpBusinessException(bloqueado ? MSG_BLOQUEADO
                    : "La contraseña actual no es correcta. Después de 3 intentos fallidos en una hora el usuario se bloquea.");
        } catch (DisabledException | LockedException e) {
            throw new SpBusinessException(
                    e instanceof DisabledException ? MSG_INACTIVO : MSG_BLOQUEADO);
        }

        usuario.setPassword2(passwordEncoder.encode(nueva));
        if (!this.ldao.abmLogin(usuario, "Q")) {
            throw new SpBusinessException(
                    "No se pudo guardar la nueva contraseña. Intente de nuevo.");
        }
        cerrarSesionesTrasCambioDeClave(usuario.getCodUsuario(), DatosToken.deONull(auth));
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(
                "Contraseña actualizada. Las demás sesiones abiertas con su usuario se cerraron.",
                usuario.getCodUsuario(), HttpStatus.CREATED.value()));
    }

    /**
     * Serv. para cambiar la contraseña que aparece por default
     * @param datos
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/changePasswordDefault")
    public ResponseEntity<?> changePasswordDefault(@RequestBody Map<String, Object> datos, Authentication auth) {
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
            // ROLE_LIM es todo el que no es de Sistemas: solo cambia su propia clave (la por defecto).
            DatosToken quien = DatosToken.deONull(auth);
            boolean esAdministrador = auth != null && auth.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_ADM".equals(a.getAuthority()));
            if (!esAdministrador && (quien == null || quien.getCodUsuario() != codUsuario)) {
                response.put("error", MSG_CLAVE_AJENA);
                response.put("ok", false);
                return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
            }

            // Crear objeto Login con información mínima necesaria
            Login login = new Login();
            login.setCodUsuario(codUsuario);
            login.setPassword2(passwordEncoder.encode( npassword ));



            if(!this.ldao.abmLogin(login, "Q")) {
                response.put("error", "Error al actualizar las credenciales");
                response.put("ok", false);
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            cerrarSesionesTrasCambioDeClave(codUsuario, quien);

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

    /**
     * Listara los usuarios
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/lstUsers")
    public List<Login> lstUsers(){

        List<Login> lstTemp = this.ldao.getAllUsers();
        if( lstTemp.size() == 0 ) return new ArrayList<Login>();
        return lstTemp;

    }

    /**
     * Procedimiento para copiar los permisos de un usuario a otro
     * @param
     * @return
     */
    @Secured ( { "ROLE_ADM", "ROLE_LIM" }  )
    @PostMapping("/registroVistaUsuario")
    public ResponseEntity<?> registrarVistaUsuario( @RequestBody VistaUsuario vu ){

        Map<String, Object> response = new HashMap<>();


        if( !this.vistaUsuarioDao.registrarVistaUsuario( vu, "E" ) ){
            response.put("msg", "Error al Actualizar el Vista Usuario");
            response.put("error", "ok");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        response.put("msg", "Datos Actualizados");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }


    /**
     * Procedimiento para registrar/actualizar los datos de un usuario
     * @param
     * @return
     */
    @Secured ( { "ROLE_ADM", "ROLE_LIM" }  )
    @PostMapping("/registroUsuario")
    public ResponseEntity<?> registrarUsuario( @RequestBody Login l ){

        Map<String, Object> response = new HashMap<>();


        String acc = "U";
        if( l.getCodUsuario() == 0){
            acc = "I";
            l.setPassword(l.getPassword2());
            l.setPassword2(passwordEncoder.encode( l.getPassword2() )); // Encriptamos la contraseña

        }


        if( !this.ldao.abmLogin( l, acc ) ){
            response.put("msg", "Error al Actualizar el Usuario");
            response.put("error", "ok");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        response.put("msg", "Datos Actualizados");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }


    /**
     * Listara los usuarios
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/lstEmpleados")
    public List<Empleado> lstEmpleados(){

        List<Empleado> lstTemp = this.empleadoDao.lisEmpleados();
        if( lstTemp.size() == 0 ) return new ArrayList<Empleado>();
        return lstTemp;

    }

    /**
     * Para verificar si un usuario existe o existe duplicidad
     * @param l
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/verificarDuplicadoUsuario")
    public int verificarDuplicado( @RequestBody Login l ){

        int doble = 0;

        if (ldao.verifDuplicidad(l, "R") > 0) {
            doble = 1;
        } else {
            if (ldao.verifDuplicidad(l,"G") > 0) {
                doble = 2;
            }
        }
        return doble;

    }


    /**
     * Devuelve la estructura de árbol jerárquica para permisos de usuario
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/lstUsuarioPermisosTree")
    public Map<String, Object> lstUsuarioPermisosTree(@RequestBody VistaUsuario vu) {

        List<VistaUsuario> lstPermisos = this.vistaUsuarioDao.lstPermisosVistasYBotones(vu.getCodUsuario());

        Map<String, Object> response = new LinkedHashMap<>();

        if (lstPermisos.isEmpty()) {
            response.put("success", false);
            response.put("message", "No se encontraron permisos para el usuario");
            response.put("data", new ArrayList<>());
            return response;
        }


        // Construir el árbol jerárquico
        List<Map<String, Object>> arbolPermisos = construirArbolPermisos(lstPermisos);

        response.put("success", true);
        response.put("message", "Permisos cargados correctamente");

        response.put("data", arbolPermisos);

        return response;
    }

    /**
     * Construye la estructura de árbol jerárquica recursivamente
     * Los botones ahora se agregan correctamente usando codVistaPadre
     */
    private List<Map<String, Object>> construirArbolPermisos(List<VistaUsuario> lstPermisos) {

        // Separar vistas/módulos de botones
        Map<Integer, Map<String, Object>> nodosVistasMap = new LinkedHashMap<>();
        List<Map<String, Object>> listaBotones = new ArrayList<>();

        // PASO 1: Procesar todos los registros
        for (VistaUsuario permiso : lstPermisos) {

            Integer codVista = permiso.getCodVista();
            Integer codBoton = permiso.getCodBoton();

            // Si es un botón (codBoton > 0)
            if (codBoton != null && codBoton > 0) {



                // Crear nodo botón
                Map<String, Object> nodoBoton = crearNodoPermiso(permiso, "Boton");
                listaBotones.add(nodoBoton);

            } else {
                // Es módulo o vista - evitar duplicados
                if (!nodosVistasMap.containsKey(codVista)) {

                    String tipo = (permiso.getCodVistaPadre() == 0) ? "Modulo" : "Vista";

                    Map<String, Object> nodoVista = crearNodoPermiso(permiso, tipo);
                    nodoVista.put("children", new ArrayList<Map<String, Object>>());

                    nodosVistasMap.put(codVista, nodoVista);
                }
            }
        }

        // PASO 2: Construir jerarquía de vistas primero (recursivo)
        List<Map<String, Object>> nodosRaiz = new ArrayList<>();

        for (Map<String, Object> nodo : nodosVistasMap.values()) {
            Integer codVistaPadre = (Integer) nodo.get("codVistaPadre");

            if (codVistaPadre == null || codVistaPadre == 0) {
                // Es un nodo raíz (módulo principal)
                nodosRaiz.add(nodo);
            } else {
                // Tiene padre - buscarlo y agregarlo como hijo
                Map<String, Object> nodoPadre = nodosVistasMap.get(codVistaPadre);

                if (nodoPadre != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> hijos = (List<Map<String, Object>>) nodoPadre.get("children");
                    hijos.add(nodo);
                } else {
                    // Si no encuentra el padre, agregar como raíz (fallback)
                    System.out.println("ADVERTENCIA: No se encontró padre codVistaPadre=" + codVistaPadre +
                            " para codVista=" + nodo.get("codVista"));
                    nodosRaiz.add(nodo);
                }
            }
        }

        // PASO 3: Agregar botones a sus vistas correspondientes usando codVistaPadre
        int botonesAgregados = 0;
        for (Map<String, Object> boton : listaBotones) {
            Integer codVistaPadre = (Integer) boton.get("codVistaPadre");

            // ✅ CORRECTO: Usar codVistaPadre para encontrar la vista padre del botón
            Map<String, Object> vistaNode = nodosVistasMap.get(codVistaPadre);

            if (vistaNode != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> children = (List<Map<String, Object>>) vistaNode.get("children");
                children.add(boton);
                botonesAgregados++;
            } else {
                System.out.println("ADVERTENCIA: No se encontró vista padre codVistaPadre=" + codVistaPadre +
                        " para botón codVista=" + boton.get("codVista") +
                        ", codBoton=" + boton.get("codBoton"));
            }
        }



        // PASO 4: Ordenar nodos raíz por codVista
        nodosRaiz.sort((a, b) -> {
            Integer codA = (Integer) a.get("codVista");
            Integer codB = (Integer) b.get("codVista");
            return codA.compareTo(codB);
        });

        return nodosRaiz;
    }

    /**
     * Crea un nodo individual del árbol de permisos
     */
    private Map<String, Object> crearNodoPermiso(VistaUsuario permiso, String tipo) {
        Map<String, Object> nodo = new LinkedHashMap<>();

        nodo.put("codVista", permiso.getCodVista());
        nodo.put("codVistaPadre", permiso.getCodVistaPadre());
        nodo.put("codBoton", permiso.getCodBoton() != 0 ? permiso.getCodBoton() : 0);
        nodo.put("direccion", permiso.getDireccion());
        nodo.put("nombreComponente", permiso.getNombreComponente());
        nodo.put("descripcion", permiso.getDescripcion());
        nodo.put("imagen", permiso.getImagen());
        nodo.put("nivelAcceso", permiso.getNivelAcceso());
        nodo.put("nivelAccesoBoton", permiso.getNivelAccesoBoton());
        nodo.put("autorizador", permiso.getAutorizador());
        nodo.put("codUsuario", permiso.getCodUsuario());
        nodo.put("tipo", tipo);

        // Los botones no tienen hijos
        if ("Boton".equals(tipo)) {
            nodo.put("children", new ArrayList<>());
        }

        return nodo;
    }


    /**
     * Procedimiento para actualizar los permisos de un usuario
     * @param
     * @return
     */
    @Secured ( { "ROLE_ADM", "ROLE_LIM" }  )
    @PostMapping("/actualizarPermisos")
    public ResponseEntity<?> actualizarPermisos( @RequestBody VistaUsuario vu ){

        Map<String, Object> response = new HashMap<>();



        if(vu.getTipo().equals("Modulo") || vu.getTipo().equals("Vista") ){

            if( !this.vistaUsuarioDao.registrarVistaUsuario( vu, "U" ) ){
                response.put("msg", "Error al Actualizar los Permisos del Usuario");
                response.put("error", "ok");
                return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        else if( vu.getTipo().equals("Boton") ){
            UsuarioBtn temp = new UsuarioBtn();
            temp.setNivelAcceso( vu.getNivelAcceso() );
            temp.setAudUsuario(vu.getAudUsuarioI());
            temp.setCodUsuario(vu.getCodUsuario() );
            temp.setCodBtn( vu.getCodBoton() );

            if(!this.usuarioBtnDao.registroBoton( temp, "U" ) ){
                response.put("msg", "Error al Actualizar los Permisos del Botón");
                response.put("error", "ok");
                return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }



        response.put("msg", "Datos Actualizados");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

}