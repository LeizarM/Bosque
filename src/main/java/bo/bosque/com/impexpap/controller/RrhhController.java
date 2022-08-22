package bo.bosque.com.impexpap.controller;
import bo.bosque.com.impexpap.dao.*;
import bo.bosque.com.impexpap.model.*;
import bo.bosque.com.impexpap.utils.Utiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.web.servlet.function.ServerResponse.status;

@RestController
@CrossOrigin("*")
@RequestMapping("/rrhh")
public class RrhhController {

    @Autowired()
    private IEmpleado empDao;

    @Autowired()
    private IPersona perDao;

    @Autowired()
    private IEmail emailDao;

    @Autowired()
    private ITelefono telfDao;

    @Autowired()
    private IExperienciaLaboral expLabDao;

    @Autowired()
    private IFormacion formDao;

    @Autowired()
    private ILicencia licenDao;

    @Autowired()
    private ICiudad ciudadDao;

    @Autowired()
    private IPais paisDao;

    @Autowired()
    private IZona zonaDao;

    @Autowired()
    private ISucursal sucDao;

    @Autowired()
    private ICargoSucursal cagoSucDao;

    @Autowired()
    private IEmpleadoCargo empCargoDao;

    @Autowired()
    private IRelEmpEmpr reeDao;


    /**
     * Procedimiento para obtener la lista de empleados
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/listEmpleados")
    public List<Empleado> obtenerListaPropuesta(@RequestBody Empleado emp){

        List <Empleado> lstTemp = this.empDao.obtenerListaEmpleados( emp.getRelEmpEmpr().getEsActivo() );

        if( lstTemp.size() == 0 ) return new ArrayList<>();

        return lstTemp;
    }
    /**
     * Procedimiento que obtendra lo datos de un empleado por su codigo
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/detalleEmpleado")
    public Empleado obtenerDetalleEmpleado ( @RequestBody  Empleado emp ){


        Empleado temp = this.empDao.obtenerEmpleado( emp.getCodEmpleado() );
        if(temp == null) return new Empleado();
        //System.out.println(temp.toString());
        return temp;
    }

    /**
     * Procedimiento para que obtendra los datos personales de un empleado
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/datosPersonales")
    public Persona obtenerDatosPersonales ( @RequestBody Persona per ){

        Persona temp = this.perDao.obtenerDatosPersonales( per.getCodPersona() );
        if(temp == null ) return new Persona();
        return temp;
    }
    /**
     * Procedimiento para que obtendra los correos por persona
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM"})
    @PostMapping("/emailPersona")
    public List<Email> obtenerEmail ( @RequestBody Email email ){

        List<Email> lstEmail = this.emailDao.obtenerCorreos( email.getCodPersona() );
        if(lstEmail.size() == 0) return new ArrayList<>();
        return lstEmail;
    }

    /**
     * Procedimiento que obtendra los telefonos de una persona
     * @param tel
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/telfPersona")
    public List<Telefono> obtenerTelefono( @RequestBody Telefono tel ){

        List<Telefono> lstTelefono = this.telfDao.obtenerTelefonos( tel.getCodPersona() );
        if( lstTelefono.size() == 0 ) return new ArrayList<>();
        return lstTelefono;

    }


    /**
     * Procedimiento para obtener la experiencia laboral de un empleado
     * @param expLab
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/expLabEmpleado")
    public List<ExperienciaLaboral> obtenerExperienciaLaboral ( @RequestBody ExperienciaLaboral expLab ){

        List<ExperienciaLaboral> lstExpLab = this.expLabDao.obtenerExperienciaLaboral( expLab.getCodEmpleado() );
        if( lstExpLab.size() == 0 ) return new ArrayList<>();
        return lstExpLab;

    }


    /**
     * Procedimiento para obtener la formacion de un empleado
     * @param form
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/formacionEmpleado")
    public List<Formacion> obtenerFormacion ( @RequestBody Formacion form ){

        List<Formacion> lstForm = this.formDao.obtenerFormacion( form.getCodEmpleado() );
        if( lstForm.size() == 0) return new ArrayList<>();
        return lstForm;
    }

    /**
     * Procedimiento para obtener la licencja de conducir de una persona
     * @param lic
     * @return
     */
    @Secured({"ROLE_ADM", "ROLE_LIM"})
    @PostMapping("/licenciaPersona")
    public List<Licencia> obtenerLicencia ( @RequestBody Licencia lic ){

        List<Licencia> lstLic = this.licenDao.obtenerLicencia( lic.getCodPersona() );
        if(lstLic.size() == 0 ) return new ArrayList<>();
        return  lstLic;

    }

