package bo.bosque.com.impexpap.controller;


import bo.bosque.com.impexpap.dao.ILoteProduccion;
import bo.bosque.com.impexpap.model.LoteProduccion;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@CrossOrigin("*")
@RequestMapping("/loteProduccion")
public class LoteProduccionController {


    private final ILoteProduccion loteProducionDao;


    public LoteProduccionController(ILoteProduccion loteProducionDao) {
        this.loteProducionDao = loteProducionDao;
    }

    /**
     * Servicio para obtener los datos de lote produccion
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/newLoteProduccion")
    public List<LoteProduccion> obtenerGaranteReferencia(){

        List<LoteProduccion> lstTemp = this.loteProducionDao.obtenerLotesProduccionNew();

        if( lstTemp.size() == 0 ) return new ArrayList<>();

        return lstTemp;

    }

    /**
     * Servicio para obtener los articulo
     * @return lstTemp
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/articulos")
    public List<LoteProduccion> obtenerArticulo(){

        List<LoteProduccion> lstTemp = this.loteProducionDao.obtenerArticulos();

        if( lstTemp.size() == 0 ) return new ArrayList<>();

        return lstTemp;

    }

}
