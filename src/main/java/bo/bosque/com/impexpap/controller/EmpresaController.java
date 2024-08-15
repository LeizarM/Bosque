package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.dao.IEmpresa;
import bo.bosque.com.impexpap.model.Empresa;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@CrossOrigin("*")
@RequestMapping("/empresa")
public class EmpresaController {


    private IEmpresa emprDao;

    public EmpresaController(IEmpresa emprDao) {
        this.emprDao = emprDao;
    }

    /**
     * Procedimiento para obtener las empresas
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/lstEmpresas")
    public List<Empresa> obtenerEmpresas(){

        List<Empresa> lstEmpr = this.emprDao.obtenerEmpresas();
        if( lstEmpr.size() == 0 ) return  new ArrayList<>();
        return lstEmpr;
    }
}
