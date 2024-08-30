package bo.bosque.com.impexpap.controller;


import bo.bosque.com.impexpap.dao.*;
import bo.bosque.com.impexpap.model.*;
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
@RequestMapping("/material-mal-estado")
public class MaterialMalEstadoController {



    private final IRegistroResma resmaDao;
    private final IRegistroResmaDetalle resmaDetalleDao;
    private final ITipoDano tipoDanoDao;
    private final IRegistroDanoBobina danoBobinaDao;
    private final IRegistroDanoBobinaDetalle danoBobinaDetalleDao;


    public MaterialMalEstadoController(IRegistroResma resmaDao, IRegistroResmaDetalle resmaDetalleDao, ITipoDano tipoDanoDao, IRegistroDanoBobina danoBobinaDao, IRegistroDanoBobinaDetalle danoBobinaDetalleDao) {

        this.resmaDao = resmaDao;
        this.resmaDetalleDao = resmaDetalleDao;
        this.tipoDanoDao = tipoDanoDao;
        this.danoBobinaDao = danoBobinaDao;
        this.danoBobinaDetalleDao = danoBobinaDetalleDao;
    }


    /**
     * Procedimiento para obtener todos los documentos de resma por empresa
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/lstDocNum")
    public List<RegistroResma> obtenerDocNum( @RequestBody RegistroResma mb  ){

        List<RegistroResma> lstEmpr = this.resmaDao.lstEntradaDeMercaderias( mb.getCodEmpresa() );
        if( lstEmpr.size() == 0 ) return  new ArrayList<>();
        return lstEmpr;
    }


    /**
     * Procedimiento para obtener todos los documentos de resma por empresa y número de documento
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/lstArticuloXEntrada")
    public List<RegistroResma> obtenerArticuloXEntrada( @RequestBody RegistroResma mb  ){
        List<RegistroResma> lstEmpr = this.resmaDao.lstArticuloXEntrada( mb.getCodEmpresa(), mb.getDocNum() );
        if( lstEmpr.size() == 0 ) return  new ArrayList<>();
        return lstEmpr;
    }

    /**
     * Procedimiento para listar todos los tipos de daño
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/lstTipoDano")
    public List<TipoDano> obtenerTipoDano( ){
        List<TipoDano> lstTemp = this.tipoDanoDao.lstTipoDano();
        if( lstTemp.size() == 0 ) return  new ArrayList<>();
        return lstTemp;
    }

    /**
     * Procedimiento para registrar o actualizar un registro de resma mal estado
     * @param mb
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/registroResmaMalEstado")
    public ResponseEntity<?> registrarResmaMalEstado(@RequestBody RegistroResma mb ) {



        Map<String, Object> response = new HashMap<>();
        mb.setFecha( new Utiles().fechaJ_a_Sql(mb.getFecha()));
        String acc = "U";
        if( mb.getIdMer() == 0){
            acc = "I";
        }

        if( !this.resmaDao.registrarRegistroResma( mb, acc ) ){
            response.put("msg", "Error al Registrar El Registro en Mal estado");
            response.put("ok", "error");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        response.put("msg", "Datos de Lote Produccion Actualizados");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Procedimiento para registrar o actualizar los detalles de resma mal estado por registro de resma mal estado
     * @param mb
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/detRegistroResmaMalEstado")
    public ResponseEntity<?> detRegistrarResmaMalEstado( @RequestBody List<RegistroResmaDetalle> mb  ) {

        Map<String, Object> response = new HashMap<>();
        boolean errorOccurred = false;

        for (RegistroResmaDetalle i : mb) {
            String acc = i.getIdRMD() == 0 ? "I" : "U"; // Determinar la acción por cada material

            if (!this.resmaDetalleDao.registrarRegistroResmaDetalle(i, acc)) {
                errorOccurred = true;
                // Podrías optar por recolectar más detalles sobre qué material causó el error
                response.put("msg", "Error al registrar el detalle de resmado con ID: " + i.getIdMer());
                response.put("ok", "error");

                // Puedes decidir si retornar inmediatamente en caso de error o continuar procesando
                return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        response.put("msg", "Todos los datos de ingreso han sido actualizados correctamente");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }


    //====================== CASO BOBINAS =========================

    /**
     * Procedimiento para listar todos los documentos de Bobina por empresa
     * @param mb
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/lstDocNumBob")
    public List<RegistroDanoBobina> obtenerDocNumBob( @RequestBody RegistroDanoBobina mb  ){

        List<RegistroDanoBobina> lstTemp = this.danoBobinaDao.lstEntradaDeMercaderiasBob( mb.getCodEmpresa() );
        if( lstTemp.size() == 0 ) return  new ArrayList<>();
        return lstTemp;
    }

    /**
     * Procedimiento para obtener todos los documentos de Bobina por empresa y número de documento
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/lstArticuloXEntradaBob")
    public List<RegistroDanoBobina> obtenerArticuloXEntradaBob( @RequestBody RegistroDanoBobina mb  ){
        List<RegistroDanoBobina> lstEmpr = this.danoBobinaDao.lstArticuloXEntradaBob( mb.getCodEmpresa(), mb.getDocNum() );
        if( lstEmpr.size() == 0 ) return  new ArrayList<>();
        return lstEmpr;
    }


    /**
     * Procedimiento para registrar o actualizar un registro de resma mal estado
     * @param mb
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/registroBobinaMalEstado")
    public ResponseEntity<?> registrarBobinaMalEstado(@RequestBody RegistroDanoBobina mb ) {



        Map<String, Object> response = new HashMap<>();
        mb.setFecha( new Utiles().fechaJ_a_Sql(mb.getFecha()));
        String acc = "U";
        if( mb.getIdReg() == 0){
            acc = "I";
        }

        if( !this.danoBobinaDao.registrarRegistroDanoBobina( mb, acc ) ){
            response.put("msg", "Error al Registrar El Registro de bobinas en Mal estado");
            response.put("ok", "error");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        response.put("msg", "Datos  Actualizados");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Procedimiento para registrar o actualizar los detalles de resma mal estado por registro de resma mal estado
     * @param mb
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/detRegistroBobinaMalEstado")
    public ResponseEntity<?> detRegistrarBobinaMalEstado( @RequestBody List<RegistroDanoBobinaDetalle> mb  ) {


        System.out.println( mb.toString() );

        Map<String, Object> response = new HashMap<>();
        boolean errorOccurred = false;

        for (RegistroDanoBobinaDetalle i : mb) {
            String acc = i.getIdRegDet() == 0 ? "I" : "U"; // Determinar la acción por cada material
            System.out.println(i.toString());
            if (!this.danoBobinaDetalleDao.registrarRegistroDanoBobinaDetalle(i, acc)) {
                errorOccurred = true;
                // Podrías optar por recolectar más detalles sobre qué material causó el error
                response.put("msg", "Error al registrar el detalle de Bobina ");
                response.put("ok", "error");

                // Puedes decidir si retornar inmediatamente en caso de error o continuar procesando
                return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        response.put("msg", "Todos los datos de ingreso han sido actualizados correctamente");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }


}
