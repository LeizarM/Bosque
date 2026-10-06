package bo.bosque.com.impexpap.dao;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Repository;

import bo.bosque.com.impexpap.utils.SpHelper;

/**
 * Escritura de {@code text_SocioNegocio} por procedimiento almacenado, via {@link SpHelper}.
 *
 * <p>El procedimiento <b>no se altera</b> (lo usa Depositos en cada listado): se llama por nombre y sin parametros
 * {@code OUTPUT}, con {@link SpHelper#ejecutarSinSalidas}, que no exige las salidas del contrato de
 * {@code ejecutarAbmMap}.
 */
@Repository
public class SocioNegocioSapDao implements ISocioNegocioSap {

    private static final String SP_ABM = "p_abm_SocioNegocio";

    private final SpHelper spHelper;

    public SocioNegocioSapDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public void actualizarDesdeSap(int audUsuario) {
        // Map armado a mano: solo viaja el usuario y la accion; el resto queda en el DEFAULT del procedimiento.
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("audUsuario", audUsuario);
        spHelper.ejecutarSinSalidas(SP_ABM, p, "B");
    }
}
