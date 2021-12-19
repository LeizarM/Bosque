package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.dao.ILoginDao;
import bo.bosque.com.impexpap.model.Login;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bosque")
public class LoginController {

    @Autowired()
    private ILoginDao ldao;

    /**
     * Procedimiento para listar el login del usuario
     */
    @PostMapping("/login")
    public Login login(@RequestBody Login obj   ) {
        //obj.getEmp().builder().codEmpleado(1111).build();
        obj.getEmp().setCodEmpleado( 98745 );

        System.out.println( obj.toString() );
        return this.ldao.obtainUser( obj.getLogin(), obj.getPassword() );
    }

}