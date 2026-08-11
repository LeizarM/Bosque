package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.commons.NotificacionEntregaService;
import bo.bosque.com.impexpap.commons.SincronizacionEntregasService;
import bo.bosque.com.impexpap.dao.ICalificacionEntrega;
import bo.bosque.com.impexpap.dao.IEntregaChofer;
import bo.bosque.com.impexpap.dto.PedidoPendienteEntregaDTO;
import bo.bosque.com.impexpap.dto.ResumenCalificacionDTO;
import bo.bosque.com.impexpap.model.CalificacionEntrega;
import bo.bosque.com.impexpap.model.EntregaChofer;
import bo.bosque.com.impexpap.utils.Utiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST para gestión de entregas de choferes.
 */
@RestController
@CrossOrigin("*")
@RequestMapping("/entregas")
public class EntregaChoferController {

    private static final Logger LOG = LoggerFactory.getLogger(EntregaChoferController.class);
    private static final Utiles UTILES = new Utiles();
    
    private static final String MSG_REGISTRO_OK = "Datos de la entrega actualizados";
    private static final String MSG_REGISTRO_INICIO_OK = "Datos de la entrega de inicio o fin actualizados";
    private static final String MSG_ERROR = "Error en el servidor";
    private static final String STATUS_OK = "ok";
    private static final String STATUS_ERROR = "error";

    private final IEntregaChofer entregaChoferDao;
    private final NotificacionEntregaService notificacionEntregaService;
    private final ICalificacionEntrega calificacionEntregaDao;
    private final SincronizacionEntregasService sincronizacionEntregasService;

    /**
     * Si la ACCIÓN 'Y' ya demostró que funciona en esta base. Ver
     * {@code SincronizacionEntregasService.soportaLecturaSinSync()}.
     */

    public EntregaChoferController(IEntregaChofer entregaChoferDao,
                                   NotificacionEntregaService notificacionEntregaService,
                                   ICalificacionEntrega calificacionEntregaDao,
                                   SincronizacionEntregasService sincronizacionEntregasService) {
        this.entregaChoferDao = entregaChoferDao;
        this.notificacionEntregaService = notificacionEntregaService;
        this.calificacionEntregaDao = calificacionEntregaDao;
        this.sincronizacionEntregasService = sincronizacionEntregasService;
    }

