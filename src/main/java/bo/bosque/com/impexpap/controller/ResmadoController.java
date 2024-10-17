package bo.bosque.com.impexpap.controller;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import bo.bosque.com.impexpap.dao.IDetalleResmado;
import bo.bosque.com.impexpap.dao.IGrupoProduccion;
import bo.bosque.com.impexpap.dao.ILoteProduccion;
import bo.bosque.com.impexpap.dao.IResmado;
import bo.bosque.com.impexpap.model.*;
import bo.bosque.com.impexpap.utils.Utiles;


@RestController
@CrossOrigin("*")
@RequestMapping("/resmado")
public class ResmadoController {


    private final IResmado resmadoDao;
    private final IGrupoProduccion grupoProduccionDao;
    private final IDetalleResmado detalleResmadoDao;
    private final ILoteProduccion loteProduccionDao;


    public ResmadoController(IResmado resmadoDao, IGrupoProduccion grupoProduccionDao, IDetalleResmado detalleResmadoDao, ILoteProduccion loteProduccionDao) {
        this.resmadoDao = resmadoDao;
        this.grupoProduccionDao = grupoProduccionDao;
        this.detalleResmadoDao = detalleResmadoDao;
        this.loteProduccionDao = loteProduccionDao;
    }


    /**
     * Servicio para obtener los articulo
     * @return lstTemp
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/articulos")
    public List<LoteProduccion> obtenerArticulo(){

        List<LoteProduccion> lstTemp = this.loteProduccionDao.obtenerArticulos();

        if( lstTemp.size() == 0 ) return new ArrayList<>();

        return lstTemp;

    }



    /**
     * Servicio para los grupos de resmadores por maquina que resmaron
     * @return lstTemp
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/grupoProduccion")
    public List<GrupoProduccion> obtenerGrupoProduccion(){

        List<GrupoProduccion> lstTemp = this.grupoProduccionDao.obtenerGrupoProduccion();

        if( lstTemp.size() == 0 ) return new ArrayList<>();

        return lstTemp;

    }


    /**
     * servicio para registrar el lote de produccion
     * @param rgs
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/registroResmado")
    public ResponseEntity<?> registrarLoteProduccion( @RequestBody Resmado rgs ) {


        Map<String, Object> response = new HashMap<>();
        rgs.setFecha( new Utiles().fechaJ_a_Sql(rgs.getFecha()));

        String acc = rgs.getIdRes() == 0 ? "I" : "U";


        if( !this.resmadoDao.registrarResmado( rgs, acc ) ){
            response.put("msg", "Error al Registrar El Resmado");
            response.put("ok", "error");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        response.put("msg", "Datos de Resmados Actualizados");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }


    /**
     * servicio para registrar el detalle de resmado
     * @param rdr
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/registroDetResmado")
    public ResponseEntity<?> registrarDetResmado( @RequestBody List<DetalleResmado> rdr ) {


        Map<String, Object> response = new HashMap<>();

        for (DetalleResmado material : rdr) {
            String acc = material.getIdRetRes() == 0 ? "I" : "U";
            if( !this.detalleResmadoDao.registrarDetalleResmado( material, acc ) ){
                response.put("msg", "Error al Registrar El Detalle Resmado");
                response.put("ok", "error");
                return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }


        response.put("msg", "Datos del Detalle de Resmado Actualizados");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }


}
