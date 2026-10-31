package bo.bosque.com.impexpap.commons;

import bo.bosque.com.impexpap.config.SpBusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cliente mínimo del Service Layer de SAP B1: sesión, buscar asientos por referencia, crear un asiento.
 * La URL, la base y el usuario son de cada empresa; el truststore es uno solo para todo el servidor.
 *
 * <p>El certificado del Service Layer es autofirmado. El script de Node lo resolvía apagando la
 * verificación ({@code rejectUnauthorized: false}); aquí no hay esa opción: el certificado se agrega
 * a un truststore ({@code sap.asiento-planilla.service-layer.truststore}) y se verifica como cualquier otro.
 */
@Component
public class SapServiceLayerCliente {

    private static final Logger log = LoggerFactory.getLogger(SapServiceLayerCliente.class);

    private static final ParameterizedTypeReference<Map<String, Object>> MAPA =
            new ParameterizedTypeReference<Map<String, Object>>() {
            };

    /** Una sesión abierta: a qué Service Layer y con qué cookie. */
    public static final class Sesion {
        private final String url;
        private final String cookie;

        Sesion(String url, String cookie) {
            this.url = url;
            this.cookie = cookie;
        }
    }

    private final AsientoPlanillaSapPropiedades props;
    private final ObjectMapper mapper;

    /** Se arma en el primer uso: un truststore mal configurado no debe impedir que arranque el backend. */
    private volatile RestTemplate rest;

    public SapServiceLayerCliente(AsientoPlanillaSapPropiedades props, ObjectMapper mapper) {
        this.props = props;
        this.mapper = mapper;
    }

    /** Inicia sesión en {@code companyDb}. */
    public Sesion abrirSesion(String url, String companyDb, String usuario, String clave) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("CompanyDB", companyDb);
        cuerpo.put("UserName", usuario);
        cuerpo.put("Password", clave);
        ResponseEntity<Map<String, Object>> r =
                llamar(HttpMethod.POST, uri(url, "Login"), null, cuerpo, "iniciar sesión", "");

