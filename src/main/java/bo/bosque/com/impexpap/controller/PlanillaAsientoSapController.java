package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.commons.AccesoPantalla;
import bo.bosque.com.impexpap.commons.AsientoPlanillaSapService;
import bo.bosque.com.impexpap.commons.AsientoSapConfigService;
import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.dto.AsientoSapEmpresaDto;
import bo.bosque.com.impexpap.dto.AsientoSapPeriodo;
import bo.bosque.com.impexpap.dto.AsientoSapResumen;
import bo.bosque.com.impexpap.model.AsientoSapConfig;
import bo.bosque.com.impexpap.security.jwt.DatosToken;
import bo.bosque.com.impexpap.utils.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Asiento contable de la planilla de sueldos en SAP B1, por empresa.
 *
 * <p>La app manda solo el periodo y la empresa. Lo de cada empresa (SP, columnas, base de SAP, usuario
 * y contraseña cifrada del Service Layer) está en {@code tsap_AsientoPlanilla} y se edita con los
 * endpoints {@code /config/*}, que exigen la pantalla de configuración.
 */
@RestController
@RequestMapping("/planilla/asiento-sap")
@CrossOrigin(origins = "*", methods = {RequestMethod.POST})
public class PlanillaAsientoSapController {

    private final AccesoPantalla pantallas;
    private final AsientoPlanillaSapService asientos;
    private final AsientoSapConfigService configuracion;

    public PlanillaAsientoSapController(AccesoPantalla pantallas, AsientoPlanillaSapService asientos,
                                        AsientoSapConfigService configuracion) {
        this.pantallas = pantallas;
        this.asientos = asientos;
        this.configuracion = configuracion;
    }

    // ── El asiento ─────────────────────────────────────────────────────────────────────

    /** Las empresas que se pueden enviar: habilitadas y con la contraseña cargada. */
    @PostMapping("/empresas")
    public ResponseEntity<ApiResponse<?>> empresas(Authentication auth) {
        pantallas.exigirAlguna(auth, asientos.pantallas());
        return ResponseEntity.ok(new ApiResponse<>("Empresas con asiento en SAP.",
                configuracion.empresasHabilitadas(), HttpStatus.OK.value()));
    }

    /** El asiento armado, para revisarlo antes de enviar. No toca SAP. */
    @PostMapping("/vista-previa")
    public ResponseEntity<ApiResponse<?>> vistaPrevia(@RequestBody AsientoSapPeriodo periodo, Authentication auth) {
        pantallas.exigirAlguna(auth, asientos.pantallas());
        AsientoSapResumen asiento = asientos.vistaPrevia(periodo);
        String mensaje = asiento.isCuadra() ? "El asiento cuadra." : "El asiento no cuadra.";
        return ResponseEntity.ok(new ApiResponse<>(mensaje, asiento, HttpStatus.OK.value()));
    }

    /** Crea el asiento en SAP. {@code data} es el {@code JdtNum}; 409 si SAP ya tiene uno con esa referencia. */
    @PostMapping("/enviar")
    public ResponseEntity<ApiResponse<?>> enviar(@RequestBody AsientoSapPeriodo periodo, Authentication auth) {
        pantallas.exigirAlguna(auth, asientos.pantallas());
        int jdtNum = asientos.enviar(periodo, DatosToken.codUsuarioDe(auth));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("Asiento N° " + jdtNum + " creado en SAP.", jdtNum, HttpStatus.CREATED.value()));
    }

    // ── La configuración por empresa ───────────────────────────────────────────────────

    /** Todas las empresas configuradas; de la contraseña solo se dice si está cargada. */
    @PostMapping("/config/listar")
    public ResponseEntity<ApiResponse<?>> listarConfig(Authentication auth) {
        pantallas.exigirAlguna(auth, asientos.pantallasConfig());
        return ResponseEntity.ok(new ApiResponse<>("Configuración del asiento en SAP.",
                configuracion.paraPantalla(), HttpStatus.OK.value()));
    }

    /** Alta o cambio de una empresa. No toca la contraseña, que tiene su propio endpoint. */
    @PostMapping("/config/guardar")
    public ResponseEntity<ApiResponse<?>> guardarConfig(@RequestBody AsientoSapConfig c, Authentication auth) {
        pantallas.exigirAlguna(auth, asientos.pantallasConfig());
        configuracion.guardar(c, DatosToken.codUsuarioDe(auth));
        return respuesta("Guardado. Rige desde el próximo asiento.");
    }

    /** La contraseña del Service Layer; se guarda cifrada y nunca vuelve a la app. */
    @PostMapping("/config/clave")
    public ResponseEntity<ApiResponse<?>> guardarClave(@RequestBody AsientoSapEmpresaDto dto, Authentication auth) {
        pantallas.exigirAlguna(auth, asientos.pantallasConfig());
        configuracion.guardarClave(empresa(dto), dto.getClave(), DatosToken.codUsuarioDe(auth));
        return respuesta("Contraseña guardada.");
    }

    @PostMapping("/config/eliminar")
    public ResponseEntity<ApiResponse<?>> eliminarConfig(@RequestBody AsientoSapEmpresaDto dto, Authentication auth) {
        pantallas.exigirAlguna(auth, asientos.pantallasConfig());
        configuracion.eliminar(empresa(dto), DatosToken.codUsuarioDe(auth));
        return respuesta("Configuración eliminada.");
    }

    /** Inicia y cierra sesión en SAP con lo guardado, para comprobar usuario, contraseña y certificado. */
    @PostMapping("/config/probar")
    public ResponseEntity<ApiResponse<?>> probar(@RequestBody AsientoSapEmpresaDto dto, Authentication auth) {
        pantallas.exigirAlguna(auth, asientos.pantallasConfig());
        return respuesta(asientos.probar(empresa(dto)));
    }

    private static int empresa(AsientoSapEmpresaDto dto) {
        if (dto == null || dto.getCodEmpresa() == null) throw new SpBusinessException("Elige la empresa.");
        return dto.getCodEmpresa();
    }

    private static ResponseEntity<ApiResponse<?>> respuesta(String mensaje) {
        return ResponseEntity.ok(new ApiResponse<>(mensaje, null, HttpStatus.OK.value()));
    }
}
