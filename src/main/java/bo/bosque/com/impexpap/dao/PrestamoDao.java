package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Prestamo;
import bo.bosque.com.impexpap.model.PrestamoDetalle;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.Tipos;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class PrestamoDao implements IPrestamo {

    private final SpHelper spHelper;

    public PrestamoDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    /**
     * Obtendra los prestamos provenientes de SAP cruzados con la base de datos
     * BOSQUE
     * 
     * @param p
     * @return
     */
    @Override
    public List<Prestamo> obtenerPrestamosSAP(Prestamo p) {
        return this.spHelper.ejecutarListado("p_list_Prestamo", p, "A1", Prestamo.class);
    }

    @Override
    public RespuestaSp registrarPrestamo(Prestamo p, String acc) {
        return this.spHelper.ejecutarAbm("p_abm_Prestamo", p, acc);
    }

    @Override
    public List<Prestamo> listarEmpleadosPrestamo(Prestamo param) {
        return this.spHelper.ejecutarListado("p_list_Prestamo", param, "L1", Prestamo.class);
    }

    @Override
    public List<PrestamoDetalle> previsualizarCuotas(Prestamo param) {
        return this.spHelper.ejecutarListado("p_abm_Prestamo", param, "PC", PrestamoDetalle.class);
    }

    @Override
    public List<Tipos> listEstadosPrestamo(Prestamo p) {
        return this.spHelper.ejecutarListado("p_list_Prestamo", p, "TE", Tipos.class);
    }

    @Override
    public List<Tipos> listTiposPagoPrestamo(Prestamo p) {
        return this.spHelper.ejecutarListado("p_list_Prestamo", p, "TP", Tipos.class);
    }

    @Override
    public RespuestaSp actualizarCuotaPrestamo(PrestamoDetalle p, String acc) {
        return this.spHelper.ejecutarAbm("p_abm_PrestamoDetalle", p, acc);
    }

    @Override
    public List<PrestamoDetalle> obtenerDetallesPrestamo(Prestamo p) {
        PrestamoDetalle param = new PrestamoDetalle();
        param.setCodPrestamo(p.getCodPrestamo());
        param.setMostrarAnulados(p.getMostrarAnulados() != null ? p.getMostrarAnulados() : 0);
        return this.spHelper.ejecutarListado("p_list_PrestamoDetalle", param, "G", PrestamoDetalle.class);
    }

    @Override
    public RespuestaSp adelantarCuota(PrestamoDetalle p, String acc) {
        return this.spHelper.ejecutarAbm("p_abm_PrestamoDetalle", p, acc);
    }
}
