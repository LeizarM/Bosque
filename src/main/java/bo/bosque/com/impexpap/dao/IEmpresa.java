package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Empresa;

import java.util.List;

public interface IEmpresa  {

    /**
     * Procedimiento para obtener las empresas
     * @return
     */
    List<Empresa> obtenerEmpresas();
}
