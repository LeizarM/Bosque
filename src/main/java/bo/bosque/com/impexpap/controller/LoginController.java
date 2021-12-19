package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.dao.ILoginDao;
import bo.bosque.com.impexpap.model.Login;
import bo.bosque.com.impexpap.model.Persona;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
    public Login login(@RequestBody Login obj   ) {



        //System.out.println( obj.toString() );
        return this.ldao.obtainUser( obj.getLogin(), obj.getPassword() );
    }

}