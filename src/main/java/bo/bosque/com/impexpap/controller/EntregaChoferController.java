package bo.bosque.com.impexpap.controller;


import bo.bosque.com.impexpap.dao.IEntregaChofer;
import bo.bosque.com.impexpap.model.EntregaChofer;
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
@RequestMapping("/entregas")
public class EntregaChoferController {


    private final IEntregaChofer entregaChoferDao;

    public EntregaChoferController(IEntregaChofer entregaChoferDao) {
        this.entregaChoferDao = entregaChoferDao;
    }

    // Implementación de endpoints para la gestión de entregas de choferes

    /**
     * Obtiene las entregas de choferes para un lote de producción
     * @param mb
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/chofer-entrega")
    public List<EntregaChofer> obtenerEntregasXEmpleado(@RequestBody EntregaChofer mb ){

        List<EntregaChofer> lstTemp = this.entregaChoferDao.listarEntregasXEmpleado( mb.getUChofer() );

        if( lstTemp.size() == 0 ) return new ArrayList<>();

        return lstTemp;




    }

    /**+
     * Registra las entregas de choferes su geolocalizacion
     * @param mb
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/registro-entrega-chofer")
    public ResponseEntity<?> registroEntregaChofer(@RequestBody EntregaChofer mb ) {

        System.out.println(mb.toString());

        Map<String, Object> response = new HashMap<>();
        mb.setFechaEntrega( new Utiles().convertirAFormatoSQLServer( mb.getFechaEntrega() ));


        if( !this.entregaChoferDao.registrarEntregaChofer( mb, "B" ) ){
            response.put("msg", "Error al Registrar Las Entregas");
            response.put("ok", "error");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        response.put("msg", "Datos de De la entrega actualizados");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }


    /**
     * Obtiene las entregas de choferes para una determinada fecha
     * @param mb
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/entregas-fecha")
    public List<EntregaChofer> obtenerEntregasChoferesXFecha(@RequestBody EntregaChofer mb ){


        List<EntregaChofer> lstTemp = this.entregaChoferDao.listarEntregasXChofer( mb.getFechaEntrega(), mb.getCodEmpleado() );


        if( lstTemp.size() == 0 ) return new ArrayList<>();

        return lstTemp;

    }

    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/registro-inicio-fin-entrega")
    public ResponseEntity<?> regitroInicioFinEntrega(@RequestBody EntregaChofer mb ) {

        mb.setUChofer(mb.getCodEmpleado());

        Map<String, Object> response = new HashMap<>();
        //mb.setFechaEntrega( new Utiles().convertirAFormatoSQLServer( mb.getFechaEntrega() ));


        if( !this.entregaChoferDao.registrarEntregaChofer( mb, "I" ) ){
            response.put("msg", "Error al Registrar El Inicio o Fin de las entregas");
            response.put("ok", "error");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        response.put("msg", "Datos de De la entrega de inicio o fin  actualizados");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }


    /**
     * Obtiene las entregas de choferes para un lote de producción
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/choferes")
    public List<EntregaChofer> lstChoferes(){

        List<EntregaChofer> lstTemp = this.entregaChoferDao.lstChoferes();

        if( lstTemp.size() == 0 ) return new ArrayList<>();

        return lstTemp;

    }

    /**
     * Obtiene las entregas de choferes para un lote de producción en un rango de fechas y por sucursal
     * @param mb
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/extracto")
    public List<EntregaChofer> lstChoferExtracto(  @RequestBody EntregaChofer mb  ){

        System.out.println( mb.toString() );

        List<EntregaChofer> lstTemp = this.entregaChoferDao.lstChoferesExtracto( mb.getFechaInicio(), mb.getFechaFin(), mb.getCodSucursal(), mb.getCodEmpleado() );

        if( lstTemp.size() == 0 ) return new ArrayList<>();

        return lstTemp;

    }

}