    /**
     * La lista de entregas pendientes del chofer. Es la pantalla que la app abre al entrar y la
     * que se refresca a mano cada dos por tres, así que es el endpoint más llamado del módulo.
     *
     * <p><b>El contrato con el Flutter no cambia:</b> mismo verbo, misma ruta, mismo body, misma
     * respuesta. Todo lo de acá abajo es interno.
     *
     * <h3>Por qué se sincroniza ANTES y se lee DESPUÉS</h3>
     * Antes, la ACCIÓN 'A' hacía las dos cosas en una sola llamada: sincronizaba
     * {@code trch_Entregas} entera contra SAP (~1,4 s medidos, 417 filas, 4 empresas) y recién
     * después devolvía los pendientes de este chofer. Ahora son dos pasos, y el orden importa:
     * <ul>
     *   <li><b>Si es este request el que sincroniza</b> (ganó el candado y el intervalo estaba
     *       vencido), {@code asegurarSincronizado()} vuelve con los datos nuevos ya comprometidos
     *       en la tabla. Leer después es lo único que tiene sentido: leer antes lo dejaría pagando
     *       1,4 s para devolver igual la foto vieja, y el beneficio se lo llevaría el chofer
     *       siguiente.</li>
     *   <li><b>Si está sincronizando otro hilo</b>, {@code asegurarSincronizado()} vuelve en el
     *       acto sin esperarlo y acá se lee lo que haya en la tabla — como mucho, de un intervalo
     *       atrás. Esperar al otro hilo sería reconstruir el problema original: quince choferes en
     *       fila detrás de una consulta a SAP, ahora en serie en vez de en paralelo.</li>
     * </ul>
     * Las dos ramas son correctas y ninguna bloquea. La única diferencia entre ellas es la
     * antigüedad de los datos, acotada por {@code entregas.sync.intervalo-segundos}.
     *
     * <h3>Las dos salidas de emergencia</h3>
     * El camino histórico (ACCIÓN 'A') sigue existiendo y se usa si {@code entregas.sync.habilitado}
     * está en {@code false}, si la ACCIÓN 'Y' falla, o si al arrancar se detectó que el script SQL
     * no está corrido en esta base (ver {@code SincronizacionEntregasService.soportaLecturaSinSync}). Es a propósito: las acciones nuevas viven en
     * un script SQL que puede no estar corrido todavía en esta base, y un chofer nunca puede
     * quedarse sin su lista por un problema de orden de despliegue.
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/chofer-entrega")
    public List<EntregaChofer> obtenerEntregasXEmpleado(@RequestBody EntregaChofer mb) {

        if (!this.sincronizacionEntregasService.soportaLecturaSinSync()) {
            // Vuelta atrás completa por configuración: ACCIÓN 'A', que sincroniza y lista en una
            // sola llamada, exactamente como antes de este cambio.
            return listarPorCaminoHistorico(mb.getUChofer());
        }

        // 1) Como mucho una sincronización cada N segundos, un solo hilo a la vez, nadie espera a
        //    nadie y nunca lanza. Si SAP está caído, esto vuelve y no pasa nada más.
        this.sincronizacionEntregasService.asegurarSincronizado();

        // 2) Solo el SELECT del chofer. Sin linked server, sin SAP, sin las otras tres empresas.
        List<EntregaChofer> lstTemp = this.entregaChoferDao.listarEntregasXEmpleadoSinSync(mb.getUChofer());

        if (lstTemp == null) {
            // null = la consulta falló (típicamente, la ACCIÓN 'Y' no está desplegada). No es lo
            // mismo que "no tiene entregas" y no se puede tratar igual.
            LOG.error("La ACCIÓN 'Y' falló para el chofer {}; se responde con el camino histórico "
                    + "(ACCIÓN 'A'). Revisar que el script de las acciones nuevas esté corrido en esta base.",
                      mb.getUChofer());
            return listarPorCaminoHistorico(mb.getUChofer());
        }

        // Vacia significa vacia. Ya no hay que deducir nada de eso: si la ACCION 'Y' no
        // existiera, soportaLecturaSinSync() lo habria detectado al arrancar y este codigo ni
        // se ejecutaria.
        return lstTemp;
    }

    /**
     * El camino de siempre: {@code p_list_trch_Entregas} ACCIÓN 'A', que sincroniza con SAP dentro
     * del request y después lista. Es lento (~1,4 s solo de sincronización) y por eso se usa nada
     * más que como red de seguridad, pero tiene una virtud que el otro no: existe desde siempre en
     * todas las bases.
     */
    private List<EntregaChofer> listarPorCaminoHistorico(int codEmpleado) {
        List<EntregaChofer> lstTemp = this.entregaChoferDao.listarEntregasXEmpleado(codEmpleado);
        return (lstTemp == null || lstTemp.isEmpty()) ? Collections.emptyList() : lstTemp;
    }

    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/registro-entrega-chofer")
    public ResponseEntity<Map<String, Object>> registroEntregaChofer(@RequestBody EntregaChofer mb) {
        Map<String, Object> response = new HashMap<>();
        try {
            mb.setFechaEntrega(UTILES.convertirAFormatoSQLServer(mb.getFechaEntrega()));

            // Se mira el CONTEO de filas, no un booleano, porque 0 y -1 significan cosas
            // opuestas y el booleano las colapsaba en la misma respuesta de error.
            int filas = this.entregaChoferDao.registrarEntregaChoferFilas(mb, "B");

            if (filas < 0) {
                // El SP explotó. Esto sí es un error de verdad.
                return buildErrorResponse(response, "Error al Registrar Las Entregas");
            }

            if (filas == 0) {
                // Cero filas = la entrega YA estaba marcada. Es el caso normal cuando el chofer
                // toca "Marcar" dos veces, o cuando reintenta tras una respuesta perdida: el
                // primer POST llegó, la respuesta se perdió, y el segundo encuentra el trabajo
                // hecho. Responder error acá le pintaría una alarma roja sobre una entrega
                // perfectamente registrada, y lo empujaría a reintentar de nuevo.
                //
                // Se responde ÉXITO —la operación es idempotente y el estado final es el que el
                // chofer quería— pero se deja un WARN, porque un pico de estos también puede
                // indicar que el docEntry que manda la app no existe en la base.
                LOG.warn("La entrega docEntry={} db={} docNum={} no afectó filas: ya estaba marcada "
                       + "como entregada, o el documento no existe. Se responde éxito (idempotente).",
                         mb.getDocEntry(), mb.getDb(), mb.getDocNum());
            }
            // Aviso de WhatsApp: solo con la entrega YA registrada, y con su propio try/catch.
            // notificarEntregaCompletada es @Async, pero eso solo mueve el CUERPO del método a
            // otro hilo: el encolado sigue corriendo acá, en el hilo del request del chofer, y
            // puede tirar (TaskRejectedException si el executor está saturado porque openWA no
            // responde, o cualquier fallo del proxy). Sin este catch esa excepción caería en el
            // catch(Exception) de abajo y devolvería 500 por una entrega que ya quedó grabada:
            // el chofer la reintentaría y el aviso se duplicaría. El envío es cortesía, la
            // entrega es el negocio.
            try {
                this.notificacionEntregaService.notificarEntregaCompletada(mb.getDocEntry(), mb.getDb());
            } catch (Throwable t) {
                LOG.error("Entrega docEntry={} db={} registrada OK, pero no se pudo encolar el aviso de WhatsApp: {}",
                          mb.getDocEntry(), mb.getDb(), t.getMessage(), t);
            }
            return buildSuccessResponse(response, MSG_REGISTRO_OK);
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(response, "Formato de fecha inválido: " + e.getMessage());
        } catch (Exception e) {
            LOG.error("Error en registroEntregaChofer: {}", e.getMessage(), e);
            return buildErrorResponse(response, MSG_ERROR);
        }
    }

    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/entregas-fecha")
    public List<EntregaChofer> obtenerEntregasChoferesXFecha(@RequestBody EntregaChofer mb) {
        List<EntregaChofer> lstTemp = this.entregaChoferDao.listarEntregasXChofer(mb.getFechaEntrega(), mb.getCodEmpleado());
        return lstTemp.isEmpty() ? Collections.emptyList() : lstTemp;
    }

    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/registro-inicio-fin-entrega")
    public ResponseEntity<Map<String, Object>> regitroInicioFinEntrega(@RequestBody EntregaChofer mb) {
        Map<String, Object> response = new HashMap<>();
        try {
            mb.setUChofer(mb.getCodEmpleado());
            if (!this.entregaChoferDao.registrarEntregaChofer(mb, "I")) {
                return buildErrorResponse(response, "Error al Registrar El Inicio o Fin de las entregas");
            }
            return buildSuccessResponse(response, MSG_REGISTRO_INICIO_OK);
        } catch (Exception e) {
            LOG.error("Error en regitroInicioFinEntrega: {}", e.getMessage(), e);
            return buildErrorResponse(response, MSG_ERROR);
        }
    }

    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/choferes")
    public List<EntregaChofer> lstChoferes() {
        List<EntregaChofer> lstTemp = this.entregaChoferDao.lstChoferes();
        return lstTemp.isEmpty() ? Collections.emptyList() : lstTemp;
    }

    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/extracto")
    public List<EntregaChofer> lstChoferExtracto(@RequestBody EntregaChofer mb) {
        List<EntregaChofer> lstTemp = this.entregaChoferDao.lstChoferesExtracto(
            mb.getFechaInicio(), mb.getFechaFin(), mb.getCodSucursal(), mb.getCodEmpleado());
        return lstTemp.isEmpty() ? Collections.emptyList() : lstTemp;
    }

    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/pendientes-entrega")
    public List<PedidoPendienteEntregaDTO> pendientesEntrega() {
        List<PedidoPendienteEntregaDTO> lstTemp = this.entregaChoferDao.lstPedidosPendientesEntrega();
        return lstTemp.isEmpty() ? Collections.emptyList() : lstTemp;
    }

