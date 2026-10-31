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
 * La configuración del asiento de planilla en SAP por empresa ({@code tsap_AsientoPlanilla}).
 *
 * <p>Se lee en cada uso, así un cambio desde la pantalla rige en el siguiente envío, sin reiniciar.
 * La contraseña del Service Layer se cifra al guardar (AES-256-GCM, {@code CONFIG_CIFRADO_CLAVE}) y
 * nunca vuelve a la app, igual que la del correo de recuperación.
 */
@Service
public class AsientoSapConfigService {

    /** Lo que puede ir entre corchetes en el EXEC: servidor, base, esquema y SP. */
    static final Pattern NOMBRE = Pattern.compile("[A-Za-z0-9_ .-]{1,128}");
    /** Parámetro de SP o columna del resultado; admite la ñ de {@code @Añio}. */
    static final Pattern IDENTIFICADOR = Pattern.compile("[\\p{L}_][\\p{L}\\p{N}_]{0,127}");
    /** Propiedad de JournalEntries, que va dentro de un {@code $filter}. */
    static final Pattern CAMPO_SAP = Pattern.compile("[A-Za-z][A-Za-z0-9_]{0,63}");
    static final Pattern PREFIJOS = Pattern.compile("[A-Za-z0-9]{1,20}(,[A-Za-z0-9]{1,20})*");
    static final Pattern URL = Pattern.compile("https?://[^\\s]+");

    /** El largo de {@code tsap_AsientoPlanilla.slClave}: más que eso SQL Server lo recortaría. */
    private static final int LARGO_CLAVE_CIFRADA = 500;

    private final IAsientoSapConfig dao;
    private final CifradoConfiguracion cifrado;

    public AsientoSapConfigService(IAsientoSapConfig dao, CifradoConfiguracion cifrado) {
        this.dao = dao;
        this.cifrado = cifrado;
    }

    /** Para la pantalla: de la contraseña solo se dice si está cargada. */
    public List<AsientoSapConfig> paraPantalla() {
        List<AsientoSapConfig> filas = dao.listar();
        for (AsientoSapConfig f : filas) {
            f.setClaveCargada(!vacio(f.getSlClave()));
            f.setSlClave(null);
        }
        return filas;
    }

    /** Las empresas que se pueden enviar: habilitadas y con la contraseña cargada. */
    public List<Map<String, Object>> empresasHabilitadas() {
        List<Map<String, Object>> empresas = new ArrayList<>();
        for (AsientoSapConfig f : dao.listar()) {
            if (Boolean.TRUE.equals(f.getHabilitado()) && !vacio(f.getSlClave())) {
                Map<String, Object> e = new LinkedHashMap<>();
                e.put("codEmpresa", f.getCodEmpresa());
                e.put("empresa", f.getEmpresa());
                empresas.add(e);
            }
        }
        return empresas;
    }

    /** La configuración con la que se arma y envía el asiento de esa empresa, ya validada. */
    public AsientoSapConfig vigente(int codEmpresa) {
        AsientoSapConfig c = dao.obtener(codEmpresa);
        if (c == null) {
            throw new SpBusinessException("La empresa " + codEmpresa + " no tiene configurado el asiento en SAP. "
                    + "Agrégala en Planillas > Asiento SAP: configuración.");
        }
        if (!Boolean.TRUE.equals(c.getHabilitado())) {
            throw new SpBusinessException("El asiento en SAP de " + nombre(c) + " está deshabilitado en la configuración.");
        }
        validar(c);
        return c;
    }

    /** La contraseña del Service Layer de esa configuración, descifrada. */
    public String clave(AsientoSapConfig c) {
        if (vacio(c.getSlClave())) {
            throw new SpBusinessException("Falta cargar la contraseña del Service Layer de " + nombre(c)
                    + " en Planillas > Asiento SAP: configuración.");
        }
        try {
            return cifrado.descifrar(c.getSlClave());
        } catch (IllegalStateException e) {
            throw new SpBusinessException("No se pudo leer la contraseña guardada del Service Layer de " + nombre(c)
                    + " (¿cambió CONFIG_CIFRADO_CLAVE?). Vuelve a cargarla en la configuración.");
        }
    }

