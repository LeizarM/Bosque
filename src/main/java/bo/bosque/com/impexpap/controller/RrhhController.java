package bo.bosque.com.impexpap.controller;
import bo.bosque.com.impexpap.dao.IEmpleado;
import bo.bosque.com.impexpap.model.Empleado;
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

    /**
     * Procedimiento para obtener la lista de empleados
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/listEmpleados")
    public List<Empleado> obtenerListaPropuesta(@RequestBody Empleado emp){

        System.out.println("el es activo es= "+emp.getRelEmpEmpr().getEsActivo());

        List <Empleado> lstTemp = this.empDao.obtenerEmpleados( emp.getRelEmpEmpr().getEsActivo() );

        if( lstTemp.size() == 0 ) return new ArrayList<>();

        return lstTemp;
    }
}
