package bo.bosque.com.impexpap.controller;



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
import bo.bosque.com.impexpap.dao.IVistaDao;
import bo.bosque.com.impexpap.model.Login;
import bo.bosque.com.impexpap.model.Vista;


@RestController
@CrossOrigin
@RequestMapping("/auth")
public class LoginController {

    @Autowired()
    private ILoginDao ldao;
    @Autowired()
    private IVistaDao vdao;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    JwtProvider jwtProvider;

    /**
     * Procedimiento para listar el login del usuario
     */
    @PostMapping("/login")
    public ResponseEntity<Jwt> login( @RequestBody Login obj,  BindingResult bindingResult) {

        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();  // extraemos la ip de donde se esta logueando
        //return this.ldao.verifyUser( obj.getLogin(), obj.getPassword(), request.getRemoteAddr() );
        if(bindingResult.hasErrors())
            return new ResponseEntity("campos mal puestos", HttpStatus.BAD_REQUEST);
        Authentication authentication =
                authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(obj.getLogin(), obj.getPassword()));
        SecurityContextHolder.getContext().setAuthentication( authentication );
        String jwt = jwtProvider.generateToken( authentication );
        UserDetails userDetails = ( UserDetails )authentication.getPrincipal();
        Jwt jwtT = new Jwt( jwt, obj.getEmpleado().getPersona().getDatoPersona(), "Cargo", "adm", 34 ,userDetails.getAuthorities() );
        return new ResponseEntity(jwtT, HttpStatus.OK);

    }

    /**
     * Procedimiento para obtener el menu dinamico por usuario
     */
    @PostMapping("/vistaDinamica")
    public List<Vista> obtenerMenuDinamico( @RequestBody Login obj ) {
        /**
         * Nota: la lista recursiva tiene que devolverlo  ordenado de forma ascendente
         * desde el nivel mas profundo hasta el nivel mas externo
         */
        List<Vista>  lstMenu = this.vdao.obtainMenuXUser( obj.getCodUsuario() );
        // Generando el menu en forma de arbol
        for ( int i = 0; i < lstMenu.size(); i++ ) {
            while (lstMenu.get(i).getCodVistaPadre() > 0) {
                for (int j = 0; j < lstMenu.size(); j++) {
                    if ( lstMenu.get(j).getCodVista() == lstMenu.get(i).getCodVistaPadre() ) {
                        if( lstMenu.get(i).getTieneHijo() == -1 ){
                            lstMenu.get(i).setItems(null)
                                    .setRouterLink( lstMenu.get(i).getDireccion() )
                                    .setIcon( "pi pi-circle" );
                        }
                        lstMenu.get(j).getItems().add(lstMenu.get(i));
                        lstMenu.remove(lstMenu.get(i));//Eliminamos el hijo una vez agregado al padre, para evitar duplicidad
                        break;
                    }
                }
            }
        }
        return lstMenu;
    }

    /**
     * Procedimiento para obtener las rutas de las paginas por usuario, pero solo de los hijos del menu
     * ****** SE DEJARA ESTE METODO EN CASO DE QUE SE LLEGARA A NECESITAR
     * @param obj
     * @return
     */
    @PostMapping("/routes")
    public List<Vista> obtenerRutas( @RequestBody Login obj ) {
        return this.vdao.obtainRoutes( obj.getCodUsuario() );
    }

}