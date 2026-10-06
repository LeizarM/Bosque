package bo.bosque.com.impexpap.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import bo.bosque.com.impexpap.commons.AccesoModuloHelper;
import bo.bosque.com.impexpap.commons.MensajesCheque;
import bo.bosque.com.impexpap.commons.ReglasCheque;
import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.dao.IChBanco;
import bo.bosque.com.impexpap.dto.BancoDto;
import bo.bosque.com.impexpap.dto.FiltroIdDto;
import bo.bosque.com.impexpap.model.ChBanco;
import bo.bosque.com.impexpap.security.jwt.DatosToken;
import bo.bosque.com.impexpap.utils.ApiResponse;
import bo.bosque.com.impexpap.utils.RespuestaSp;

/**
 * Bancos ({@code tch_banco}), pantalla {@code tchBanco/banco} del legacy (vista 43).
 *
 * <p>Los dos listados ({@code /bancosX}, {@code /bancosPlanilla}) ya existian y los consume el registro de
 * empleados del frontend: devuelven una lista pelada, no {@code ApiResponse}, y esa forma NO cambia.
 * Las escrituras son nuevas y siguen el patron de la casa: permiso por boton, usuario del token,
 * errores de negocio como {@link SpBusinessException}.
 *
 * <p><b>Borrar un banco</b> lo rechaza {@code p_abm_Banco} 'D' con el error 547 si tiene filas en
 * Depositos o en Pagos al Exterior. Lo que el procedimiento no mira es {@code tch_cheque}
 * (no hay FK): borrar un banco con cheques los saca de la grilla del modulo, porque la consulta hace
 * INNER JOIN con {@code tch_banco}. Es como se comporta el legacy hoy y se conserva (decision 2).
 */
@RestController
@CrossOrigin("*")
@RequestMapping("/banco")
public class ChBancoController {

    /** {@code tb_vista.codVista} de "Bancos" ({@code tchBanco/banco}, padre 140). */
    static final int VISTA_BANCOS = 43;
    static final String BTN_NUEVO = "btnNuevoB";
    static final String BTN_EDITAR = "btnEditarB";
    static final String BTN_ELIMINAR = "btnEliminarB";

    private final IChBanco chBdao;
    private final AccesoModuloHelper acceso;

    public ChBancoController(IChBanco chBdao, AccesoModuloHelper acceso) {
        this.chBdao = chBdao;
        this.acceso = acceso;
    }

    /** Todos los bancos. Lista pelada: es el contrato que ya usa el frontend. */
    @Secured({ "ROLE_ADM", "ROLE_LIM" }) //que un usuario admin o limitado si tiene acceso para consumir este recurso
    @PostMapping("/bancosX")
    public List<BancoDto> listadoX() {
        return this.chBdao.listBancos();
    }

    /** Los bancos habilitados para planillas. Lista pelada. */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/bancosPlanilla")
    public List<BancoDto> listadoBancosPlanilla() {
        return this.chBdao.listBancosPlanilla();
    }

    /**
     * Alta (codBanco 0/null) o edicion. Botones {@code btnNuevoB} / {@code btnEditarB}. El nombre es
     * obligatorio y tiene el formato del validador del legacy.
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/registrar")
    @Transactional
    public ResponseEntity<ApiResponse<?>> registrar(@RequestBody ChBanco banco, Authentication auth) {
        boolean alta = banco.getCodBanco() == null || banco.getCodBanco() == 0;
        MensajesCheque.exigirBoton(acceso, auth, VISTA_BANCOS, alta ? BTN_NUEVO : BTN_EDITAR);

        if (banco.getNombre() == null || banco.getNombre().isEmpty()) {
            throw new SpBusinessException(MensajesCheque.FALTA_NOMBRE_BANCO);
        }
        String error = ReglasCheque.errorNombreBanco(banco.getNombre());
        if (error != null) throw new SpBusinessException(error);

        if (!alta && chBdao.obtener(banco.getCodBanco()) == null) {
            throw new SpBusinessException(MensajesCheque.bancoNoEncontrado(banco.getCodBanco()));
        }
        banco.setAudUsuario(DatosToken.codUsuarioDe(auth));
        RespuestaSp res = chBdao.registrar(banco);
        return creado(alta ? "Banco registrado." : "Banco actualizado.", res.getIdGenerado());
    }

    /** Baja de un banco. Boton {@code btnEliminarB}. Ver el javadoc de la clase. */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/eliminar")
    @Transactional
    public ResponseEntity<ApiResponse<?>> eliminar(@RequestBody FiltroIdDto filtro, Authentication auth) {
        MensajesCheque.exigirBoton(acceso, auth, VISTA_BANCOS, BTN_ELIMINAR);
        if (filtro.getId() <= 0) throw new SpBusinessException(MensajesCheque.FALTA_BANCO_A_ELIMINAR);
        if (chBdao.obtener((int) filtro.getId()) == null) throw new SpBusinessException(MensajesCheque.bancoNoEncontrado((int) filtro.getId()));

        chBdao.eliminar((int) filtro.getId(), DatosToken.codUsuarioDe(auth));
        return creado("Banco eliminado.", filtro.getId());
    }

    private ResponseEntity<ApiResponse<?>> creado(String mensaje, long id) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(mensaje, id, HttpStatus.CREATED.value()));
    }
}
