package bo.bosque.com.impexpap.commons;

import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.dao.IAsientoSapConfig;
import bo.bosque.com.impexpap.model.AsientoSapConfig;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * La configuración del asiento de planilla en SAP por empresa ({@code tsap_AsientoPlanilla}): el SP,
 * sus columnas, los textos del asiento y a qué conexión de SAP va.
 *
 * <p>Se lee en cada uso, así un cambio desde la pantalla rige en el siguiente envío, sin reiniciar. La
 * conexión (base, linked server, usuario y contraseña del Service Layer) es de {@link SapConexiones}.
 */
@Service
public class AsientoSapConfigService {

    /** Parámetro de SP o columna del resultado; admite la ñ de {@code @Añio}. */
    static final Pattern IDENTIFICADOR = Pattern.compile("[\\p{L}_][\\p{L}\\p{N}_]{0,127}");
    /** Propiedad de JournalEntries, que va dentro de un {@code $filter}. */
    static final Pattern CAMPO_SAP = Pattern.compile("[A-Za-z][A-Za-z0-9_]{0,63}");
    static final Pattern PREFIJOS = Pattern.compile("[A-Za-z0-9]{1,20}(,[A-Za-z0-9]{1,20})*");

    private final IAsientoSapConfig dao;

    public AsientoSapConfigService(IAsientoSapConfig dao) {
        this.dao = dao;
    }

    /** Para la pantalla. De la conexión llega solo lo que se muestra: la contraseña no sale de la base. */
    public List<AsientoSapConfig> paraPantalla() {
        return dao.listar();
    }

    /** Las empresas que se pueden enviar: habilitadas, con su conexión habilitada y con la contraseña cargada. */
    public List<Map<String, Object>> empresasHabilitadas() {
        List<Map<String, Object>> empresas = new ArrayList<>();
        for (AsientoSapConfig f : dao.listar()) {
            if (Boolean.TRUE.equals(f.getHabilitado()) && Boolean.TRUE.equals(f.getConexionHabilitada())
                    && Boolean.TRUE.equals(f.getClaveCargada())) {
                Map<String, Object> e = new LinkedHashMap<>();
                e.put("codEmpresa", f.getCodEmpresa());
                e.put("empresa", f.getEmpresa());
                empresas.add(e);
            }
        }
        return empresas;
    }

    /** La configuración guardada de esa empresa, habilitada o no. */
    public AsientoSapConfig existente(int codEmpresa) {
        AsientoSapConfig c = dao.obtener(codEmpresa);
        if (c == null) {
            throw new SpBusinessException("La empresa " + codEmpresa + " no tiene configurado el asiento en SAP. "
                    + "Agrégala en Planillas > Asiento SAP: configuración.");
        }
        return c;
    }

    /** La configuración con la que se arma y envía el asiento de esa empresa, ya validada. */
    public AsientoSapConfig vigente(int codEmpresa) {
        AsientoSapConfig c = existente(codEmpresa);
        if (!Boolean.TRUE.equals(c.getHabilitado())) {
            throw new SpBusinessException("El asiento en SAP de " + nombre(c) + " está deshabilitado en la configuración.");
        }
        validar(c);
        return c;
    }

    /** La base de datos comprueba además que la conexión sea de la misma empresa. */
    public void guardar(AsientoSapConfig c, long codUsuario) {
        if (c == null || c.getCodEmpresa() == null) throw new SpBusinessException("Elige la empresa.");
        validar(c);
        dao.guardar(c, codUsuario);
    }

    public void eliminar(int codEmpresa, long codUsuario) {
        dao.eliminar(codEmpresa, codUsuario);
    }

    /**
     * Lo que se valida al guardar y otra vez antes de cada envío: los nombres van al EXEC y al
     * {@code $filter} de SAP, así que solo pasan los que no pueden cerrar un corchete ni una comilla.
     */
    static void validar(AsientoSapConfig c) {
        if (c.getIdConexion() == null) throw new SpBusinessException("Elige la conexión de SAP.");
        if (!vacio(c.getEsquema())) SapConexiones.exigir(SapConexiones.NOMBRE, c.getEsquema(), "Esquema");
        SapConexiones.exigir(SapConexiones.NOMBRE, c.getSp(), "SP");
        SapConexiones.exigir(IDENTIFICADOR, c.getParamAnio(), "Parámetro del año");
        SapConexiones.exigir(IDENTIFICADOR, c.getParamMes(), "Parámetro del mes");
        SapConexiones.exigir(IDENTIFICADOR, c.getColCodigo(), "Columna del código");
        SapConexiones.exigir(IDENTIFICADOR, c.getColNombre(), "Columna del nombre");
        SapConexiones.exigir(IDENTIFICADOR, c.getColCuenta(), "Columna de la cuenta asociada");
        SapConexiones.exigir(IDENTIFICADOR, c.getColDebito(), "Columna del débito");
        SapConexiones.exigir(IDENTIFICADOR, c.getColCredito(), "Columna del crédito");
        if (!vacio(c.getPrefijosSocio())) {
            SapConexiones.exigir(PREFIJOS, c.getPrefijosSocio().replace(" ", ""), "Prefijos de socio");
        }
        if (vacio(c.getMemo())) throw new SpBusinessException("Escribe el memo del asiento.");
        if (vacio(c.getReferencia())) throw new SpBusinessException("Escribe la referencia del asiento.");
        if (!vacio(c.getCampoReferencia())) SapConexiones.exigir(CAMPO_SAP, c.getCampoReferencia(), "Campo de la referencia");
        if (Boolean.TRUE.equals(c.getEvitarDuplicados()) && vacio(c.getCampoReferencia())) {
            throw new SpBusinessException("Para evitar duplicados hace falta el campo de la referencia (por ejemplo Reference3).");
        }
    }

    private static String nombre(AsientoSapConfig c) {
        return vacio(c.getEmpresa()) ? "la empresa " + c.getCodEmpresa() : c.getEmpresa();
    }

    private static boolean vacio(String s) {
        return s == null || s.trim().isEmpty();
    }
}
