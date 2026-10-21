package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.NivelJerarquico;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

public interface INivelJerarquico {


    List<NivelJerarquico> getAllNiveles();

    /** {@code p_list_NivelJerarquico 'P'}: todas las escalas, con vigencia ({@code activo}) y cargos que la usan. */
    List<NivelJerarquico> listar();

    /** {@code p_abm_NivelJerarquico} 'I' (codNivel 0) o 'U'. Las reglas y el mensaje los pone el SP. */
    RespuestaSp registrar(NivelJerarquico escala);

    /** {@code p_abm_NivelJerarquico} 'D'. El SP rechaza una escala que usan cargos. */
    RespuestaSp eliminar(int codNivel, int audUsuario);
}
