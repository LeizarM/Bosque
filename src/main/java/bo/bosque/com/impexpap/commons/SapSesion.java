package bo.bosque.com.impexpap.commons;

import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.model.SapConexion;
import org.springframework.http.HttpMethod;

import java.util.Map;

/**
 * Una sesión abierta en el Service Layer de una conexión, para el trabajo de un módulo. La abre y la
 * cierra {@link SapConexiones#sesion}: el módulo solo pide recursos.
 *
 * <p>{@code recurso} es la ruta debajo de la URL del Service Layer: {@code "JournalEntries"},
 * {@code "BusinessPartners('C0001')"}. Un error de SAP llega como {@link SpBusinessException} con el
 * mensaje de SAP, listo para mostrar. En una escritura que se corta, el mensaje pide revisar en SAP si
 * quedó hecha antes de repetirla: el Service Layer pudo haberla aplicado igual.
 */
public final class SapSesion {

    private static final String SI_SE_CORTA_ESCRITURA = " Revisa en SAP si quedó hecho antes de repetirlo.";

    private final SapServiceLayerCliente cliente;
    private final SapServiceLayerCliente.Sesion sesion;
    private final int idConexion;
    private final String nombre;
    private final String baseSap;
    private final String ambiente;

    SapSesion(SapServiceLayerCliente cliente, SapServiceLayerCliente.Sesion sesion, SapConexion conexion) {
        this.cliente = cliente;
        this.sesion = sesion;
        this.idConexion = conexion.getIdConexion() == null ? 0 : conexion.getIdConexion();
        this.nombre = conexion.getNombre();
        this.baseSap = conexion.getBaseSap();
        this.ambiente = conexion.getAmbiente();
    }

    /** @param query {@code $select}, {@code $filter}... sin codificar; puede ser {@code null} */
    public Map<String, Object> get(String recurso, Map<String, String> query, String paraQue) {
        return cliente.pedir(sesion, HttpMethod.GET, recurso, query, null, paraQue, "");
    }

    /** Crea: el cuerpo de la respuesta trae lo creado (por ejemplo el {@code JdtNum} de un asiento). */
    public Map<String, Object> post(String recurso, Object cuerpo, String paraQue) {
        return cliente.pedir(sesion, HttpMethod.POST, recurso, null, cuerpo, paraQue, SI_SE_CORTA_ESCRITURA);
    }

    // Sin PATCH: el cliente HTTP del JDK (HttpURLConnection) no lo acepta. El día que un módulo tenga
    // que modificar algo en SAP, agregar Apache HttpClient al pom, usar HttpComponentsClientHttpRequestFactory
    // en SapServiceLayerCliente.fabrica y sumar aquí un patch() igual a post().

    public int getIdConexion() {
        return idConexion;
    }

    public String getNombre() {
        return nombre;
    }

    /** {@code CompanyDB} en la que se inició la sesión. */
    public String getBaseSap() {
        return baseSap;
    }

    /** {@link SapConexion#PRODUCCION} o {@link SapConexion#PRUEBA}. */
    public String getAmbiente() {
        return ambiente;
    }
}
