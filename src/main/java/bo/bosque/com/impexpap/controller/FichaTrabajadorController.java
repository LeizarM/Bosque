package bo.bosque.com.impexpap.controller;
import java.util.ArrayList;
import java.util.List;
import bo.bosque.com.impexpap.dao.IGaranteReferencia;
import bo.bosque.com.impexpap.dao.IPersona;
import bo.bosque.com.impexpap.model.GaranteReferencia;

import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import bo.bosque.com.impexpap.dao.IDependiente;
import bo.bosque.com.impexpap.model.Dependiente;

@RestController
@CrossOrigin("*")
@RequestMapping("/fichaTrabajador")
public class FichaTrabajadorController {

    private final IDependiente dependienteDao;
    private final IGaranteReferencia garanteReferenciaDao;
    private final IPersona personaDao;

    public FichaTrabajadorController(IDependiente dependienteDao, IGaranteReferencia garanteReferenciaDao, IPersona personaDao){

        this.dependienteDao = dependienteDao;
        this.garanteReferenciaDao = garanteReferenciaDao;
        this.personaDao = personaDao;
    }

    /**
     * Servicio para obtener los dependientes por empleado
     * @param dep
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/dependientes")
    public List<Dependiente> obtenerDependientes( @RequestBody Dependiente dep ){

        List<Dependiente> lstTemp = this.dependienteDao.obtenerDependientes( dep.getCodEmpleado() );

        if( lstTemp.size() == 0 ) return new ArrayList<>();

        return lstTemp;
    }

    /**
     * Servicio para obtener los garantes y referencia por empleado
     * @param garRef
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/garanteReferencia")
    public List<GaranteReferencia> obtenerGaranteReferencia( @RequestBody GaranteReferencia garRef ){

        List<GaranteReferencia> lstTemp = this.garanteReferenciaDao.obtenerGaranteReferencia(  garRef.getCodEmpleado() );

        if( lstTemp.size() == 0 ) return new ArrayList<>();

        return lstTemp;

    }
}
