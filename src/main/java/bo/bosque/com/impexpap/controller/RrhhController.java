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
        System.out.println(temp.toString());
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

}
