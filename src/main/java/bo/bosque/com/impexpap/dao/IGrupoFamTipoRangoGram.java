package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.GrupoFamTipoRangoGram;

public interface IGrupoFamTipoRangoGram {

    /**
     * Para registrar el Grupo Familia y Tipo Rango Gramaje
     * @param grupoFamTipoRangoGram
     * @param acc
     */
    boolean registrarGrupoFamTipoRangoGram(GrupoFamTipoRangoGram grupoFamTipoRangoGram, String acc);
}
