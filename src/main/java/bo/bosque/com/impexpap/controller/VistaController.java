package bo.bosque.com.impexpap.controller;


import bo.bosque.com.impexpap.dao.IUsuarioBtn;
import bo.bosque.com.impexpap.dao.IVistaDao;
import bo.bosque.com.impexpap.model.Login;
import bo.bosque.com.impexpap.model.UsuarioBtn;
import bo.bosque.com.impexpap.model.Vista;
import bo.bosque.com.impexpap.security.jwt.DatosToken;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.text.Collator;
import java.util.*;

@RestController
@CrossOrigin("*")
@RequestMapping("/view")
public class VistaController {


    private final IVistaDao vdao;
    private final IUsuarioBtn uDao;

    public VistaController(IVistaDao vdao, IUsuarioBtn uDao) {
        this.vdao = vdao;
        this.uDao = uDao;
    }

    /**
     * Procedimiento para obtener el menu dinamico por usuario.
     *
     * <p>Mismo arreglo que {@link #obtenerPermisosBotones}: el {@code codUsuario}
     * sale del token. Tenia el mismo agujero —el menu de otro usuario se pedia
     * cambiando un numero del body— y no tiene sentido tapar uno y dejar el
     * otro, porque los dos describen los permisos de la misma persona.
     */
        @Secured({ "ROLE_ADM", "ROLE_LIM" })
        @PostMapping("/vistaDinamica")
    public List<Vista> obtenerMenuDinamico(@RequestBody(required = false) Login obj,
                                           Authentication auth) {
        List<Vista> flat = this.vdao.obtainMenuXUser(DatosToken.codUsuarioDe(auth));



        // Index por id
        Map<Integer, Vista> byId = new HashMap<>();
        for (Vista v : flat) {
            byId.put(v.getCodVista(), v);
            if (v.getItems() == null && v.getTieneHijo() != -1) {
                v.setItems(new ArrayList<>());
            }
        }

        // Construir árbol
        List<Vista> roots = new ArrayList<>();
        for (Vista v : flat) {
            if (v.getCodVistaPadre() > 0) {
                Vista parent = byId.get(v.getCodVistaPadre());
                if (parent != null) {
                    if (v.getTieneHijo() == -1) {
                        v.setItems(null)
                                .setRouterLink(v.getDireccion())
                                .setIcon("pi pi-circle");
                    } else if (v.getItems() == null) {
                        v.setItems(new ArrayList<>());
                    }
                    if (parent.getItems() == null) parent.setItems(new ArrayList<>());
                    parent.getItems().add(v);
                } else {
                    // si por algún motivo el padre no viene, lo tratamos como raíz
                    roots.add(v);
                }
            } else {
                roots.add(v);
            }
        }

        

        // Ordenar recursivamente por título (hijos primero, luego el nivel actual)
        sortTreeByTitle(roots);

        return roots;
    }


    /**
     * Procedimiento para obtener los permisos por usuario por boton.
     *
     * <p><b>El {@code codUsuario} sale del token, no del body.</b> Antes se leia
     * de {@code obj.getCodUsuario()}, o sea que cualquier usuario autenticado
     * —incluido un {@code ROLE_LIM}— pedia el ACL de <b>cualquier otro</b> con
     * solo cambiar el numero, y con eso enumeraba los permisos de los 134
     * usuarios de {@code tb_usuario}. Es el reconocimiento previo a una
     * escalada de privilegios, y ya estaba senalado como "el precedente malo"
     * en el javadoc de {@code AccesoModuloHelper}.
     *
     * <p>El body dejo de leerse. Se sigue aceptando —{@code required=false}—
     * para no romper al frontend, que todavia lo manda; cuando deje de hacerlo
     * se puede borrar el parametro.
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" }) //que un usuario admin o limitado si tiene acceso para consumir este recurso
    @PostMapping("/vistaBtn")
    public List<UsuarioBtn> obtenerPermisosBotones( @RequestBody(required = false) Login obj,
                                                    Authentication auth ) {

        List<UsuarioBtn> lstPermisos = this.uDao.botonesXUsuario( DatosToken.codUsuarioDe(auth) );

//        if (lstPermisos.isEmpty()) {
//            throw new RuntimeException("No hay permisos asociados al usuario.");
//        }
        return lstPermisos;
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


    private void sortTreeByTitle(List<Vista> nodes) {
        if (nodes == null) return;
        Collator coll = Collator.getInstance(new Locale("es"));
        coll.setStrength(Collator.PRIMARY);
        for (Vista v : nodes) sortTreeByTitle(v.getItems());
        nodes.sort((a, b) -> coll.compare(titleOf(a), titleOf(b)));
    }

    private String titleOf(Vista v) {
        String t;
        try {
            t = v.getTitulo(); // cambia por getTitulo() si corresponde
        } catch (Exception e) {
            t = null;
        }
        return t == null ? "" : t;
    }

}
