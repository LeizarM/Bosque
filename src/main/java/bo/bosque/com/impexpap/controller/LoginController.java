package bo.bosque.com.impexpap.controller;



import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.List;
import java.util.Objects;

import bo.bosque.com.impexpap.security.jwt.JwtProvider;
import bo.bosque.com.impexpap.security.model.Jwt;
import bo.bosque.com.impexpap.dao.ILoginDao;
import bo.bosque.com.impexpap.model.Login;


@RestController
@CrossOrigin
@RequestMapping("/auth")
@Slf4j
public class LoginController {

    @Autowired()
    private ILoginDao ldao;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    JwtProvider jwtProvider;

    /**
     * Procedimiento para listar el login del usuario
     * @return
     */
    @PostMapping("/login")
    public ResponseEntity login(@RequestBody Login obj, BindingResult bindingResult) {

        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();  // extraemos la ip de donde se esta logueando
        if(bindingResult.hasErrors()) return new ResponseEntity("campos mal puestos", HttpStatus.BAD_REQUEST);
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken( obj.getLogin(), obj.getPassword() ) );
        SecurityContextHolder.getContext().setAuthentication( authentication );
        String jwt = jwtProvider.generateToken( authentication );
        UserDetails userDetails = ( UserDetails )authentication.getPrincipal();

                                                //con este parametro
                                                //obtendra la informacion
                                                // del usuario                      // solo para la bitacora            // ip solo para la bitacora
        Login loginTemp = this.ldao.verifyUser(userDetails.getUsername(),  passwordEncoder.encode( obj.getPassword() ), request.getRemoteAddr());

        if ( loginTemp.getCodUsuario() <= 0) return new ResponseEntity("campos mal puestos", HttpStatus.BAD_REQUEST);

        log.info( loginTemp.toString() );

        Jwt jwtT = new Jwt( jwt, loginTemp.getEmpleado().getPersona().getDatoPersona(), loginTemp.getEmpleado().getCargo().getDescripcion(), loginTemp.getTipoUsuario(), loginTemp.getCodUsuario() ,userDetails.getAuthorities() );
        return new ResponseEntity(jwtT, HttpStatus.OK);

    }



}