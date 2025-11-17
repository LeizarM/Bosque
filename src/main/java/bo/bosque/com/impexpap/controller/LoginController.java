package bo.bosque.com.impexpap.controller;



import bo.bosque.com.impexpap.dao.IEmpleado;
import bo.bosque.com.impexpap.dao.ILoginDao;
import bo.bosque.com.impexpap.dao.IUsuarioBtn;
import bo.bosque.com.impexpap.dao.IVistaUsuario;
import bo.bosque.com.impexpap.model.Empleado;
import bo.bosque.com.impexpap.model.UsuarioBtn;
import bo.bosque.com.impexpap.model.VistaUsuario;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
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

    public LoginController(PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, JwtProvider jwtProvider, ILoginDao ldao, IVistaUsuario vistaUsuarioDao, IEmpleado empleadoDao, IUsuarioBtn usuarioBtnDao){
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtProvider = jwtProvider;
        this.ldao = ldao;
        this.vistaUsuarioDao = vistaUsuarioDao;
        this.empleadoDao = empleadoDao;
        this.usuarioBtnDao = usuarioBtnDao;
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