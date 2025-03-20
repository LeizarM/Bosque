package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.DepositoCheque;

import java.util.Date;
import java.util.List;


public interface IDepositoCheque {


    /**
     * REgistrar un depósito cheque
     * @param mb
     * @param acc
     * @return
     */
    boolean registrarDepositoCheque(DepositoCheque mb, String acc);

    /**
     * Obtener el último ID registrado
     * @param audUsuario
     * @return
     */
    int obtenerUltimoId(int audUsuario);

    /**
     * Validar si existe un registro con los mismos datos
     * @param mb
     * @return
     */
    //int existeRegistro( DepositoCheque mb );

    /**
     * Listar todos los depósitos cheque solo los ultimos X registros
     * @return
     */
    List<DepositoCheque> listarDepositosCheque();


    /**
     * Lsitara los depositos con el idBxC, fecha inicio, fecha fin y código de cliente
     * @param idBxC
     * @param fechaInicio
     * @param fechaFin
     * @param codCliente
     * @return
     */
    List<DepositoCheque> listarDepositosChequeReconciliado(int idBxC, Date fechaInicio, Date fechaFin, String codCliente);

    /**
     * Listar todos los depósitos cheque por identificar con el idBxC, fecha inicio, fecha fin y código de cliente
     * @param idBxC
     * @param fechaInicio
     * @param fechaFin
     * @param codCliente
     * @return
     */
    List<DepositoCheque> lstDepositxIdentificar(int idBxC, Date fechaInicio, Date fechaFin, String codCliente);

}
