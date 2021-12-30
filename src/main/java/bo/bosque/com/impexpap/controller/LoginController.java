package bo.bosque.com.impexpap.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import javax.servlet.http.HttpServletRequest;
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
        /**
         * Nota: la lista recursiva tiene que devolverlo  ordenado de forma ascendente
         * desde el nivel mas profundo hasta el nivel mas externo
         */
        List<Vista>  lstMenu = this.vdao.obtainMenuXUser( obj.getCodUsuario() );
        // Generando el menu en forma de arbol
        for ( int i = 0; i < lstMenu.size(); i++ ) {
            while (lstMenu.get(i).getCodVistaPadre() > 0) {
                System.out.println( "en while el codvista "+ lstMenu.get(i).getCodVista()+" el codVistaPadre es= "+lstMenu.get(i).getCodVistaPadre()+" la raiz es "+ lstMenu.get(i).getEsRaiz()   );
                for (int j = 0; j < lstMenu.size(); j++) {
                    if ( lstMenu.get(j).getCodVista() == lstMenu.get(i).getCodVistaPadre() ) {
                        lstMenu.get(j).getItems().add(lstMenu.get(i));
                        lstMenu.remove(lstMenu.get(i));//Eliminamos el hijo una vez agregado al padre, para evitar duplicidad
                        break;
                    }
                }
            }
        }
        /**
         * Usando recursividad para eliminar listas vacias de los hijos mas profundos de los nodos
         */


        return lstMenu;
    }

}