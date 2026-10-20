package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.commons.TcpeClaveService;
import bo.bosque.com.impexpap.commons.TcpeLoteService;
import bo.bosque.com.impexpap.commons.TcpePermisos;
import bo.bosque.com.impexpap.dao.ITcpeAcceso;
import bo.bosque.com.impexpap.dao.ITcpeUsuario;
import bo.bosque.com.impexpap.dto.TcpeAccesoDto;
import bo.bosque.com.impexpap.dto.TcpeClaveGeneradaDto;
import bo.bosque.com.impexpap.dto.TcpeUsuarioClaveDto;
import bo.bosque.com.impexpap.model.TcpeUsuario;
import bo.bosque.com.impexpap.security.jwt.DatosToken;
import bo.bosque.com.impexpap.utils.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Claves de los usuarios externos del módulo de exámenes. Activar y desactivar se hace con el catálogo
 * ({@code /examenes/catalogo/usuario/guardar}); aquí solo lo que toca {@code password2}, que siempre pasa por
 * {@code p_abm_tcpe_Acceso 'P'} y siempre como hash.
 */
@RestController
@CrossOrigin(originPatterns = "${tcpe.cors.origenes:*}", methods = {RequestMethod.POST})
@RequestMapping("/examenes/usuarios")
@PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
public class TcpeUsuariosController {

    private static final Logger log = LoggerFactory.getLogger(TcpeUsuariosController.class);

    private final ITcpeUsuario usuarios;
    private final ITcpeAcceso acceso;
    private final TcpeClaveService claves;
    private final TcpePermisos permisos;

    public TcpeUsuariosController(ITcpeUsuario usuarios, ITcpeAcceso acceso, TcpeClaveService claves,
                                  TcpePermisos permisos) {
        this.usuarios = usuarios;
        this.acceso = acceso;
        this.claves = claves;
        this.permisos = permisos;
    }

    /**
     * Genera una clave nueva al azar, guarda su hash y la devuelve en claro <b>una sola vez</b>. A quién se le puede
     * cambiar (que exista, que sea externo) lo decide {@code p_abm_tcpe_Acceso 'P'}.
     */
    @PostMapping("/regenerar-clave")
    public ResponseEntity<ApiResponse<?>> regenerarClave(@Valid @RequestBody TcpeUsuarioClaveDto dto,
                                                         Authentication auth) {
        permisos.exigirAlguna(auth, TcpePermisos.USUARIOS);
        long aud = DatosToken.codUsuarioDe(auth);
        long codUsuario = dto.getCodUsuario();
        String clave = claves.generar();
        acceso.guardarHash(codUsuario, claves.hash(clave), aud);
        log.info("Clave de examen regenerada para tcpe_usuario {} por el usuario {}", codUsuario, aud);
        TcpeUsuario u = usuarios.obtener(codUsuario);
        if (u == null) return TcpeRespuestas.ok(new TcpeClaveGeneradaDto(codUsuario, null, clave, null, null));
        String nombre = TcpeLoteService.nombreCompleto(u.getApellidoPaterno(), u.getApellidoMaterno(), u.getNombres());
        return TcpeRespuestas.ok(new TcpeClaveGeneradaDto(codUsuario, u.getLogin(), clave, u.getCi(), nombre));
    }

    /**
     * Migración masiva: pasa a BCrypt toda {@code password2} que todavía está en texto plano (las del legacy).
     * Los hash se calculan en paralelo; después se guardan uno por uno. Solo administradores.
     */
    @PostMapping("/migrar-claves")
    @PreAuthorize("hasRole('ROLE_ADM')")
    public ResponseEntity<ApiResponse<?>> migrarClaves(Authentication auth) {
        long aud = DatosToken.codUsuarioDe(auth);
        List<TcpeAccesoDto> pendientes = new ArrayList<>();
        for (TcpeAccesoDto a : acceso.sinHash()) {
            if (a.getCodUsuario() != null && a.getPassword2() != null && !a.getPassword2().isEmpty()
                    && !TcpeClaveService.esHash(a.getPassword2())) {
                pendientes.add(a);
            }
        }
        List<String> planos = new ArrayList<>(pendientes.size());
        for (TcpeAccesoDto a : pendientes) planos.add(a.getPassword2());
        List<String> hashes = claves.hashEnParalelo(planos);

        int migrados = 0;
        for (int i = 0; i < pendientes.size(); i++) {
            acceso.guardarHash(pendientes.get(i).getCodUsuario(), hashes.get(i), aud);
            migrados++;
        }
        log.info("Migración de claves de exámenes: {} usuarios pasados a BCrypt por el usuario {}", migrados, aud);
        return TcpeRespuestas.ok(Collections.singletonMap("migrados", migrados));
    }
}
