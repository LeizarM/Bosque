package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.SocioNegocio;

import java.util.List;

public interface ISocionegocio {



    /**
     * Procedimiento para obtener Socio Negocio
     * @return
     */
    List<SocioNegocio> obtenerSocioNegocio( int codEmpresa );
}
