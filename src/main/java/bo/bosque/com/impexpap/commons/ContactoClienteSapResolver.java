package bo.bosque.com.impexpap.commons;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Resuelve el WhatsApp del cliente contra el maestro de socios de negocio de SAP B1
 * ({@code OCRD}), al que se llega por el linked server configurado.
 *
 * <p>Reemplaza a {@link ContactoClienteResolverStub} vía {@code @Primary}. El stub queda
 * en el proyecto a propósito: documenta por qué esto no existía antes y sirve de vuelta
 * atrás si hay que desconectar SAP sin recompilar (basta quitarle el {@code @Primary} a
 * esta clase).
 *
 * <h3>De dónde sale el dato (verificado contra el servidor real)</h3>
 * Los teléfonos de clientes NO están en {@code BOSQUE-2_0}: se buscaron columnas
 * {@code %telefon%}, {@code %celul%}, {@code %phone%} en las 494 tablas y las únicas que
 * hay son de empleados, de Tigo y de vendedores. Viven en las bases de SAP del servidor
 * remoto, en {@code OCRD.Cellular} y {@code OCRD.Phone1}.
 *
 * <p>Cobertura medida sobre los clientes que realmente reciben entregas (1893 distintos):
 * <b>100 % existen en {@code OCRD}</b>, 1381 tienen {@code Cellular} y de esos
 * <b>1301 (94 %) ya vienen en un formato que {@code normalizarChatId} acepta</b>.
 *
 * <h3>Las tres trampas de este join</h3>
 * <ol>
 *   <li><b>Intercalación.</b> {@code BOSQUE-2_0} es {@code Modern_Spanish_CI_AS} y las
 *       bases de SAP son {@code SQL_Latin1_General_CP850_CI_AS}. Comparar {@code CardCode}
 *       sin {@code COLLATE DATABASE_DEFAULT} tira
 *       <i>"No se puede resolver el conflicto de intercalación"</i> y el aviso muere. El
 *       {@code COLLATE} va del lado de la COLUMNA; el parámetro se coerce solo.</li>
 *   <li><b>Nombres de base con espacio.</b> {@code PRODUCTIVA PAPEL} obliga a los
 *       corchetes en el nombre de cuatro partes.</li>
 *   <li><b>La columna {@code db} llega del cliente HTTP.</b> Por eso NUNCA se interpola en
 *       el SQL: se traduce por el mapa blanco de {@code openwa.entregas.sap-bases} y, si no
 *       está, no se consulta nada. Lo único que se concatena son valores de configuración,
 *       validados además contra un patrón conservador al arrancar.</li>
 * </ol>
 */
@Service
@Primary
public class ContactoClienteSapResolver implements ContactoClienteResolver {

    private static final Logger logger = LoggerFactory.getLogger(ContactoClienteSapResolver.class);

    /** Solo letras, dígitos, espacio, guion bajo y guion: lo que puede llamarse una base o un linked server. */
    private static final String PATRON_IDENTIFICADOR = "[A-Za-z0-9_ -]{1,128}";

    /** Piso del timeout si la propiedad viene en 0 o negativa. */
    private static final int TIMEOUT_POR_DEFECTO = 8;

    private final JdbcTemplate jdbcTemplate;

    @Value("${openwa.entregas.sap-linked-server:SRV_2022}")
    private String linkedServer;

    /** Formato: {@code ESP=ESPPAPEL,IPX=IMPEXPAP,...}. Clave = columna {@code db} de {@code trch_Entregas}. */
    @Value("${openwa.entregas.sap-bases:}")
    private String basesConfig;

    @Value("${openwa.entregas.sap-timeout-segundos:8}")
    private int timeoutSegundos;

    /**
     * Si el celular no sirve, ¿probar con {@code Phone1}? Apagado a propósito: ver el
     * comentario en {@link #resolverChatId(String, String)}.
     */
    @Value("${openwa.entregas.usar-phone1:false}")
    private boolean usarPhone1;

    /** Mapa blanco ya validado. Inmutable después del arranque. */
    private Map<String, String> basesPorEmpresa = Collections.emptyMap();