    public void guardar(AsientoSapConfig c, long codUsuario) {
        if (c == null || c.getCodEmpresa() == null) throw new SpBusinessException("Elige la empresa.");
        validar(c);
        dao.guardar(c, codUsuario);
    }

    /** Una contraseña puede tener espacios: es el único valor que no se recorta. */
    public void guardarClave(int codEmpresa, String clave, long codUsuario) {
        if (clave == null || clave.isEmpty()) throw new SpBusinessException("Escribe la contraseña.");
        String cifrada = cifrado.cifrar(clave);
        if (cifrada.length() > LARGO_CLAVE_CIFRADA) {
            throw new SpBusinessException("La contraseña es demasiado larga para guardarla cifrada; usa una de menos de 340 caracteres.");
        }
        dao.guardarClave(codEmpresa, cifrada, codUsuario);
    }

    public void eliminar(int codEmpresa, long codUsuario) {
        dao.eliminar(codEmpresa, codUsuario);
    }

    /**
     * Lo que se valida al guardar y otra vez antes de cada envío: los nombres van al EXEC y al
     * {@code $filter} de SAP, así que solo pasan los que no pueden cerrar un corchete ni una comilla.
     */
    static void validar(AsientoSapConfig c) {
        exigir(NOMBRE, c.getBaseSap(), "Base de SAP");
        if (!vacio(c.getLinkedServer())) exigir(NOMBRE, c.getLinkedServer(), "Linked server");
        if (!vacio(c.getEsquema())) exigir(NOMBRE, c.getEsquema(), "Esquema");
        exigir(NOMBRE, c.getSp(), "SP");
        exigir(IDENTIFICADOR, c.getParamAnio(), "Parámetro del año");
        exigir(IDENTIFICADOR, c.getParamMes(), "Parámetro del mes");
        exigir(IDENTIFICADOR, c.getColCodigo(), "Columna del código");
        exigir(IDENTIFICADOR, c.getColNombre(), "Columna del nombre");
        exigir(IDENTIFICADOR, c.getColCuenta(), "Columna de la cuenta asociada");
        exigir(IDENTIFICADOR, c.getColDebito(), "Columna del débito");
        exigir(IDENTIFICADOR, c.getColCredito(), "Columna del crédito");
        if (!vacio(c.getPrefijosSocio())) exigir(PREFIJOS, c.getPrefijosSocio().replace(" ", ""), "Prefijos de socio");
        if (vacio(c.getMemo())) throw new SpBusinessException("Escribe el memo del asiento.");
        if (vacio(c.getReferencia())) throw new SpBusinessException("Escribe la referencia del asiento.");
        if (!vacio(c.getCampoReferencia())) exigir(CAMPO_SAP, c.getCampoReferencia(), "Campo de la referencia");
        if (Boolean.TRUE.equals(c.getEvitarDuplicados()) && vacio(c.getCampoReferencia())) {
            throw new SpBusinessException("Para evitar duplicados hace falta el campo de la referencia (por ejemplo Reference3).");
        }
        exigir(URL, c.getSlUrl(), "URL del Service Layer");
        if (vacio(c.getSlUsuario())) throw new SpBusinessException("Escribe el usuario del Service Layer.");
    }

    private static void exigir(Pattern patron, String valor, String campo) {
        if (valor == null || !patron.matcher(valor.trim()).matches()) {
            throw new SpBusinessException(campo + ": '" + (valor == null ? "" : valor.trim()) + "' no es válido.");
        }
    }

    private static String nombre(AsientoSapConfig c) {
        return vacio(c.getEmpresa()) ? "la empresa " + c.getCodEmpresa() : c.getEmpresa();
    }

    private static boolean vacio(String s) {
        return s == null || s.trim().isEmpty();
    }
}
