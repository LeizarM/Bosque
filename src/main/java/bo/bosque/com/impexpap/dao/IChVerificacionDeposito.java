package bo.bosque.com.impexpap.dao;

import java.time.LocalDate;
import java.util.List;

import bo.bosque.com.impexpap.dto.ChequePendienteFilaDto;
import bo.bosque.com.impexpap.dto.VerificacionDepositoFilaDto;
import bo.bosque.com.impexpap.model.ChVerificacionDeposito;
import bo.bosque.com.impexpap.utils.RespuestaSp;

/**
 * Acceso a {@code tch_verificacionDeposito} por {@code p_list_VerificacionDeposito} y
 * {@code p_abm_VerificacionDeposito}.
 *
 * <p>Cuidado con los nombres de los parametros del procedimiento de listado: {@code @estad} es el estado de la
 * <b>verificacion</b> (Y / N) y {@code @estado} el del <b>cheque</b> (PEN / CER).
 */
public interface IChVerificacionDeposito {

    // ------------------------------ Lecturas ------------------------------

    /**
     * Rama {@code A}: las verificaciones con los datos de su cheque. {@code fechaBanco} = el dia de la verificacion;
     * null = todas. El procedimiento no ordena: quien llama ordena.
     */
    List<VerificacionDepositoFilaDto> listar(LocalDate fechaBanco);

    /** Rama {@code A} con {@code @codvd}: una verificacion con los datos de su cheque, o {@code null}. */
    VerificacionDepositoFilaDto obtener(int codvd);

    /**
     * Rama {@code C} con la fecha: los cheques que NO tienen una verificacion valida y cuya fecha de cobranza es
     * {@code fechaCobranza} (el "solo los de hoy" del JSF). {@code estadoCheque} PEN / CER; null = todos. Ya vienen
     * ordenados por fecha de cobranza descendente y numero de cheque.
     */
    List<ChequePendienteFilaDto> sinRegularizarDelDia(LocalDate fechaCobranza, String estadoCheque);

    /**
     * Rama {@code D}: los cheques que NO tienen una verificacion valida y cuya fecha de cobranza es hoy o anterior
     * (el "todos los anteriores" del JSF). {@code estadoCheque} PEN / CER; null = todos. Ya vienen ordenados.
     */
    List<ChequePendienteFilaDto> sinRegularizarHastaHoy(String estadoCheque);

    /**
     * Rama {@code C} con {@code @codCheque}: ese cheque, si existe y todavia no tiene una verificacion valida; si no,
     * {@code null}. Es lo que cargaba {@code cargarNuevoRegistroSolic} al elegir "Seleccionar".
     */
    ChequePendienteFilaDto sinRegularizarDeUnCheque(long codCheque);

    /**
     * Rama {@code A} con {@code @estad='Y'} y {@code @codCheque}: si el cheque ya tiene una verificacion valida
     * ({@code verifikChkValid} del legacy).
     */
    boolean tieneVerificacionValida(long codCheque);

    // ------------------------------ Escrituras ------------------------------

    /** {@code p_abm_VerificacionDeposito} 'I'. Devuelve el codvd en {@code idGenerado}. */
    RespuestaSp registrar(ChVerificacionDeposito verificacion);

    /**
     * {@code p_abm_VerificacionDeposito} 'U': escribe banco, fecha, observacion y estado de la verificacion
     * {@code codvd}. Quien llama manda los cuatro (el procedimiento reescribe todos).
     */
    RespuestaSp actualizar(ChVerificacionDeposito verificacion);
}