    /** Consultas ya armadas, una por empresa. Se arman una sola vez y no dependen del request. */
    private Map<String, String> consultasPorEmpresa = Collections.emptyMap();

    @Autowired
    public ContactoClienteSapResolver(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Valida la configuración y precompila una consulta por empresa.
     *
     * <p>Se hace acá y no en cada llamada para que un error de configuración se vea al
     * arrancar —en el log, una sola vez— y no como avisos que fallan de a uno en silencio.
     * Una entrada mal formada se descarta con WARN en vez de tumbar el arranque: perder el
     * aviso de una empresa es molesto, no levantar el backend es un incidente.
     */
    @PostConstruct
    void inicializar() {
        if (!esIdentificadorValido(linkedServer)) {
            logger.error("openwa.entregas.sap-linked-server inválido ('{}'); el aviso al cliente queda desactivado.",
                         linkedServer);
            return;
        }

        Map<String, String> bases = new LinkedHashMap<String, String>();
        Map<String, String> consultas = new HashMap<String, String>();

        if (basesConfig != null && !basesConfig.trim().isEmpty()) {
            String[] entradas = basesConfig.split(",");
            for (int i = 0; i < entradas.length; i++) {
                String entrada = entradas[i].trim();
                if (entrada.isEmpty()) {
                    continue;
                }
                int igual = entrada.indexOf('=');
                if (igual <= 0 || igual == entrada.length() - 1) {
                    logger.warn("Entrada mal formada en openwa.entregas.sap-bases, se ignora: '{}' "
                              + "(se esperaba EMPRESA=BASE_SAP)", entrada);
                    continue;
                }
                String empresa = entrada.substring(0, igual).trim().toUpperCase(java.util.Locale.ROOT);
                String baseSap = entrada.substring(igual + 1).trim();
                if (!esIdentificadorValido(baseSap)) {
                    logger.warn("Nombre de base SAP inválido para la empresa '{}', se ignora: '{}'", empresa, baseSap);
                    continue;
                }
                bases.put(empresa, baseSap);
                consultas.put(empresa, armarConsulta(baseSap));
            }
        }

        this.basesPorEmpresa = Collections.unmodifiableMap(bases);
        this.consultasPorEmpresa = Collections.unmodifiableMap(consultas);

        if (bases.isEmpty()) {
            logger.warn("openwa.entregas.sap-bases está vacío: no se va a resolver el teléfono de ningún cliente. "
                      + "Formato esperado: ESP=ESPPAPEL,IPX=IMPEXPAP,...");
        } else {
            logger.info("Contacto de cliente vía SAP listo. Linked server '{}', empresas mapeadas: {}",
                        linkedServer, bases.keySet());
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Usa {@code Cellular}. El fallback a {@code Phone1} existe pero viene APAGADO
     * ({@code openwa.entregas.usar-phone1=false}), y conviene dejarlo así: parecía seguro
     * porque {@code normalizarChatId} exige que el número empiece en 6 o 7, pero al medirlo
     * contra los datos reales resultó que de los 319 clientes que se resolvían por
     * {@code Phone1}, <b>39 caían en un número que en {@code OCRD} figura como
     * {@code Cellular} de OTRO cliente</b>. Sube la cobertura de ~72 % a ~96 % a cambio de
     * un 12 % de destinatarios equivocados, y esos dos errores no cuestan lo mismo: no
     * avisar es una molestia, avisarle al que no es expone datos de un cliente a otro.
     *
     * <p>Nunca lanza: ante cualquier problema devuelve {@code null} y el aviso simplemente
     * no se manda. La entrega ya está registrada mucho antes de llegar acá.
     */
    @Override
    public String resolverChatId(String cardCode, String db) {
        try {
            if (cardCode == null || cardCode.trim().isEmpty()) {
                return null;
            }
            if (db == null || db.trim().isEmpty()) {
                return null;
            }

            String empresa = db.trim().toUpperCase(java.util.Locale.ROOT);
            String consulta = consultasPorEmpresa.get(empresa);
            if (consulta == null) {
                // Caso normal para db='ALL', que son los marcadores de inicio/fin de ruta.
                logger.debug("La empresa '{}' no está mapeada a ninguna base SAP; sin aviso para cardCode={}.",
                             empresa, cardCode);
                return null;
            }

            List<Map<String, Object>> filas = consultar(consulta, cardCode.trim());
            if (filas.isEmpty()) {
                logger.debug("cardCode={} no existe en OCRD de {}.", cardCode, basesPorEmpresa.get(empresa));
                return null;
            }

            Map<String, Object> fila = filas.get(0);
            String chatId = WhatsAppService.normalizarChatId(texto(fila.get("cel")));
            if (chatId == null && usarPhone1) {
                // Fallback APAGADO por defecto, y la razón está medida: de los 319 clientes
                // que se resuelven por Phone1, 39 terminan en un número que en OCRD figura
                // como Cellular de OTRO cliente. Sube la cobertura de ~72 % a ~96 %, pero a
                // costa de un 12 % de destinatarios equivocados — y el costo de acertarle a
                // la persona equivocada no es simétrico con el de no avisar.
                chatId = WhatsAppService.normalizarChatId(texto(fila.get("tel")));
                if (chatId != null) {
                    logger.debug("cardCode={} resuelto por Phone1 (fallback habilitado).", cardCode);
                }
            }
            if (chatId == null) {
                logger.info("El cliente {} ({}) no tiene un número de WhatsApp utilizable en SAP; no se avisa.",
                            cardCode, empresa);
            }
            return chatId;

        } catch (Exception e) {
            logger.warn("No se pudo resolver el contacto del cliente cardCode={} db={}: {}",
                        cardCode, db, e.getMessage());
            return null;
        } catch (Throwable t) {
            logger.error("Fallo grave resolviendo el contacto del cliente cardCode={} db={}: {}",
                         cardCode, db, t.getMessage(), t);
            return null;
        }
    }

    /**
     * Ejecuta con timeout propio. El linked server es una dependencia de red hacia otro
     * servidor: si SAP no contesta, esto no puede quedarse colgado ocupando uno de los dos
     * hilos del pool de notificaciones.
     */
    private List<Map<String, Object>> consultar(String consulta, String cardCode) {
        JdbcTemplate acotado = new JdbcTemplate(jdbcTemplate.getDataSource());
        // Un 0 significa SIN LÍMITE por especificación JDBC, y un negativo hace que Spring ni
        // siquiera llame a setQueryTimeout. Los dos valores son escrituras naturales de
        // "desactivado" en un .properties y los dos dejarían este hilo colgado esperando a SAP,
        // que es justo lo que el timeout viene a evitar. Se fuerza un piso.
        acotado.setQueryTimeout(timeoutSegundos > 0 ? timeoutSegundos : TIMEOUT_POR_DEFECTO);
        // queryForList NO lanza EmptyResultDataAccessException: devuelve lista vacía. (Esa la
        // tira queryForObject/queryForMap.) Por eso acá no hay catch de ese tipo.
        return acotado.queryForList(consulta, new Object[] { cardCode });
    }

    /**
     * Arma la consulta de una empresa. {@code baseSap} ya pasó por el patrón blanco y sale
     * de configuración, nunca del request.
     */
    private String armarConsulta(String baseSap) {
        // CardType = 'C' (Cliente). OCRD mezcla clientes con proveedores ('S', 2.975 en las
        // cuatro bases) y leads ('L') bajo la MISMA clave CardCode. Sin este filtro, un
        // cardCode que exista como proveedor devuelve el telefono del proveedor y el aviso
        // "su pedido ya fue entregado" le llega a quien nos vende.
        return "SELECT TOP 1 LTRIM(RTRIM(ISNULL(o.Cellular,''))) AS cel,"
             + " LTRIM(RTRIM(ISNULL(o.Phone1,''))) AS tel"
             + " FROM [" + linkedServer.trim() + "].[" + baseSap + "].dbo.OCRD o"
             + " WHERE o.CardCode COLLATE DATABASE_DEFAULT = ?"
             + " AND o.CardType = 'C'";
    }

    private static boolean esIdentificadorValido(String valor) {
        return valor != null && valor.trim().matches(PATRON_IDENTIFICADOR) && valor.indexOf(']') < 0;
    }

    private static String texto(Object valor) {
        return valor == null ? null : valor.toString();
    }
}
