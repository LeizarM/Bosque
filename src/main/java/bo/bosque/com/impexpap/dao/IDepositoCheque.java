package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.DepositoCheque;

import java.util.Date;
import java.util.List;


public interface IDepositoCheque {

    /**
     * Registra o actualiza un depósito de cheque mediante procedimiento almacenado.
     *
     * @param deposito objeto con los datos del depósito
     * @param accion    acción a ejecutar: "I" insertar, "U" actualizar, "A" asignar nro. transacción, "B" rechazar
     * @return true si la operación fue exitosa, false en caso contrario
     */
    boolean registrarDepositoCheque(DepositoCheque deposito, String accion);

    /**
     * Obtiene el último ID de depósito registrado por el usuario indicado.
     *
     * @param audUsuario ID del usuario que registró el depósito
     * @return el último ID registrado, o 0 si no existe ninguno
     */
    int obtenerUltimoId(int audUsuario);

    /**
     * Lista los últimos depósitos de cheque registrados.
     *
     * @return lista de depósitos, o lista vacía si no hay resultados
     */
    List<DepositoCheque> listarDepositosCheque();

    /**
     * Lista depósitos reconciliados filtrando por empresa, banco/cuenta, rango de fechas, cliente y estado.
     *
     * @param codEmpresa   código de la empresa
     * @param idBxC        ID del banco por cuenta
     * @param fechaInicio  fecha de inicio del rango
     * @param fechaFin     fecha de fin del rango
     * @param codCliente   código del cliente (puede ser vacío para todos)
     * @param estadoFiltro filtro de estado (puede ser vacío para todos)
     * @return lista de depósitos reconciliados, o lista vacía si no hay resultados
     */
    List<DepositoCheque> listarDepositosChequeReconciliado(int codEmpresa, int idBxC, Date fechaInicio, Date fechaFin, String codCliente, String estadoFiltro);

    /**
     * Lista depósitos pendientes de identificar filtrando por banco/cuenta, rango de fechas y cliente.
     *
     * @param idBxC       ID del banco por cuenta
     * @param fechaInicio fecha de inicio del rango
     * @param fechaFin    fecha de fin del rango
     * @param codCliente  código del cliente (puede ser vacío para todos)
     * @return lista de depósitos por identificar, o lista vacía si no hay resultados
     */
    List<DepositoCheque> lstDepositxIdentificar(int idBxC, Date fechaInicio, Date fechaFin, String codCliente);

}
