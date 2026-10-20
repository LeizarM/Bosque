package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.TcpePersonaExamenDto;

/**
 * {@code p_abm_tcpe_GenerarExamen} (contrato 3.3): crea <b>un</b> examen del lote con su selección de preguntas y
 * sus fotos. El backend lo llama N veces dentro de una transacción ({@code TcpeLoteService}).
 */
public interface ITcpeGenerarExamen {

    /**
     * @param codEmpleado  empleado que rinde (usa o crea su {@code tcpe_usuario}); null para un externo
     * @param password2    hash BCrypt de la clave del externo (obligatorio si {@code codEmpleado} es null)
     * @return el {@code codExamen} creado
     */
    long generar(long codRolExamen, Long codEmpleado, String password2, long audUsuario);

    /**
     * Externo identificado por CI: el SP reutiliza su usuario (conserva el login y le pone esta clave) o lo crea.
     *
     * @param password2 hash BCrypt de la clave en claro que se le muestra
     * @return el {@code codExamen} creado
     */
    long generarParaPersona(long codRolExamen, TcpePersonaExamenDto persona, String password2, long audUsuario);
}
