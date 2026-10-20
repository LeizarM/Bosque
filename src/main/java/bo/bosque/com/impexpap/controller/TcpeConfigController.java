package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.dao.ITcpeConfig;
import bo.bosque.com.impexpap.utils.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Configuración del módulo ({@code p_list_tcpe_Config 'L'}, contrato 7.1): límites, valores por defecto y textos que
 * el frontend toma de SQL. La de empleados exige su token; la pública es {@code permitAll} en {@code MainSecurity}
 * porque la pantalla de ingreso del postulante la necesita antes de tener token.
 */
@RestController
@CrossOrigin(originPatterns = "${tcpe.cors.origenes:*}", methods = {RequestMethod.POST})
public class TcpeConfigController {

    private final ITcpeConfig config;

    public TcpeConfigController(ITcpeConfig config) {
        this.config = config;
    }

    @PostMapping("/examenes/config")
    @PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
    public ResponseEntity<ApiResponse<?>> empleados() {
        return TcpeRespuestas.listado(config.listar());
    }

    @PostMapping("/examen-publico/config")
    public ResponseEntity<ApiResponse<?>> publica() {
        return TcpeRespuestas.listado(config.listar());
    }
}
