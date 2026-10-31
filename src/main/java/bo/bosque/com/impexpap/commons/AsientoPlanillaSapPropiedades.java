package bo.bosque.com.impexpap.commons;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Lo del asiento de planilla en SAP que es del servidor y no de una empresa ({@code sap.asiento-planilla.*}).
 *
 * <p>Lo de cada empresa (SP, columnas, textos y su conexión a SAP) está en {@code tsap_AsientoPlanilla}
 * y se edita desde la pantalla {@code tplPlanilla/asientoSapConfig}; la conexión (base, usuario y
 * contraseña del Service Layer), en Administración &gt; Conexiones SAP. Un cambio rige al instante, sin
 * compilar ni reiniciar. El truststore del Service Layer está en {@link SapServiceLayerPropiedades}.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "sap.asiento-planilla")
public class AsientoPlanillaSapPropiedades {

    /** Interruptor general. Apagado, ninguna empresa envía, aunque esté habilitada en la tabla. */
    private boolean habilitado = true;

    /** {@code tb_vista.direccion} que dejan ver y enviar el asiento; basta una. Vacía = nadie. */
    private List<String> pantallas = new ArrayList<>(Collections.singletonList("tplPlanilla/planilla"));

    /** {@code tb_vista.direccion} de la pantalla de configuración. */
    private List<String> pantallasConfig = new ArrayList<>(Collections.singletonList("tplPlanilla/asientoSapConfig"));

    /** Tope del SP que arma el asiento: 0 sería "sin límite" y retendría una de las conexiones del pool. */
    private int timeoutSegundos = 120;
}
