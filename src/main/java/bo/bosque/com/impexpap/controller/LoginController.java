package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.dao.ILoginDao;
import bo.bosque.com.impexpap.model.Login;
import bo.bosque.com.impexpap.model.Persona;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@RestController
@CrossOrigin
@RequestMapping("/bosque")
public class LoginController {

    @Autowired()
    private ILoginDao ldao;

    /**
     * Procedimiento para listar el login del usuario
     */
    @PostMapping("/login")
    public Login login( @RequestBody Login obj ) {

        // extraemos la ip de donde se esta logueando
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        System.out.println("el ip de acceso es = "+ request.getRemoteAddr());

        //System.out.println( obj.toString() );
        return this.ldao.verifyUser( obj.getLogin(), obj.getPassword() );
    }

}