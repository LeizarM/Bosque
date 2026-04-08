package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.CambiosTigo;
import bo.bosque.com.impexpap.model.ChipTigo;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import bo.bosque.com.impexpap.utils.Tipos;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ChipTigoDao implements IChipTigo {
    private final SpHelper spHelper;

    public ChipTigoDao(SpHelper spHelper){this.spHelper = spHelper;}

    /**
     * ABM CHIP TIGO
     * @param ct
     * @param acc
     * @return
     */
    @Override
    public RespuestaSp abmChipTigo(ChipTigo ct, String acc) {
        return this.spHelper.ejecutarAbm("p_abm_tTigoChip", ct, acc);
    }

    /**
     * LISTADO DE LOS REGISTROS DE PERDIDA DE CHIP TIGO
     * @param filtro
     * @return
     */
    @Override
    public List<ChipTigo> listarPerdidas(ChipTigo filtro) {
        return spHelper.ejecutarListado(
                "p_list_tTigoChip",
                filtro,
                "L",
                ChipTigo.class
        );
    }
    /**
     * LISTAR PERIODOS PARA EL FILTRO
     * @return
     */
    @Override
    public List<ChipTigo> listarPeriodos() {
        // Usamos un objeto vacío solo para disparar la acción 'A'
        return spHelper.ejecutarListado(
                "p_list_tTigoChip",
                new ChipTigo(),
                "A",
                ChipTigo.class
        );
    }
    /**
     * Obtendra una lista de tipo de renovacion chip tigo
     * @return
     */
    public List<Tipos> listTipoRenovacion() {
        return new Tipos().listTipoRenovacion();
    }
}
