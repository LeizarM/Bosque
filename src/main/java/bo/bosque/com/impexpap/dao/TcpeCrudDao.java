package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Base de los 38 DAO {@code tcpe_}: cada uno declara su PK, si es IDENTITY, los filtros del {@code p_list} y las
 * columnas del {@code p_abm}. Se manda un {@code Map} con los parámetros exactos, nunca el modelo: SQL Server rechaza
 * los que el SP no declara. En el ABM lo nulo no se manda (toma el default del SP); la PK va siempre.
 */
public abstract class TcpeCrudDao<T> implements ITcpeCrud<T> {

    private final SpHelper spHelper;
    private final Class<T> tipo;
    private final String entidad;
    private final String pk;
    private final boolean pkIdentidad;
    private final List<String> filtros;
    private final List<String> columnas;
    private final String spList;
    private final String spAbm;

    protected TcpeCrudDao(SpHelper spHelper, Class<T> tipo, String entidad, String pk, boolean pkIdentidad,
                          String[] filtros, String[] columnas) {
        this.spHelper = spHelper;
        this.tipo = tipo;
        this.entidad = entidad;
        this.pk = pk;
        this.pkIdentidad = pkIdentidad;
        this.filtros = Collections.unmodifiableList(Arrays.asList(filtros));
        this.columnas = Collections.unmodifiableList(Arrays.asList(columnas));
        String sufijo = Character.toUpperCase(entidad.charAt(0)) + entidad.substring(1);
        this.spList = "p_list_tcpe_" + sufijo;
        this.spAbm = "p_abm_tcpe_" + sufijo;
    }

    @Override
    public List<T> listar(T filtro) {
        Map<String, Object> p = new LinkedHashMap<>();
        if (filtro != null) {
            BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(filtro);
            for (String f : filtros) {
                TcpeParametros.filtro(p, f, bw.getPropertyValue(f));
            }
        }
        return spHelper.ejecutarListado(spList, p, "L", tipo);
    }

    @Override
    public T obtener(Long id) {
        Long cod = TcpeParametros.id(id);
        if (cod == null) return null;
        Map<String, Object> p = new LinkedHashMap<>();
        p.put(pk, cod);
        List<T> filas = spHelper.ejecutarListado(spList, p, "L", tipo);
        return filas.isEmpty() ? null : filas.get(0);
    }

    @Override
    public RespuestaSp guardar(T modelo, long audUsuario) {
        if (modelo == null) throw new SpBusinessException("No llegaron los datos a guardar.");
        BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(modelo);
        Long id = TcpeParametros.id(aLargo(bw.getPropertyValue(pk)));

        // PK sin IDENTITY (conocimientosPost, empleado, postulacion): sin código el SP asigna el siguiente número;
        // con código, es alta si todavía no existe y modificación si existe (CONTRATO_CRUD.md).
        String accion;
        if (id == null) {
            accion = "I";
        } else if (pkIdentidad) {
            accion = "U";
        } else {
            accion = obtener(id) == null ? "I" : "U";
        }

        Map<String, Object> p = new LinkedHashMap<>();
        p.put(pk, id == null ? 0L : id);
        for (String c : columnas) {
            TcpeParametros.poner(p, c, bw.getPropertyValue(c));
        }
        p.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(spAbm, p, accion);
    }

    @Override
    public RespuestaSp eliminar(Long id, long audUsuario) {
        Long cod = TcpeParametros.id(id);
        if (cod == null) throw new SpBusinessException("Indique el registro a eliminar (" + pk + ").");
        Map<String, Object> p = new LinkedHashMap<>();
        p.put(pk, cod);
        p.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(spAbm, p, "D");
    }

    @Override
    public String entidad() {
        return entidad;
    }

    @Override
    public Class<T> tipo() {
        return tipo;
    }

    @Override
    public String pk() {
        return pk;
    }

    /** Parámetros que se le mandan a {@code p_list} como filtro (para pruebas y diagnóstico). */
    public List<String> filtros() {
        return filtros;
    }

    /** Columnas de datos que se le mandan a {@code p_abm} (para pruebas y diagnóstico). */
    public List<String> columnas() {
        return columnas;
    }

    private static Long aLargo(Object v) {
        return v instanceof Number ? ((Number) v).longValue() : null;
    }
}
