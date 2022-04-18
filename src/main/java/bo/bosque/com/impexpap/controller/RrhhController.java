package bo.bosque.com.impexpap.controller;
import bo.bosque.com.impexpap.dao.*;
import bo.bosque.com.impexpap.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

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
}
