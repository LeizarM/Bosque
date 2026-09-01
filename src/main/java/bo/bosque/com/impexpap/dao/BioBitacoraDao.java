package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.BioBitacora;
import bo.bosque.com.impexpap.utils.SpHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class BioBitacoraDao implements IBioBitacora {

    private static final String SP_LIST = "p_list_BioBitacora";

    private final SpHelper spHelper;

    public BioBitacoraDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public List<BioBitacora> listar(Map<String, Object> filtro) {
        return spHelper.ejecutarListado(SP_LIST, filtro, "L", BioBitacora.class);
    }
}
