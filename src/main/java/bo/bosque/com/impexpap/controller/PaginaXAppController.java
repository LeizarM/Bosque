package bo.bosque.com.impexpap.controller;


import bo.bosque.com.impexpap.dao.IArticuloPrecioDisponible;

import bo.bosque.com.impexpap.model.ArticuloPrecioDisponible;
import bo.bosque.com.impexpap.model.LoteProduccion;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@CrossOrigin("*")
@RequestMapping("/paginaXApp")
public class PaginaXAppController {

    private final IArticuloPrecioDisponible articuloPrecioDisponibleDao;


    public PaginaXAppController( IArticuloPrecioDisponible articuloPrecioDisponibleDao ) {

        this.articuloPrecioDisponibleDao = articuloPrecioDisponibleDao;
    }



    /**
     * Servicio para obtener los articulos de IPX y ESPP
     * @return List
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })

    @PostMapping("/articulosX") //que un usuario admin o limitado si tiene acceso para consumir este recurso
    public List<ArticuloPrecioDisponible> listadoX(  @RequestBody ArticuloPrecioDisponible apd  ) {

        System.out.println( apd.toString() );

        List<ArticuloPrecioDisponible> lstTemp = this.articuloPrecioDisponibleDao.obtenerArticulosIPXyESPP( apd.getCodCiudad() );

        if( lstTemp.size() == 0 ) return new ArrayList<>();

        return lstTemp;


    }


}