        // B1SESSION y, detrás de un balanceador, ROUTEID. Solo el nombre=valor de cada uno.
        List<String> partes = new ArrayList<>();
        List<String> setCookie = r.getHeaders().get(HttpHeaders.SET_COOKIE);
        if (setCookie != null) {
            for (String c : setCookie) {
                String par = c.split(";", 2)[0].trim();
                if (!par.isEmpty()) partes.add(par);
            }
        }
        if (partes.isEmpty() && r.getBody() != null && r.getBody().get("SessionId") != null) {
            partes.add("B1SESSION=" + r.getBody().get("SessionId"));
        }
        if (partes.isEmpty()) {
            throw new SpBusinessException("SAP aceptó el inicio de sesión pero no devolvió la sesión.");
        }
        return new Sesion(url, String.join("; ", partes));
    }

    /** Los {@code JdtNum} de los asientos que ya tienen esa referencia en ese campo. */
    public List<Integer> asientosConReferencia(Sesion s, String campo, String referencia) {
        URI u = base(s.url).pathSegment("JournalEntries")
                .queryParam("$select", "JdtNum")
                .queryParam("$filter", campo + " eq '" + referencia.replace("'", "''") + "'")
                .build().encode().toUri();
        ResponseEntity<Map<String, Object>> r = llamar(HttpMethod.GET, u, s.cookie, null, "buscar asientos anteriores", "");
        List<Integer> numeros = new ArrayList<>();
        Object valor = r.getBody() == null ? null : r.getBody().get("value");
        if (valor instanceof List) {
            for (Object o : (List<?>) valor) {
                Object n = o instanceof Map ? ((Map<?, ?>) o).get("JdtNum") : null;
                if (n instanceof Number) numeros.add(((Number) n).intValue());
            }
        }
        return numeros;
    }

    /** Crea el asiento y devuelve su {@code JdtNum}. */
    public int crearAsiento(Sesion s, Map<String, Object> asiento) {
        ResponseEntity<Map<String, Object>> r = llamar(HttpMethod.POST, uri(s.url, "JournalEntries"), s.cookie, asiento,
                "crear el asiento", " Revisa en SAP si el asiento se creó antes de volver a enviarlo.");
        Object n = r.getBody() == null ? null : r.getBody().get("JdtNum");
        if (!(n instanceof Number)) {
            throw new SpBusinessException("SAP respondió sin el número del asiento (JdtNum). "
                    + "Revisa en SAP si se creó antes de volver a enviarlo.");
        }
        return ((Number) n).intValue();
    }

    /** Nunca lanza: si falla, la sesión vence sola en el Service Layer. */
    public void cerrarSesion(Sesion s) {
        try {
            llamar(HttpMethod.POST, uri(s.url, "Logout"), s.cookie, null, "cerrar sesión", "");
        } catch (RuntimeException e) {
            log.debug("No se pudo cerrar la sesión del Service Layer: {}", e.getMessage());
        }
    }

    private ResponseEntity<Map<String, Object>> llamar(HttpMethod metodo, URI uri, String cookie, Object cuerpo,
                                                       String paraQue, String siSeCorta) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        if (cookie != null) h.add(HttpHeaders.COOKIE, cookie);
        try {
            return cliente().exchange(uri, metodo, new HttpEntity<>(cuerpo, h), MAPA);
        } catch (HttpStatusCodeException e) {
            throw new SpBusinessException("SAP respondió con error al " + paraQue + ": " + mensajeSap(e));
        } catch (ResourceAccessException e) {
            String causa = e.getMostSpecificCause().getMessage();
            String pista = causa != null && (causa.contains("PKIX") || causa.contains("certification path"))
                    ? " Falta el certificado del Service Layer en el truststore (sap.asiento-planilla.service-layer.truststore)."
                    : "";
            throw new SpBusinessException("No se pudo conectar con el Service Layer de SAP al " + paraQue
                    + " (" + causa + ")." + pista + siSeCorta);
        }
    }

    /** El texto de {@code error.message} (v1: {@code {lang, value}}; v2: texto). */
    private String mensajeSap(HttpStatusCodeException e) {
        try {
            JsonNode m = mapper.readTree(e.getResponseBodyAsString()).path("error").path("message");
            String texto = m.isTextual() ? m.asText() : m.path("value").asText("");
            if (!texto.isEmpty()) return texto;
        } catch (IOException | RuntimeException noEraJson) {
            // cae al código HTTP
        }
        return "HTTP " + e.getRawStatusCode();
    }

    private static URI uri(String url, String recurso) {
        return base(url).pathSegment(recurso).build().toUri();
    }

    private static UriComponentsBuilder base(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new SpBusinessException("Falta la URL del Service Layer en la configuración de la empresa.");
        }
        return UriComponentsBuilder.fromHttpUrl(url.trim());
    }

    private RestTemplate cliente() {
        RestTemplate r = rest;
        if (r == null) {
            synchronized (this) {
                r = rest;
                if (r == null) {
                    r = new RestTemplate(fabrica(props.getServiceLayer()));
                    rest = r;
                }
            }
        }
        return r;
    }

    private static SimpleClientHttpRequestFactory fabrica(AsientoPlanillaSapPropiedades.ServiceLayer sl) {
        final SSLSocketFactory ssl = vacio(sl.getTruststore()) ? null : socketsDe(sl);
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection conexion, String metodo) throws IOException {
                if (ssl != null && conexion instanceof HttpsURLConnection) {
                    ((HttpsURLConnection) conexion).setSSLSocketFactory(ssl);
                }
                super.prepareConnection(conexion, metodo);
            }
        };
        f.setConnectTimeout(sl.getTimeoutConexionMs());
        f.setReadTimeout(sl.getTimeoutLecturaMs());
        return f;
    }

    private static SSLSocketFactory socketsDe(AsientoPlanillaSapPropiedades.ServiceLayer sl) {
        String ruta = sl.getTruststore().trim();
        try (InputStream in = new FileInputStream(ruta)) {
            KeyStore ks = KeyStore.getInstance(ruta.toLowerCase().endsWith(".p12") ? "PKCS12" : "JKS");
            ks.load(in, vacio(sl.getTruststorePassword()) ? null : sl.getTruststorePassword().toCharArray());
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ks);
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, tmf.getTrustManagers(), null);
            return ctx.getSocketFactory();
        } catch (Exception e) {
            throw new SpBusinessException("No se pudo leer el truststore del Service Layer (" + ruta + "): "
                    + e.getMessage());
        }
    }

    private static boolean vacio(String s) {
        return s == null || s.trim().isEmpty();
    }
}
