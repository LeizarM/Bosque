package bo.bosque.com.impexpap.controller;


import bo.bosque.com.impexpap.dao.IUsuarioBtn;
import bo.bosque.com.impexpap.dao.IVistaDao;
import bo.bosque.com.impexpap.model.Login;
import bo.bosque.com.impexpap.model.UsuarioBtn;
import bo.bosque.com.impexpap.model.Vista;
import org.springframework.security.access.annotation.Secured;
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
     * Procedimiento para obtener el menu dinamico por usuario
     * @param obj
     * @return JWT
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/vistaDinamica")
    public List<Vista> obtenerMenuDinamico(@RequestBody Login obj) {
        List<Vista> flat = this.vdao.obtainMenuXUser(obj.getCodUsuario());

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
     * Procedimiento para obtener los permisos por usuario por boton
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" }) //que un usuario admin o limitado si tiene acceso para consumir este recurso
    @PostMapping("/vistaBtn")
    public List<UsuarioBtn> obtenerPermisosBotones( @RequestBody Login obj ) {

        List<UsuarioBtn> lstPermisos = this.uDao.botonesXUsuario( obj.getCodUsuario() );

        if (lstPermisos.isEmpty()) {
            throw new RuntimeException("No hay permisos asociados al usuario.");
        }
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
