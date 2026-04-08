package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.ConfigComisionesBanco;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

public interface IConfigComisionesBanco {

    /**
     * Registrar, actualizar o eliminar una Configuración de Comisiones de Banco
     * @param mb   Objeto con los datos
     * @param acc  Acción ('I', 'U', 'D')
     */
    RespuestaSp registrarConfigComisionesBanco(ConfigComisionesBanco mb, String acc);

    /**
     * Obtener configuraciones por ID. Si idConfig == 0 devuelve todas.
     * @param idConfig ID a buscar (0 = todas)
     */
    List<ConfigComisionesBanco> obtenerConfigComisionesBanco(long idConfig);

    /**
     * Obtener configuraciones de comisiones filtradas por banco
     * @param codBanco Código de banco
     */
    List<ConfigComisionesBanco> obtenerConfigPorBanco(int codBanco);
}