    /**
     * Procedimiento para obtener las ciudades por pais
     * @param ciu
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/ciudadxPais")
    public List<Ciudad> obtenerCiudadxPais( @RequestBody Ciudad ciu  ){

        List<Ciudad> lstCiudad =  this.ciudadDao.obtenerCiudadesXPais( ciu.getCodPais() );
        if( lstCiudad.size() == 0 ) return new ArrayList<>();
        return lstCiudad;
    }

    /**
     * Procedimiento para obtener las Zonas por ciudad
     * @param ciu
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/zonas")
    public List<Zona> obtenerZona( @RequestBody Ciudad ciu  ){

        List<Zona> lstZona = this.zonaDao.obtenerZonaXCiudad( ciu.getCodCiudad() );
        if( lstZona.size() == 0 ) return new ArrayList<>();
        return lstZona;
    }

    /**
     * Procedimiento para obtener los paises registrados
     * @return
     */
    @Secured ( { "ROLE_ADM", "ROLE_LIM" }  )
    @PostMapping("/paises")
    public List<Pais> obtenerPaises(){

        List<Pais> lstPais = this.paisDao.obtenerPais();
        if( lstPais.size() == 0 ) return new ArrayList<>();
        return lstPais;
    }

    /**
     * Procedimiento para el abm de una persona
     */
    @Secured ( { "ROLE_ADM", "ROLE_LIM" }  )
    @PostMapping("/registroPersona")
    public ResponseEntity<?> registrarPersona( @RequestBody Persona per ){

        System.out.println(per.toString());
        Map<String, Object> response = new HashMap<>();

        per.setCiFechaVencimiento( new Utiles().fechaJ_a_Sql(per.getCiFechaVencimiento()));
        per.setFechaNacimiento(new Utiles().fechaJ_a_Sql(per.getFechaNacimiento()));
        String acc = "U";
        if( per.getCodPersona() == 0 ){
            acc = "I";
        }
        if( !this.perDao.registrarPersona(per, acc) ){
            response.put("msg", "Error al Actualizar los Datos de la persona");
            response.put("error", "ok");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        response.put("msg", "Datos Actualizados");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }


    /**
     * Procedimiento para obtener las sucursales por empresa
     * @param suc
     * @return
     */
    @Secured ( { "ROLE_ADM", "ROLE_LIM" }  )
    @PostMapping("/sucXEmpresa")
    public List<Sucursal> obtenerSucursalesXEmpresa ( @RequestBody Sucursal suc  ){

        List<Sucursal> lstSuc = this.sucDao.obtenerSucursalesXEmpresa( suc.getCodEmpresa() );
        if(lstSuc.size() == 0 ) return new ArrayList<>();
        return lstSuc;
    }

    /**
     * Procedimiento para devovler una lista de los cargos por sucursal que pertenecea a una empresa
     * @param cs
     * @return
     */
    @Secured ( { "ROLE_ADM", "ROLE_LIM" }  )
    @PostMapping("/cargoXSuc")
    public List<CargoSucursal> obtenerSucursalesXEmpresa ( @RequestBody CargoSucursal cs  ){

        List<CargoSucursal> lstCs = this.cagoSucDao.obtenerSucursalesXEmpresa(  cs.getCodSucursal() );

        if( lstCs.size() == 0 ) return new ArrayList<>();

        return lstCs;

    }

    /**
     * Procedimiento para registrar Empleado
     * @param emp
     * @return
     */
    @Secured ( { "ROLE_ADM", "ROLE_LIM" }  )
    @PostMapping("/registroEmpleado")
    public ResponseEntity<?> registrarEmpleado( @RequestBody Empleado emp ){
        Map<String, Object> response = new HashMap<>();


        String acc = "U";
        if( emp.getCodEmpleado() == 0 ){
            acc = "I";
        }

        if( !this.empDao.registroEmpleado( emp, acc ) ){
            response.put("msg", "Error al Actualizar los Datos del Empleado");
            response.put("error", "ok");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        response.put("msg", "Datos Actualizados");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Procedimiento para el registro de empleado y cargo
     * @param empCar
     * @return
     */
    @Secured ( { "ROLE_ADM", "ROLE_LIM" }  )
    @PostMapping("/registroEmpleadoCargo")
    public ResponseEntity<?> registrarEmpleado( @RequestBody EmpleadoCargo empCar ){


        Map<String, Object> response = new HashMap<>();
        empCar.setFechaInicio( new Utiles().fechaJ_a_Sql(empCar.getFechaInicio()));
        System.out.println(empCar.toString());

        String acc = "U";
        if(empCar.getExiste() == 0)  acc = "I";

        if( !this.empCargoDao.registrarEmpleadoCargo( empCar, acc ) ){
            response.put("msg", "Error al Actualizar los Datos del Empleado Cargo");
            response.put("error", "ok");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        response.put("msg", "Datos Actualizados");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Procedimiento para listar las fechas de beneficio interno y para planilla
     * @param ree
     * @return
     */
    @Secured ( { "ROLE_ADM", "ROLE_LIM" }  )
    @PostMapping("/fechasBeneficio")
    public List<RelEmplEmpr> obtenerFechasBeneficios ( @RequestBody RelEmplEmpr ree  ){

        List<RelEmplEmpr> lstRee = this.reeDao.obtenerRelacionesLaborales(  ree.getCodEmpleado() );

        if( lstRee.size() == 0 ) return new ArrayList<>();

        return lstRee;

    }

    /**
     * Procedimiento para el registro de Relacion del empleado con la empresa
     * @param
     * @return
     */
    @Secured ( { "ROLE_ADM", "ROLE_LIM" }  )
    @PostMapping("/registroRelEmp")
    public ResponseEntity<?> registrarRelEmpEmpr( @RequestBody RelEmplEmpr ree ){
        Map<String, Object> response = new HashMap<>();
        ree.setFechaIni( new Utiles().fechaJ_a_Sql(ree.getFechaIni()));
        ree.setFechaFin( new Utiles().fechaJ_a_Sql(ree.getFechaFin()));

        String acc = "U";
        if( ree.getCodRelEmplEmpr() == 0){
            acc = "I";
        }

        if( !this.reeDao.registrarRelEmpEmpr( ree, acc ) ){
            response.put("msg", "Error al Actualizar los Datos del Empleado Relacion Empresa");
            response.put("error", "ok");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        response.put("msg", "Datos Actualizados");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }


    /**
     * Procedimiento para el registro de Email
     * @param
     * @return
     */
    @Secured ( { "ROLE_ADM", "ROLE_LIM" }  )
    @PostMapping("/registroEmail")
    public ResponseEntity<?> registroEmail( @RequestBody Email e ){
        Map<String, Object> response = new HashMap<>();

        String acc = "U";
        if( e.getCodEmail() == 0){
            acc = "I";
        }

        if( !this.emailDao.registrarEmail( e, acc ) ){
            response.put("msg", "Error al Actualizar los Datos del Email del Empleado");
            response.put("error", "ok");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        response.put("msg", "Datos Actualizados");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Procedimiento para eliminar un Email
     * @param
     * @return
     */
    @Secured ( { "ROLE_ADM", "ROLE_LIM" }  )
    @PostMapping("/eliminarEmail")
    public ResponseEntity<?> eliminarEmail( @RequestBody Email e ){
        Map<String, Object> response = new HashMap<>();

        if( !this.emailDao.registrarEmail( e, "D" ) ){
            response.put("msg", "Error al Eliminar el Email del Empleado");
            response.put("error", "ok");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        response.put("msg", "Datos de Email Eliminados");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Procedimiento para el registro de Telefono
     * @param
     * @return
     */
    @Secured ( { "ROLE_ADM", "ROLE_LIM" }  )
    @PostMapping("/registroTelefono")
    public ResponseEntity<?> registroEmail( @RequestBody Telefono tel ){
        Map<String, Object> response = new HashMap<>();
        System.out.println(tel.toString());
        String acc = "U";
        if( tel.getCodTelefono() == 0){
            acc = "I";
        }

        if( !this.telfDao.registrarTelefono( tel,acc ) ){
            response.put("msg", "Error al Actualizar los Datos del Telefono del Empleado");
            response.put("error", "ok");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        response.put("msg", "Datos Actualizados");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

        /**
         * Procedimiento para eliminar un Email
         * @param
         * @return
         */
        @Secured ( { "ROLE_ADM", "ROLE_LIM" }  )
        @PostMapping("/eliminarTelefono")
        public ResponseEntity<?> eliminarTelefono( @RequestBody Telefono tel ){
            Map<String, Object> response = new HashMap<>();

            if( !this.telfDao.registrarTelefono( tel, "D" ) ){
                response.put("msg", "Error al Eliminar el Telefono del Empleado");
                response.put("error", "ok");
                return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
            }
            response.put("msg", "Datos de Telefono Eliminados");
            response.put("ok", "ok");
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        }

    /**
     * Procedimiento para registrar o actualizar la experiencia Laboral
     * @param
     * @return
     */
    @Secured ( { "ROLE_ADM", "ROLE_LIM" }  )
    @PostMapping("/registrarExpLaboral")
    public ResponseEntity<?> registrarExpLaboral( @RequestBody ExperienciaLaboral expl ){
        Map<String, Object> response = new HashMap<>();

        expl.setFechaInicio( new Utiles().fechaJ_a_Sql(expl.getFechaInicio()));
        expl.setFechaFin( new Utiles().fechaJ_a_Sql(expl.getFechaFin()));

        String acc = "U";
        if( expl.getCodExperienciaLaboral() == 0){
            acc = "I";
        }

        if( !this.expLabDao.registrarExpLaboral( expl, acc ) ){
            response.put("msg", "Error al Registrar la experiencia laboral del Empleado");
            response.put("error", "ok");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        response.put("msg", "Datos de ExperienciaLaboral Actualizados");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Procedimiento para registrar o actualizar la formacion de un empleado
     * @param
     * @return
     */
    @Secured ( { "ROLE_ADM", "ROLE_LIM" }  )
    @PostMapping("/registrarFormacion")
    public ResponseEntity<?> registrarFormacion( @RequestBody Formacion fr ){
        Map<String, Object> response = new HashMap<>();

        fr.setFechaFormacion( new Utiles().fechaJ_a_Sql(fr.getFechaFormacion()) );

        String acc = "U";
        if( fr.getCodFormacion() == 0){
            acc = "I";
        }

        if( !this.formDao.registrarFormacion( fr, acc ) ){
            response.put("msg", "Error al Registrar la Formacion del Empleado");
            response.put("error", "ok");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        response.put("msg", "Datos de Formacion Actualizados");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Procedimiento para registrar o actualizar la formacion de un empleado
     * @param
     * @return
     */
    @Secured ( { "ROLE_ADM", "ROLE_LIM" }  )
    @PostMapping("/registrarLicencia")
    public ResponseEntity<?> registrarLicencia( @RequestBody Licencia lc ){
        Map<String, Object> response = new HashMap<>();

        lc.setFechaCaducidad( new Utiles().fechaJ_a_Sql(lc.getFechaCaducidad()) );

        String acc = "U";
        if( lc.getCodLicencia() == 0){
            acc = "I";
        }

        if( !this.licenDao.registrarLicencia( lc, acc ) ){
            response.put("msg", "Error al Registrar la Licencia del Empleado");
            response.put("error", "ok");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        response.put("msg", "Datos de Licencia Actualizados");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Procedimiento para obtener el ultimo codigo insertado de empleado
     * @return
     */
    @Secured ( { "ROLE_ADM", "ROLE_LIM" }  )
    @PostMapping("/ultimoCodEmpleado")
    public Empleado obtenerUltimoCodEmpleado ( ){
        Empleado temp = new Empleado();
        temp.setCodEmpleado( this.empDao.obtenerUltimoEmpleado() );
        if(temp.getCodEmpleado() <= 0 ) return new Empleado();
        return temp;
    }

    /**
     * Procedimiento para obtener el ultimo codigo insertado de una persona
     * @return
     */
    @Secured ( { "ROLE_ADM", "ROLE_LIM" }  )
    @PostMapping("/ultimoCodPersona")
    public Persona obtenerUltimoCodPersona ( ){
        Persona temp = new Persona();
        temp.setCodPersona( this.perDao.obtenerUltimoPersona() );
        if(temp.getCodPersona() <= 0 ) return new Persona();
        return temp;
    }

}
