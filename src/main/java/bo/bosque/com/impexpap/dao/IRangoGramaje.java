package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.RangoGramaje;

public interface IRangoGramaje {

    /**
     * Para registrar el Rango Gramaje
     * @param rangoGramaje
     * @param acc
     * @return
     */
    boolean registrarRangoGramaje( RangoGramaje rangoGramaje, String acc );
}
