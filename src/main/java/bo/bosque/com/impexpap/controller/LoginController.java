package bo.bosque.com.impexpap.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import bo.bosque.com.impexpap.dao.ILoginDao;
import bo.bosque.com.impexpap.dao.IVistaDao;
import bo.bosque.com.impexpap.model.Login;
import bo.bosque.com.impexpap.model.Vista;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import static jdk.nashorn.internal.objects.NativeArray.forEach;


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
        /**
         * Nota: la lista recursiva tiene que estar ordenada de forma ascendente
         * desde el nivel mas profundo hasta el nivel mas externo
         */
        List<Vista>  lstMenu = this.vdao.obtainMenuXUser( obj.getCodUsuario() );
        LinkedList<Vista> items = new LinkedList<Vista>();
        for ( Vista i  : lstMenu ) items.add(i);

        for ( int i = 0; i < items.size(); i++ ){
            while (  items.get(i).getCodVistaPadre() > 0 ){
                for ( int j = 0; j < items.size(); j++ ){
                    if ( items.get(j).getCodVista() == items.get(i).getCodVistaPadre() ){
                        items.get(j).getItems().add( items.get(i) );
                        //Eliminamos el hijo una vez agregado al padre, para evitar duplicidad
                        items.remove( items.get(i) );
                        break;
                    }
                }
            }
        }
        return items;
    }

}