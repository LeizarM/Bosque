package bo.bosque.com.impexpap.commons;

import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import bo.bosque.com.impexpap.config.SinPermisoException;
import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.dao.ISocioNegocioSap;
import bo.bosque.com.impexpap.security.jwt.DatosToken;

/**
 * "ACTUALIZAR DATOS SAP" de la pantalla de Cheques: trae a {@code text_SocioNegocio} los clientes nuevos de SAP.
 *
 * <p>Sin esos clientes un cheque desaparece de la grilla (la consulta hace {@code INNER JOIN} con
 * {@code text_SocioNegocio} por cliente y empresa) y el cliente no sale en el combo del formulario.
 *
 * <h3>Que hacia el legacy</h3>
 * {@code WizardCheque.actualizarRegistos} -> {@code SocioNegocioSapDAO.abmSocioNegocioSapBosque} llamaba a
 * {@code p_abm_SocioNegocio} con seis parametros por posicion (la firma del procedimiento del servidor de SAP), y en
 * esta base el procedimiento tiene once: la 'B' caia en {@code @datoCiudad}, {@code @ACCION} quedaba en NULL y no
 * ejecutaba nada, pero el boton decia "Registros Actualizados" (comprobado con {@code ProbarLlamadaSocios.java}). Aqui se
 * llama por nombre y la rama 'B' corre de verdad, que es lo que el boton queria hacer (su comentario dice "realizara por
 * Default el Insert de los nuevos registros") y lo que ya hace {@code p_list_tdep_BancoXCuenta} 'A' cada vez que
 * Depositos lista sus cuentas. Decision del usuario: se migra con la intencion y <b>sin tocar el procedimiento</b>.
 *
 * <h3>Reglas que se conservan</h3>
 * Permiso: el del legacy, {@code wInfoCenter.esAutorizado('btnNuevoCH')} = {@code Loggin.autorizarBtn}: el boton
 * {@code btnNuevoCH} de la vista 42 (existe una sola vez en {@code tb_vistaBtn}) o administrador. No depende de la
 * empresa ni de la sucursal: el procedimiento trae las cuatro empresas de SAP.
 *
 * <h3>Lo que se agrega</h3>
 * <ul>
 *   <li>El usuario de auditoria sale del token (el legacy lo tomaba de la sesion).</li>
 *   <li>Una sola actualizacion a la vez ({@link #enCurso}): cada una lee SAP y reescribe unas 12.800 filas, y dos
 *       a la vez se pisarian al insertar los mismos clientes nuevos. La segunda recibe un mensaje claro en vez de
 *       esperar o de fallar por clave duplicada.</li>
 *   <li>Mensajes explicitos ({@link MensajesClientesSap}).</li>
 * </ul>
 *
 * <p>El procedimiento no devuelve cuantos clientes trajo (no tiene salidas y no se altera), asi que este servicio tampoco.
 *
 * <p><b>Sin {@code @Transactional} a proposito.</b> Es una sola llamada: no hay nada que agrupar, y abrir una transaccion
 * aqui dejaria la consulta a SAP (un servidor remoto que puede colgarse) dentro de una transaccion del servicio,
 * manteniendo bloqueos sobre la tabla mientras se espera.
 */
@Service
public class ClientesSapService {

    private static final Logger log = LoggerFactory.getLogger(ClientesSapService.class);

    /** {@code tb_vista.codVista} de Cheques. */
    static final int VISTA_CHEQUES = 42;
    /** El boton del legacy; es el mismo de "Registrar". */
    static final String BOTON = "btnNuevoCH";

    private final ISocioNegocioSap clientes;
    private final AccesoModuloHelper acceso;
    private final ReentrantLock enCurso = new ReentrantLock();

    public ClientesSapService(ISocioNegocioSap clientes, AccesoModuloHelper acceso) {
        this.clientes = clientes;
        this.acceso = acceso;
    }

    /**
     * Trae los clientes nuevos de SAP y actualiza los ya cargados.
     *
     * @throws SinPermisoException 403 si el usuario no tiene {@code btnNuevoCH} (el administrador pasa siempre)
     * @throws SpBusinessException 400 con el motivo y que hacer, si ya hay una en curso o si la base fallo
     */
    public void actualizarDesdeSap(Authentication auth) {
        exigirPermiso(auth);
        int usuario = DatosToken.codUsuarioDe(auth);

        if (!enCurso.tryLock()) {
            throw new SpBusinessException(MensajesClientesSap.YA_EN_CURSO);
        }
        try {
            clientes.actualizarDesdeSap(usuario);
        } catch (SpBusinessException e) {
            throw e;
        } catch (RuntimeException e) {
            // SpHelper envuelve toda falla de base de datos en un texto generico; el detalle esta en la causa.
            log.error("No se pudo actualizar los clientes desde SAP (usuario {})", usuario, e);
            throw new SpBusinessException(MensajesClientesSap.NO_SE_PUDO);
        } finally {
            enCurso.unlock();
        }
    }

    /** Como {@code acceso.exigirBoton}, pero el 403 dice que permiso falta y que hacer ({@link SinPermisoException}). */
    private void exigirPermiso(Authentication auth) {
        try {
            acceso.exigirBoton(auth, VISTA_CHEQUES, BOTON);
        } catch (AccessDeniedException e) {
            throw new SinPermisoException(MensajesClientesSap.sinPermiso(BOTON));
        }
    }
}
