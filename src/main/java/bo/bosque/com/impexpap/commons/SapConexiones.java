package bo.bosque.com.impexpap.commons;

import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.dao.ISapConexion;
import bo.bosque.com.impexpap.dto.SapConexionOpcion;
import bo.bosque.com.impexpap.model.SapConexion;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Las conexiones a SAP que comparten todos los módulos ({@code tsap_Conexion}): dónde corren los SP
 * (linked server y base) y con qué usuario se entra al Service Layer.
 *
 * <p><b>Cómo lo usa un módulo.</b> Guarda en su configuración solo el {@code idConexion} (y lo suyo:
 * SP, columnas, textos). Para el desplegable de su pantalla, {@link #opciones()}, detrás de SU pantalla:
 * no ve URL, usuario ni contraseña. Para trabajar:
 * <pre>
 * SapConexion cx = sapConexiones.vigente(cfg.getIdConexion());
 * String sp = "EXEC " + SapConexiones.nombreSp(cx, "dbo", "spMiSp") + " @Desde = ?";   // por el linked server
 * Integer n = sapConexiones.sesion(cx, s -&gt; {                                           // por el Service Layer
 *     Map&lt;String, Object&gt; r = s.post("JournalEntries", cuerpo, "crear el asiento");
 *     return ((Number) r.get("JdtNum")).intValue();
 * });
 * </pre>
 * La sesión se cierra siempre, aunque el trabajo falle. La contraseña se cifra al guardar
 * (AES-256-GCM, {@code CONFIG_CIFRADO_CLAVE}) y nunca sale hacia la app.
 */
@Service
public class SapConexiones {

    /** La pantalla que administra las conexiones, bajo Administración. */
    public static final String PANTALLA = "tsapConexion/Conexiones";

    /** Lo que puede ir entre corchetes en un EXEC: servidor, base, esquema y SP. */
    static final Pattern NOMBRE = Pattern.compile("[A-Za-z0-9_ .-]{1,128}");
    static final Pattern URL = Pattern.compile("https?://[^\\s]+");

    /** El largo de {@code tsap_Conexion.slClave}: más que eso SQL Server lo recortaría. */
    private static final int LARGO_CLAVE_CIFRADA = 500;
    private static final int LARGO_NOMBRE = 100;

    private final ISapConexion dao;
    private final CifradoConfiguracion cifrado;
    private final SapServiceLayerCliente cliente;

    public SapConexiones(ISapConexion dao, CifradoConfiguracion cifrado, SapServiceLayerCliente cliente) {
        this.dao = dao;
        this.cifrado = cifrado;
        this.cliente = cliente;
    }

    // ── La pantalla de conexiones ──────────────────────────────────────────────────────

    /** Todas; de la contraseña solo se dice si está cargada. */
    public List<SapConexion> paraPantalla() {
        List<SapConexion> filas = dao.listar();
        for (SapConexion c : filas) {
            c.setClaveCargada(!vacio(c.getSlClave()));
            c.setSlClave(null);
        }
        return filas;
    }

    /** Alta ({@code idConexion} null) o cambio. No toca la contraseña, que tiene su propio paso. */
    public long guardar(SapConexion c, long codUsuario) {
        if (c == null) throw new SpBusinessException("Faltan los datos de la conexión.");
        validar(c);
        long id = dao.guardar(c, codUsuario).getIdGenerado();
        return c.getIdConexion() == null ? id : c.getIdConexion();
    }

    /** Una contraseña puede tener espacios: es el único valor que no se recorta. */
    public void guardarClave(int idConexion, String clave, long codUsuario) {
        if (clave == null || clave.isEmpty()) throw new SpBusinessException("Escribe la contraseña.");
        String cifrada = cifrado.cifrar(clave);
        if (cifrada.length() > LARGO_CLAVE_CIFRADA) {
            throw new SpBusinessException("La contraseña es demasiado larga para guardarla cifrada; usa una de menos de 340 caracteres.");
        }
        dao.guardarClave(idConexion, cifrada, codUsuario);
    }

    public void eliminar(int idConexion, long codUsuario) {
        dao.eliminar(idConexion, codUsuario);
    }

    /** Inicia y cierra sesión con lo guardado, aunque esté deshabilitada: para comprobarla sin enviar nada. */
    public String probar(int idConexion) {
        SapConexion c = existente(idConexion);
        validar(c);
        sesion(c, s -> null);
        return "Conexión correcta: " + c.getSlUsuario().trim() + " entró a " + c.getBaseSap().trim() + ".";
    }

    // ── Para los módulos ───────────────────────────────────────────────────────────────

    /** Para el desplegable de un módulo: sin URL, usuario ni contraseña. */
    public List<SapConexionOpcion> opciones() {
        List<SapConexionOpcion> opciones = new ArrayList<>();
        for (SapConexion c : dao.listar()) {
            opciones.add(SapConexionOpcion.builder()
                    .idConexion(c.getIdConexion())
                    .nombre(c.getNombre())
                    .codEmpresa(c.getCodEmpresa())
                    .empresa(c.getEmpresa())
                    .ambiente(c.getAmbiente())
                    .baseSap(c.getBaseSap())
                    .habilitada(Boolean.TRUE.equals(c.getHabilitada()))
                    .claveCargada(!vacio(c.getSlClave()))
                    .build());
        }
        return opciones;
    }

    /**
     * La conexión lista para usar: existe, está habilitada y sus nombres son válidos. La contraseña se
     * exige recién en {@link #sesion}: correr un SP por el linked server no la necesita.
     */
    public SapConexion vigente(int idConexion) {
        SapConexion c = existente(idConexion);
        if (!Boolean.TRUE.equals(c.getHabilitada())) {
            throw new SpBusinessException("La conexión de SAP «" + c.getNombre() + "» está deshabilitada "
                    + "en Administración > Conexiones SAP.");
        }
        validar(c);
        return c;
    }

    /**
     * Abre una sesión en el Service Layer de la conexión, ejecuta {@code trabajo} y la cierra siempre,
     * aunque el trabajo falle.
     *
     * @param c una conexión de {@link #vigente}: trae la contraseña cifrada
     */
    public <T> T sesion(SapConexion c, Function<SapSesion, T> trabajo) {
        SapServiceLayerCliente.Sesion s =
                cliente.abrirSesion(c.getSlUrl().trim(), c.getBaseSap().trim(), c.getSlUsuario().trim(), clave(c));
        try {
            return trabajo.apply(new SapSesion(cliente, s, c));
        } finally {
            cliente.cerrarSesion(s);
        }
    }

    public <T> T sesion(int idConexion, Function<SapSesion, T> trabajo) {
        return sesion(vigente(idConexion), trabajo);
    }

    /**
     * {@code [linked].[base].[esquema].[sp]} para un {@code EXEC}, con cada nombre validado: ninguno
     * puede cerrar un corchete. Sin linked server, {@code [base].[esquema].[sp]}.
     */
    public static String nombreSp(SapConexion c, String esquema, String sp) {
        validar(c);
        String e = vacio(esquema) ? "dbo" : esquema.trim();
        exigir(NOMBRE, e, "Esquema");
        exigir(NOMBRE, sp, "SP");
        StringBuilder sb = new StringBuilder();
        if (!vacio(c.getLinkedServer())) {
            sb.append('[').append(c.getLinkedServer().trim()).append("].");
        }
        return sb.append('[').append(c.getBaseSap().trim()).append("].[").append(e)
                 .append("].[").append(sp.trim()).append(']').toString();
    }

    /** Lo que se valida al guardar y otra vez antes de usarla: los nombres van a un EXEC. */
    static void validar(SapConexion c) {
        if (vacio(c.getNombre())) throw new SpBusinessException("Escribe el nombre de la conexión.");
        if (c.getNombre().trim().length() > LARGO_NOMBRE) {
            throw new SpBusinessException("El nombre de la conexión es de hasta " + LARGO_NOMBRE + " caracteres.");
        }
        if (c.getCodEmpresa() == null) throw new SpBusinessException("Elige la empresa.");
        if (!SapConexion.PRODUCCION.equals(c.getAmbiente()) && !SapConexion.PRUEBA.equals(c.getAmbiente())) {
            throw new SpBusinessException("Elige si la conexión es de producción o de prueba.");
        }
        if (!vacio(c.getLinkedServer())) exigir(NOMBRE, c.getLinkedServer(), "Linked server");
        exigir(NOMBRE, c.getBaseSap(), "Base de SAP");
        exigir(URL, c.getSlUrl(), "URL del Service Layer");
        if (vacio(c.getSlUsuario())) throw new SpBusinessException("Escribe el usuario del Service Layer.");
    }

    private SapConexion existente(int idConexion) {
        SapConexion c = dao.obtener(idConexion);
        if (c == null) {
            throw new SpBusinessException("La conexión de SAP " + idConexion + " no existe. "
                    + "Revisa Administración > Conexiones SAP.");
        }
        return c;
    }

    /** La contraseña del Service Layer, descifrada. */
    private String clave(SapConexion c) {
        if (vacio(c.getSlClave())) {
            throw new SpBusinessException("Falta cargar la contraseña de la conexión de SAP «" + c.getNombre()
                    + "» en Administración > Conexiones SAP.");
        }
        try {
            return cifrado.descifrar(c.getSlClave());
        } catch (IllegalStateException e) {
            throw new SpBusinessException("No se pudo leer la contraseña guardada de la conexión «" + c.getNombre()
                    + "» (¿cambió CONFIG_CIFRADO_CLAVE?). Vuelve a cargarla en Administración > Conexiones SAP.");
        }
    }

    static void exigir(Pattern patron, String valor, String campo) {
        if (valor == null || !patron.matcher(valor.trim()).matches()) {
            throw new SpBusinessException(campo + ": '" + (valor == null ? "" : valor.trim()) + "' no es válido.");
        }
    }

    private static boolean vacio(String s) {
        return s == null || s.trim().isEmpty();
    }
}
