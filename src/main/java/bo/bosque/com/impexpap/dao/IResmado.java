package bo.bosque.com.impexpap.dao;


import bo.bosque.com.impexpap.model.Resmado;

public interface IResmado {


    /**
     * Para registrar el Resmado
     * @param regResmado
     * @param acc
     * @return
     */
    boolean registrarResmado( Resmado regResmado, String acc );

    /**
     * Para obtener los resmados de un rango de fechas.
     * Con ambas fechas en null devuelve los ultimos 125.
     * @param fechaIni
     * @param fechaFin
     * @return
     */
    java.util.List<Resmado> obtenerResmados( java.util.Date fechaIni, java.util.Date fechaFin );

    /**
     * Para actualizar solo la orden de fabricacion y la empresa de un resmado
     * @param regResmado
     * @return
     */
    boolean actualizarOrdenFabricacion( Resmado regResmado );
}
