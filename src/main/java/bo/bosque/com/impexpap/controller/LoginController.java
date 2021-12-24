package bo.bosque.com.impexpap.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import bo.bosque.com.impexpap.dao.ILoginDao;
import bo.bosque.com.impexpap.dao.IVistaDao;
import bo.bosque.com.impexpap.model.Login;
import bo.bosque.com.impexpap.model.Vista;


@RestController
@CrossOrigin
@RequestMapping("/login")
public class LoginController {

    @Autowired()
    private ILoginDao ldao;
    @Autowired()
    private IVistaDao vdao;

    /**
     * Procedimiento para listar el login del usuario
     */
    @PostMapping("/login")
    public Login login( @RequestBody Login obj ) {

        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();  // extraemos la ip de donde se esta logueando
        return this.ldao.verifyUser( obj.getLogin(), obj.getPassword(), request.getRemoteAddr() );

    }

    /**
     * Procedimiento para obtener el menu dinamico por usuario
     */
    @PostMapping("/vistaDinamica")
    public List<Vista> obtenerMenuDinamico( @RequestBody Login obj ) {
        List<Vista> lstMenu;
        List<List<Vista>> lstTemp = new ArrayList<List<Vista>>();
        System.out.println("El tamaño de la lista Temp antes de agregar elementos "+lstTemp.size());
        lstMenu = this.vdao.obtainMenuXUser( obj.getCodUsuario() );
        System.out.println("****************************************************************************");
        lstTemp.add(lstMenu);
        System.out.println("El tamaño de la lista Temp despues de agregar elementos una vez "+lstTemp.size());
        lstTemp.add(lstMenu);
        System.out.println("El tamaño de la lista Temp antes de agregar elementos segunda vez  "+lstTemp.size());
        System.out.println( lstTemp.size() );
        System.out.println("El tamaño de la lista Temp total "+lstTemp.size());
        return null;

    }

}