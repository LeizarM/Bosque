package bo.bosque.com.impexpap.controller;



import bo.bosque.com.impexpap.dao.ILoginDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import bo.bosque.com.impexpap.security.jwt.JwtProvider;
import bo.bosque.com.impexpap.security.model.Jwt;
import bo.bosque.com.impexpap.model.Login;




@RestController
@CrossOrigin
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
     * EndPoint para listar el login del usuario
     * @return
     */
    @PostMapping("/login")
    public ResponseEntity<?>login(@RequestBody Login login, BindingResult bindingResult) {
        Map<String, Object> response = new HashMap<>();
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();  // extraemos la ip de donde se esta logueando

        //con este parametro
        //obtendra la informacion
        // del usuario                      // solo para la bitacora            // ip solo para la bitacora
        Login loginTemp = this.ldao.verifyUser(login.getUsername(),  passwordEncoder.encode( login.getPassword() ), request.getRemoteAddr());

        if( loginTemp.getCodUsuario() <= 0 ){
            response.put("error", "Error credenciales incorrectas");
            response.put("ok", false);
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        if(bindingResult.hasErrors()) return new ResponseEntity<>("campos mal puestos", HttpStatus.BAD_REQUEST);

        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken( login.getLogin(), login.getPassword() ) );
        SecurityContextHolder.getContext().setAuthentication( authentication );
        String jwt = jwtProvider.generateToken( authentication, loginTemp );
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        Jwt jwtT = new Jwt( jwt,loginTemp.getEmpleado().getPersona().getDatoPersona(), loginTemp.getEmpleado().getEmpleadoCargo().getCargoSucursal().getCargo().getDescripcion(), loginTemp.getTipoUsuario() , loginTemp.getCodUsuario(), loginTemp.getCodEmpleado() ,loginTemp.getCodEmpresa(), login.getLogin() ,userDetails.getAuthorities() );

        return new ResponseEntity<>(jwtT, HttpStatus.OK);

    }



}