// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dao\ITesBase.java
package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.DiaRevisadoTesBaseDto;
import bo.bosque.com.impexpap.dto.TraspasoEfectivoPendienteDto;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.Date;
import java.util.List;

/**
 * La parte de TesBase que usa la tarea rutinaria "Verificar Traspaso de
 * Efectivo Entre Sistemas" (idATR 11).
 *
 * <p>Solo la verificación. El registro de transferencias (vista 102, bajo
 * Cobranza) sigue en el sistema anterior: decisión de Marcelo, 2026-09-11.
 */
public interface ITesBase {

    /** [B] Las transferencias en estado PEN, con el nombre de su empresa. */
    List<TraspasoEfectivoPendienteDto> listarPendientes();

    /**
     * [C] Cierra una transferencia pendiente desde la tarea.
     *
     * <p>El SP rechaza si la ocurrencia no es de una tarea con idATR 11
     * (error 22) o si la transferencia ya no está pendiente (error 23).
     */
    RespuestaSp cerrar(long codTes, long idBitTarRuti, long audUsuario);

    /**
     * [S] Da el día por revisado cuando no queda ninguna pendiente.
     *
     * <p>El SP vuelve a contar las pendientes registradas hasta el día que
     * revisa la ocurrencia (ver {@link #diaRevisado}) y rechaza si hay alguna
     * (error 24): si el cliente pudiera afirmarlo por su cuenta, el registro
     * no valdría nada.
     */
    RespuestaSp sinPendientes(long idBitTarRuti, long audUsuario);

    /**
     * [H] Qué día revisa una ocurrencia: el hábil anterior a su fecha, con los
     * feriados de la sucursal de quien la tiene. Nulo si la ocurrencia no es
     * de una tarea con idATR 11. Archivo SQL 58.
     */
    DiaRevisadoTesBaseDto diaRevisado(long idBitTarRuti);

    /**
     * [D] Las transferencias registradas en {@code fecha}, en cualquier estado
     * y con las pendientes primero. Archivo SQL 58.
     */
    List<TraspasoEfectivoPendienteDto> listarDelDia(Date fecha);
}
