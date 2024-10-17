package bo.bosque.com.impexpap.controller;


import bo.bosque.com.impexpap.dao.IRegistroFacturas;
import bo.bosque.com.impexpap.model.RegistroFacturas;
import bo.bosque.com.impexpap.model.RegistroResma;
import bo.bosque.com.impexpap.utils.Tipos;
import bo.bosque.com.impexpap.utils.Utiles;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin("*")
@RequestMapping("/registro-facturas")
public class RegistroFacturasController {

    private final IRegistroFacturas registroFacturasDao;

    public RegistroFacturasController(IRegistroFacturas registroFacturasDao) {
        this.registroFacturasDao = registroFacturasDao;
    }

    /**
     * Devovlera una lista del estado de las propuestas
     * @return
     */
    /**@Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/tiposFactura")
    public List<Tipos> lstTipoFactura(){
        return this.registroFacturasDao.lstTipoFactura();
    }**/

    /**
     * Obtener las empresas registradas
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/lstEpresas")
    public List<RegistroFacturas> lstEmpresas(){
        return this.registroFacturasDao.lstEmpresas();
    }

    /**
     * Registrar las facturas
     * @param mb
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/registroFactura")
    public ResponseEntity<?> registrarFactura(@RequestBody RegistroFacturas mb ) {


        Map<String, Object> response = new HashMap<>();
        mb.setFecha( new Utiles().fechaJ_a_Sql(mb.getFecha()));
        String acc = "U";
        if( mb.getIdFac() == 0){
            acc = "I";
        }

        if( !this.registroFacturasDao.registrarRegistroFacturas( mb, acc ) ){
            response.put("msg", "Error al Registrar El Registro de Facturas");
            response.put("ok", "error");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        response.put("msg", "Datos de Registro de Facturas Registrados Correctamente");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Obtener las facturas registradas por fecha
     * @param mb
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/lstFacturasRegistradas")
    public List<RegistroFacturas> lstFacturasRegistradas( @RequestBody RegistroFacturas mb  ){



        mb.setFechaSistema( new Utiles().fechaJ_a_Sql(mb.getFechaSistema()) );

        List<RegistroFacturas> lstEmpr = this.registroFacturasDao.lstFacturasRegistradas( mb.getFechaSistema() );
        if( lstEmpr.size() == 0 ) return  new ArrayList<>();
        return lstEmpr;
    }

}
