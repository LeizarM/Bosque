package bo.bosque.com.impexpap.commons;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Implementación por defecto de {@link ContactoClienteResolver}: SIEMPRE devuelve
 * {@code null}. No es un "TODO" olvidado — es la respuesta correcta con los datos que
 * hay hoy en la base.
 *
 * <h3>Por qué devuelve null: no existe el teléfono del cliente en BOSQUE-2_0</h3>
 * Se verificó contra el servidor real, no de memoria:
 * <ul>
 *   <li>{@code trch_Entregas.cardCode} (el código de cliente SAP de cada entrega) cruza
 *       1170 de 1170 con {@code tped_clienteSAP.codClienteSAP}. El cruce está perfecto.</li>
 *   <li>Pero {@code tped_clienteSAP} tiene {@code correo} y NO tiene ninguna columna de
 *       teléfono, celular ni WhatsApp.</li>
 *   <li>No hay otra tabla en {@code BOSQUE-2_0} que guarde el contacto telefónico del
 *       cliente. Los teléfonos que sí existen en la base son de EMPLEADOS
 *       (para cumpleaños / rol), no de clientes.</li>
 * </ul>
 *
 * <h3>Dónde viven realmente los teléfonos: SAP Business One</h3>
 * La fuente de verdad del contacto de un cliente es el maestro de socios de negocio de
 * SAP B1: la tabla <b>{@code OCRD}</b>, columnas <b>{@code OCRD.Phone1}</b> (teléfono
 * principal) y <b>{@code OCRD.Cellular}</b> (celular, que es el que sirve para WhatsApp);
 * el join es {@code OCRD.CardCode = trch_Entregas.cardCode}. Si el cliente tiene varias
 * personas de contacto, el detalle está en {@code OCPR} ({@code OCPR.Cellolar} —sí, con
 * ese nombre— y {@code OCPR.Tel1}), filtrando por {@code OCPR.CardCode}.
 * Ojo con la calidad del dato: en SAP esos campos son texto libre y vienen con guiones,
 * espacios, prefijos {@code +591}, internos tipo "int. 23" y a veces dos números en la
 * misma celda. Por eso la normalización a {@code 591XXXXXXXX@c.us} es obligatoria y ya
 * está resuelta en {@code WhatsAppService.normalizarChatId(String)}.
 *
 * <h3>Cómo activar el aviso al cliente (receta completa)</h3>
 * <ol>
 *   <li>Conseguir el dato: o bien una vista/tabla en {@code BOSQUE-2_0} alimentada desde
 *       SAP (por ejemplo agregarle la columna telefónica a {@code tped_clienteSAP} en su
 *       proceso de sincronización), o bien un SP nuevo que consulte {@code OCRD} en la
 *       base de SAP.</li>
 *   <li>Crear una clase nueva en este mismo paquete, por ejemplo
 *       {@code ContactoClienteSapResolver}, anotada
 *       {@code @Service @Primary implements ContactoClienteResolver}. El
 *       {@code @Primary} es lo que hace que Spring la inyecte en lugar de este stub;
 *       NO hace falta borrar ni tocar esta clase.</li>
 *   <li>Esa implementación debe atrapar sus propias excepciones y devolver {@code null}
 *       ante cualquier problema, y devolver el chatId ya normalizado con
 *       {@code WhatsAppService.normalizarChatId(...)}.</li>
 *   <li>Poner {@code openwa.entregas.notificar-cliente=true} (o la variable de entorno
 *       {@code OPENWA_NOTIF_CLIENTE=true}). Mientras esa propiedad esté en {@code false},
 *       {@link NotificacionEntregaService} ni siquiera llama al resolver.</li>
 * </ol>
 *
 * <h3>Qué NO hacer</h3>
 * No "arreglar" este stub devolviendo el número de la empresa, el del chofer o un número
 * de prueba: el mensaje que se manda está redactado como un aviso de la empresa AL
 * CLIENTE, y mandarlo al destinatario equivocado es peor que no mandarlo. Devolver
 * {@code null} es el comportamiento seguro.
 */
@Service
public class ContactoClienteResolverStub implements ContactoClienteResolver {

    private static final Logger logger = LoggerFactory.getLogger(ContactoClienteResolverStub.class);

    /**
     * {@inheritDoc}
     *
     * <p>Devuelve siempre {@code null}. Loguea en DEBUG (nivel activo para
     * {@code bo.bosque.com.impexpap} según {@code application.properties}) para que, si
     * alguien enciende el flag sin haber puesto una implementación real, quede rastro de
     * qué clientes se quedaron sin aviso.
     */
    @Override
    public String resolverChatId(String cardCode, String db) {
        logger.debug("sin fuente de telefono para cardCode={} db={}", cardCode, db);
        return null;
    }
}
