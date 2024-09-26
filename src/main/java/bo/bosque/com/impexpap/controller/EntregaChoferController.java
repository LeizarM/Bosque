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

    /**
     * Registra las entregas de choferes su geolocalizacion
     * @param mb
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/registro-entrega-chofer")
    public ResponseEntity<?> regitroEntregaChofer(@RequestBody EntregaChofer mb ) {



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

        System.out.println(mb.getDocDate());

       // mb.setDocDate( new Utiles().fechaJ_a_Sql ( mb.getDocDate() ));

        System.out.println(mb.getDocDate());

        List<EntregaChofer> lstTemp = this.entregaChoferDao.listarEntregasXChofer( mb.getDocDate() );

        if( lstTemp.size() == 0 ) return new ArrayList<>();

        return lstTemp;

    }



}
