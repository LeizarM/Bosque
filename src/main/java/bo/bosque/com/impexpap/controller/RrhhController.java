package bo.bosque.com.impexpap.controller;
import bo.bosque.com.impexpap.dao.IEmpleado;
import bo.bosque.com.impexpap.dao.IPersona;
import bo.bosque.com.impexpap.model.Empleado;
import bo.bosque.com.impexpap.model.Persona;
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
}
