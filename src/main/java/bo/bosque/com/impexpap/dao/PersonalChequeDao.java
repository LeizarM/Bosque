package bo.bosque.com.impexpap.dao;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import bo.bosque.com.impexpap.dto.PersonalChequeDto;
import bo.bosque.com.impexpap.utils.SpHelper;

/**
 * Ver {@link IPersonalCheque}.
 *
 * <p>{@code p_list_Empleado} recibe la <b>sucursal en {@code @codPersona}</b> (herencia del legacy) y
 * la traduce con {@code tch_ParametroSuc}. Devuelve {@code (codEmpleado, 0, nombreCompleto)}: se leen
 * las columnas 1 y 3.
 */
@Repository
public class PersonalChequeDao implements IPersonalCheque {

    private static final String SP = "p_list_Empleado";

    private final SpHelper spHelper;

    public PersonalChequeDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public List<PersonalChequeDto> responsablesDeCustodia(long codSucursal) {
        return listar("U", codSucursal);
    }

    @Override
    public List<PersonalChequeDto> quienesEntregan(long codSucursal) {
        return listar("C", codSucursal);
    }

    private List<PersonalChequeDto> listar(String accion, long codSucursal) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codPersona", codSucursal);   // si, la sucursal va en @codPersona
        return spHelper.ejecutarListadoPorPosicion(SP, p, accion,
                (rs, i) -> new PersonalChequeDto(rs.getInt(1), rs.getString(3)));
    }
}
