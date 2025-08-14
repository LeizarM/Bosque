package bo.bosque.com.impexpap.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import bo.bosque.com.impexpap.dao.*;
import bo.bosque.com.impexpap.model.*;
import bo.bosque.com.impexpap.utils.ApiResponse;
import bo.bosque.com.impexpap.utils.Utiles;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RestController
@CrossOrigin("*")
@RequestMapping("/loteProduccion")
public class LoteProduccionController {


    private final ILoteProduccion loteProducionDao;
    private final IMaterialIngreso materialIngresoDao;
    private final IMaterialSalida materialSalidaDao;
    private final IMerma mermaDao;
    private final IMaquinaProduccion maquinaProduccionDao;
    private final IEmpresa empresaDao;

    /**
     * Constructor de la clase
     * @param loteProducionDao
     * @param materialIngresoDao
     * @param materialSalidaDao
     * @param mermaDao
     * @param maquinaProduccionDao
     * @param empresaDao
     */
    public LoteProduccionController(ILoteProduccion loteProducionDao, IMaterialIngreso materialIngresoDao, IMaterialSalida materialSalidaDao, IMerma mermaDao, IMaquinaProduccion maquinaProduccionDao, IEmpresa empresaDao) {
        this.loteProducionDao = loteProducionDao;
        this.materialIngresoDao = materialIngresoDao;
        this.materialSalidaDao = materialSalidaDao;
        this.mermaDao = mermaDao;
        this.maquinaProduccionDao = maquinaProduccionDao;
        this.empresaDao = empresaDao;
    }

    /**
     * Servicio para obtener los datos de lote produccion
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/newLoteProduccion")
    public List<LoteProduccion> obtenerListadeLotesDeProduccion( @RequestBody LoteProduccion mb ){

        List<LoteProduccion> lstTemp = this.loteProducionDao.obtenerLotesProduccionNew( mb.getIdMa() );

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

    /**
     * servicio para registrar el lote de produccion
     * @param regLoteProduccion
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/registroLoteProduccion")
    public ResponseEntity<?> registrarLoteProduccion(@RequestBody LoteProduccion regLoteProduccion ) {


        Map<String, Object> response = new HashMap<>();
        regLoteProduccion.setFecha( new Utiles().fechaJ_a_Sql(regLoteProduccion.getFecha()));
        String acc = "U";
        if( regLoteProduccion.getIdLp() == 0){
            acc = "I";
        }

        if( !this.loteProducionDao.registrarLoteProduccion( regLoteProduccion, acc ) ){
            response.put("msg", "Error al Registrar El lote de produccion");
            response.put("ok", "error");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        response.put("msg", "Datos de Lote Produccion Actualizados");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * servicio para registrar el material de ingreso
     * @param regMatIng
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/registroIngreso")
    public ResponseEntity<?> registrarMaterialIngreso( @RequestBody List<MaterialIngreso> regMatIng  ) {

        Map<String, Object> response = new HashMap<>();
        boolean errorOccurred = false;

        for (MaterialIngreso material : regMatIng) {
            String acc = material.getIdMi() == 0 ? "I" : "U"; // Determinar la acción por cada material

            if (!this.materialIngresoDao.registrarMaterialIngreso(material, acc)) {
                errorOccurred = true;
                // Podrías optar por recolectar más detalles sobre qué material causó el error
                response.put("msg", "Error al registrar el material de ingreso con ID: " + material.getIdMi());
                response.put("ok", "error");

                // Puedes decidir si retornar inmediatamente en caso de error o continuar procesando
                return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        response.put("msg", "Todos los datos de ingreso han sido actualizados correctamente");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * servicio para registrar el material de ingreso
     * @param regMatSal
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/registroSalida")
    public ResponseEntity<?> registrarMaterialSalida( @RequestBody List<MaterialSalida> regMatSal  ) {

        Map<String, Object> response = new HashMap<>();

        for (MaterialSalida material : regMatSal) {
            String acc = material.getIdMs() == 0 ? "I" : "U"; // Determinar la acción por cada material

            if (!this.materialSalidaDao.registrarMaterialSalida(material, acc)) {

                // Podrías optar por recolectar más detalles sobre qué material causó el error
                response.put("msg", "Error al registrar el material de salida con ID: " + material.getIdMs());
                response.put("ok", "error");

                // Puedes decidir si retornar inmediatamente en caso de error o continuar procesando
                return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        response.put("msg", "Todos los datos de salida han sido actualizados correctamente");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);

    }


    /**
     * Servicio para registrar la merma
     * @param regMerma
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/registroMerma")
    public ResponseEntity<?> registrarMerma( @RequestBody List<Merma> regMerma  ) {

        Map<String, Object> response = new HashMap<>();

        for (Merma material : regMerma) {
            String acc = material.getIdMe() == 0 ? "I" : "U"; // Determinar la acción por cada material

            if (!this.mermaDao.registrarMerma(material, acc)) {

                // Podrías optar por recolectar más detalles sobre qué material causó el error
                response.put("msg", "Error al registrar la merma con ID: " + material.getIdMe());
                response.put("ok", "error");

                // Puedes decidir si retornar inmediatamente en caso de error o continuar procesando
                return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        response.put("msg", "Todos los datos de merma han sido actualizados correctamente");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }


    /**
     * Servicio para obtener las maquinas de producción
     * @return lstTemp
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/maquina")
    public List<MaquinaProduccion> obtenerMaquina(){

        List<MaquinaProduccion> lstTemp = this.maquinaProduccionDao.obtenerMaquina();

        if( lstTemp.size() == 0 ) return new ArrayList<>();

        return lstTemp;

    }


    /**
     * Obtiene todos las empresas registradas en el sistema.
     * @return
     */
    @PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
    @PostMapping("/lst-empresas")
    public List<Empresa> obtenerEmpresas() {

        List<Empresa> empresas = empresaDao.obtenerEmpresas();

        if( empresas.size() == 0 ) return new ArrayList<>();

        return empresas;

    }


    /**
     * Obtiene los docNum por orden de fabricación para una empresa.
     * @return
     */
    @PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
    @PostMapping("/lstDocNumOrdFabXEmpresa")
    public List<LoteProduccion> obtenerDocNumOrdFabXEmpresa( @RequestBody LoteProduccion mb ) {

        List<LoteProduccion> temp = loteProducionDao.obtenerDocNumXEmpresa( mb.getCodEmpresa() );

        if( temp.size() == 0 ) return new ArrayList<>();

        return temp;

    }


}