    // ==================== CALIFICACIÓN DE ENTREGAS ====================
    // Reporte de lo que responden los clientes al aviso de entrega. Son endpoints de
    // consulta con el JWT de siempre; nada que ver con /whatsapp/webhook, que es el que
    // recibe las respuestas y es público.

    /**
     * Calificaciones del período, una fila por entrega calificada (o a la espera de respuesta).
     *
     * <p>Body: {@code fechaInicio}, {@code fechaFin} y {@code codEmpleado}. {@code codEmpleado = 0}
     * trae las de todos los choferes — el SP trata el 0 como "sin filtro", igual que el resto de
     * los reportes del módulo.
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/calificaciones")
    public List<CalificacionEntrega> lstCalificaciones(@RequestBody EntregaChofer mb) {
        List<CalificacionEntrega> lstTemp = this.calificacionEntregaDao.listarCalificaciones(
            mb.getFechaInicio(), mb.getFechaFin(), mb.getCodEmpleado());
        // El contrato del DAO es "nunca lanza, devuelve lista vacía", pero el null igual se
        // contempla: un reporte que devuelve [] es un reporte vacío, uno que tira NPE es un 500.
        return (lstTemp == null || lstTemp.isEmpty()) ? Collections.emptyList() : lstTemp;
    }

    /**
     * Resumen del período agrupado por chofer: cuántas encuestas se enviaron, cuántas
     * respondieron, el promedio y el conteo de cada puntaje del 1 al 5.
     *
     * <p>Body: {@code fechaInicio} y {@code fechaFin}. No lleva chofer: el sentido del reporte es
     * justamente comparar a todos entre sí.
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/calificaciones-resumen")
    public List<ResumenCalificacionDTO> lstCalificacionesResumen(@RequestBody EntregaChofer mb) {
        List<ResumenCalificacionDTO> lstTemp = this.calificacionEntregaDao.resumenPorChofer(
            mb.getFechaInicio(), mb.getFechaFin());
        return (lstTemp == null || lstTemp.isEmpty()) ? Collections.emptyList() : lstTemp;
    }

    // ==================== MÉTODOS AUXILIARES ====================
    
    private ResponseEntity<Map<String, Object>> buildSuccessResponse(Map<String, Object> response, String msg) {
        response.put("msg", msg);
        response.put("ok", STATUS_OK);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(Map<String, Object> response, String msg) {
        response.put("msg", msg);
        response.put("ok", STATUS_ERROR);
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
