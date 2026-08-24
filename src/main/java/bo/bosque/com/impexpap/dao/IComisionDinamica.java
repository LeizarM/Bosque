package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.ComisionDinamica;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

public interface IComisionDinamica {

    /** Alta, cambio o cierre de vigencia de una escala. acc: I, U, D. */
    RespuestaSp registrarComisionDinamica(ComisionDinamica mb, String acc);

    /** Una escala puntual. */
    List<ComisionDinamica> obtenerPorId(long idDc);

    /** Escalas vigentes a la fecha indicada. */
    List<ComisionDinamica> obtenerVigentes(Integer esInterno, java.util.Date fecha);

    /** Todas las escalas. esInterno null trae internas y externas. */
    List<ComisionDinamica> obtenerTodas(Integer esInterno);
}
