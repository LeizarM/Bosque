package bo.bosque.com.impexpap.dao;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import bo.bosque.com.impexpap.model.Ciudad;
import bo.bosque.com.impexpap.model.Pais;
import bo.bosque.com.impexpap.model.Zona;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;

/**
 * Acceso a {@code trh_pais}, {@code trh_ciudad} y {@code trh_zona} vía {@link SpHelper}.
 * Los DAO viejos ({@code PaisDao}, {@code CiudadDao}, {@code ZonaDao}) siguen sirviendo a la ficha.
 */
@Repository
public class UbicacionGeograficaDao implements IUbicacionGeografica {

    private final SpHelper spHelper;

    public UbicacionGeograficaDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public List<Pais> listarPaises() {
        return spHelper.ejecutarListado("p_list_Pais", new LinkedHashMap<String, Object>(), "P", Pais.class);
    }

    @Override
    public List<Ciudad> listarCiudades(long codPais) {
        Map<String, Object> p = new LinkedHashMap<>();
        if (codPais > 0) p.put("codPais", codPais);
        // p_list_Ciudad declara el parámetro como @Accion; SpHelper agrega @ACCION y SQL Server no distingue.
        return spHelper.ejecutarListado("p_list_Ciudad", p, "P", Ciudad.class);
    }

    @Override
    public List<Zona> listarZonas(long codCiudad) {
        Map<String, Object> p = new LinkedHashMap<>();
        if (codCiudad > 0) p.put("codCiudad", codCiudad);
        return spHelper.ejecutarListado("p_list_Zona", p, "P", Zona.class);
    }

    @Override
    public RespuestaSp registrarPais(Pais pais) {
        boolean alta = pais.getCodPais() == 0;
        Map<String, Object> p = new LinkedHashMap<>();
        if (!alta) p.put("codPais", pais.getCodPais());
        p.put("pais", pais.getPais());
        p.put("audUsuarioI", pais.getAudUsuario());
        return spHelper.ejecutarAbmMap("p_abm_Pais", p, alta ? "I" : "U");
    }

    @Override
    public RespuestaSp registrarCiudad(Ciudad ciudad) {
        boolean alta = ciudad.getCodCiudad() == 0;
        Map<String, Object> p = new LinkedHashMap<>();
        if (!alta) p.put("codCiudad", ciudad.getCodCiudad());
        p.put("codPais", ciudad.getCodPais());
        p.put("ciudad", ciudad.getCiudad());
        p.put("audUsuarioI", ciudad.getAudUsuario());
        return spHelper.ejecutarAbmMap("p_abm_Ciudad", p, alta ? "I" : "U");
    }

    @Override
    public RespuestaSp registrarZona(Zona zona) {
        boolean alta = zona.getCodZona() == 0;
        Map<String, Object> p = new LinkedHashMap<>();
        if (!alta) p.put("codZona", zona.getCodZona());
        p.put("codCiudad", zona.getCodCiudad());
        p.put("zona", zona.getZona());
        p.put("audUsuarioI", zona.getAudUsuario());
        return spHelper.ejecutarAbmMap("p_abm_Zona", p, alta ? "I" : "U");
    }

    @Override
    public RespuestaSp eliminarPais(long codPais, int audUsuario) {
        return eliminar("p_abm_Pais", "codPais", codPais, audUsuario);
    }

    @Override
    public RespuestaSp eliminarCiudad(long codCiudad, int audUsuario) {
        return eliminar("p_abm_Ciudad", "codCiudad", codCiudad, audUsuario);
    }

    @Override
    public RespuestaSp eliminarZona(long codZona, int audUsuario) {
        return eliminar("p_abm_Zona", "codZona", codZona, audUsuario);
    }

    private RespuestaSp eliminar(String sp, String clave, long id, int audUsuario) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put(clave, id);
        p.put("audUsuarioI", audUsuario);
        return spHelper.ejecutarAbmMap(sp, p, "D");
    }
}
