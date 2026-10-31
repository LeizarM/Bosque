package bo.bosque.com.impexpap.commons;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Lo del Service Layer de SAP que es del servidor y no de una conexión ({@code sap.service-layer.*}):
 * el truststore con los certificados autofirmados de los Service Layer y los tiempos de espera.
 *
 * <p>Lo de cada conexión (URL, base, usuario y contraseña cifrada) está en {@code tsap_Conexion} y se
 * edita desde Administración &gt; Conexiones SAP, sin reiniciar.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "sap.service-layer")
public class SapServiceLayerPropiedades {

    /** Archivo .jks con los certificados de los Service Layer. Vacío = el truststore de la JVM. */
    private String truststore = "";
    private String truststorePassword = "";
    private int timeoutConexionMs = 5000;
    private int timeoutLecturaMs = 60000;
}
