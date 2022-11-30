package bo.bosque.com.impexpap.controller;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import bo.bosque.com.impexpap.dao.IAutorizacionDao;
import bo.bosque.com.impexpap.model.Autorizacion;
import bo.bosque.com.impexpap.utils.Tipos;
import lombok.extern.slf4j.Slf4j;


@RestController
@CrossOrigin("*")
@RequestMapping("/price")
@Slf4j
public class PrecioController {

    @Autowired
    private IAutorizacionDao autDao;

    /**
     * Procedimiento para obtener la lista de propuesta para que sean autorizadas
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/autorizacion")
    public List<Autorizacion> obtenerListaPropuesta(){
        List <Autorizacion> lstAut = this.autDao.listAutorizacion();

        if( lstAut.size() == 0 ) return new ArrayList<>();
        
        return lstAut;
    }

    /**
     * Devovlera una lista del estado de las propuestas
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/estadoPropuesta")
    public List<Tipos> listPropuestas(){
        return this.autDao.lstEstadoPropuestas();
    }
}
