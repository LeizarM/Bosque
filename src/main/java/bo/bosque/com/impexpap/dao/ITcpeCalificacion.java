package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.utils.RespuestaSp;

/** Calificación del evaluador: {@code p_abm_tcpe_Calificacion} (contrato 3.5). */
public interface ITcpeCalificacion {

    /** {@code P}: marca una abierta como correcta o incorrecta (el examen debe estar entregado). */
    RespuestaSp marcar(long codExamen, Long codRespuestaH, Boolean esCorrecta, long audUsuario);

    /** {@code C}: cierra la calificación; exige todas las abiertas marcadas y calcula el resultado. */
    RespuestaSp cerrar(long codExamen, long audUsuario);

    /** {@code N}: nota manual de 0 a 100 (escritos). */
    RespuestaSp nota(long codExamen, Double resultado, long audUsuario);
}
