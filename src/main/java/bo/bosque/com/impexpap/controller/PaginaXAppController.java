package bo.bosque.com.impexpap.controller;


import bo.bosque.com.impexpap.dao.IArticuloPrecioDisponible;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin("*")
@RequestMapping("/paginaXApp")
public class PaginaXAppController {

    private final IArticuloPrecioDisponible articuloPrecioDisponibleDao;


    public PaginaXAppController( IArticuloPrecioDisponible articuloPrecioDisponibleDao ) {

        this.articuloPrecioDisponibleDao = articuloPrecioDisponibleDao;
    }



}
