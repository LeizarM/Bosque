package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.PlanillaIncapacidad;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.Date;
import java.util.List;

public interface IPlanillaIncapacidad {
    List<PlanillaIncapacidad> listarRevision(Date desde, Date hasta, long audUsuario);
    RespuestaSp marcarRevisado(long idPIT, int fueRevisado, long audUsuario);
}
