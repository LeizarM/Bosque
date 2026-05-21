package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.DepositoCheque;

import java.util.Date;
import java.util.List;


public interface IDepositoCheque {


    /**
     * Registra un nuevo depósito de cheque en el sistema.
     *
     * @param mb   objeto {@link DepositoCheque} con los datos del depósito a registrar
     * @param acc  acción de auditoría que identifica la operación realizada
     * @return {@code true} si el registro se completó exitosamente, {@code false} en caso contrario
     */
    boolean registrarDepositoCheque(DepositoCheque mb, String acc);

    /**
     * Obtiene el último identificador de depósito registrado por el usuario especificado.
     *
     * @param audUsuario código del usuario de auditoría que realizó el registro
     * @return el último ID de depósito generado para el usuario
     */
    int obtenerUltimoId(int audUsuario);

    /**
     * Valida si ya existe un registro de depósito con los mismos datos.
     *
     * @param mb objeto {@link DepositoCheque} con los datos a verificar
     * @return el ID del registro existente, o 0 si no existe coincidencia
     */
    //int existeRegistro( DepositoCheque mb );

    /**
     * Lista los depósitos de cheque más recientes registrados en el sistema.
     * Retorna únicamente los últimos registros según el límite definido en la consulta.
     * @return lista de {@link DepositoCheque} con los registros más recientes
     */
    List<DepositoCheque> listarDepositosCheque();


    /**
     * Lista los depósitos de cheque reconciliados según los filtros proporcionados.
     *
     * @param codEmpresa   código de la empresa para filtrar los resultados
     * @param idBxC        identificador del banco por cliente (BxC) para filtrar
     * @param fechaInicio  fecha de inicio del rango de búsqueda (inclusive)
     * @param fechaFin     fecha de fin del rango de búsqueda (inclusive)
     * @param codCliente   código del cliente para filtrar; puede ser vacío para incluir todos
     * @param estadoFiltro filtro de estado del depósito (ej.: pendiente, conciliado, anulado)
     * @return lista de {@link DepositoCheque} que cumplen con los criterios de reconciliación
     */
    List<DepositoCheque> listarDepositosChequeReconciliado(int codEmpresa, int idBxC, Date fechaInicio, Date fechaFin, String codCliente, String estadoFiltro);

    /**
     * Lista los depósitos de cheque que aún están pendientes de identificación,
     * filtrados por banco por cliente, rango de fechas y código de cliente.
     *
     * @param idBxC       identificador del banco por cliente (BxC) para filtrar
     * @param fechaInicio fecha de inicio del rango de búsqueda (inclusive)
     * @param fechaFin    fecha de fin del rango de búsqueda (inclusive)
     * @param codCliente  código del cliente para filtrar; puede ser vacío para incluir todos
     * @return lista de {@link DepositoCheque} pendientes de identificación
     */
    List<DepositoCheque> lstDepositxIdentificar(int idBxC, Date fechaInicio, Date fechaFin, String codCliente);

}