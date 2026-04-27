package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.dao.IEntregaChofer;
import bo.bosque.com.impexpap.dto.PedidoPendienteEntregaDTO;
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

    public EntregaChoferController(IEntregaChofer entregaChoferDao) {
        this.entregaChoferDao = entregaChoferDao;
    }

    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/chofer-entrega")
    public List<EntregaChofer> obtenerEntregasXEmpleado(@RequestBody EntregaChofer mb) {
        List<EntregaChofer> lstTemp = this.entregaChoferDao.listarEntregasXEmpleado(mb.getUChofer());
        return lstTemp.isEmpty() ? Collections.emptyList() : lstTemp;
    }

    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/registro-entrega-chofer")
    public ResponseEntity<Map<String, Object>> registroEntregaChofer(@RequestBody EntregaChofer mb) {
        Map<String, Object> response = new HashMap<>();
        try {
            mb.setFechaEntrega(UTILES.convertirAFormatoSQLServer(mb.getFechaEntrega()));
            if (!this.entregaChoferDao.registrarEntregaChofer(mb, "B")) {
                return buildErrorResponse(response, "Error al Registrar Las Entregas");
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
