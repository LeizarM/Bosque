package bo.bosque.com.impexpap.controller;


import bo.bosque.com.impexpap.dao.IVistaDao;
import bo.bosque.com.impexpap.model.Login;
import bo.bosque.com.impexpap.model.Vista;
import bo.bosque.com.impexpap.security.jwt.JwtProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin("*")
@RequestMapping("/view")
public class VistaController {

    @Autowired()
    private IVistaDao vdao;

    @Autowired
    JwtProvider jwtProvider;


    /**
     * Procedimiento para obtener el menu dinamico por usuario
     * @param obj
     * @return JWT
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" }) //que un usuario admin o limitado si tiene acceso para consumir este recurso
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
     * @return List
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" }) //que un usuario admin o limitado si tiene acceso para consumir este recurso
    @PostMapping("/routes")
    public List<Vista> obtenerRutas( @RequestBody Login obj ) {
        return this.vdao.obtainRoutes( obj.getCodUsuario() );
    }
}
