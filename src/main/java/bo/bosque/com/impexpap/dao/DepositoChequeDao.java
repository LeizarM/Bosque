package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.DepositoCheque;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@Repository
public class DepositoChequeDao implements IDepositoCheque {

    private static final Logger logger = LoggerFactory.getLogger(DepositoChequeDao.class);

    private static final String SQL_STORED_PROCEDURE =
            "execute p_abm_tdep_DepositoCheques " +
                    "@idDeposito = ?, " +
                    "@codCliente = ?, " +
                    "@codEmpresa = ?, " +
                    "@idBxC = ?, " +
                    "@importe = ?, " +
                    "@moneda = ?, " +
                    "@estado = ?, " +
                    "@fotoPath = ?, " +
                    "@aCuenta = ?, " +
                    "@fechaI = ?,"+
                    "@nroTransaccion = ?,"+
                    "@obs = ?,"+
                    "@audUsuario = ?, " +
                    "@ACCION = ?";

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public DepositoChequeDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Registra o actualiza un depósito de cheque utilizando un procedimiento almacenado.
     *
     * @param deposito Objeto con los datos del depósito
     * @param accion   Acción a realizar: "I" insertar, "U" actualizar, "A" asignar nro. transacción, "B" rechazar
     * @return true si la operación fue exitosa, false en caso contrario
     */
    @Override
    public boolean registrarDepositoCheque(DepositoCheque deposito, String accion) {
        try {
            int affectedRows = jdbcTemplate.update(SQL_STORED_PROCEDURE, ps -> {
                ps.setEscapeProcessing(true);
                ps.setInt(1, deposito.getIdDeposito());
                ps.setString(2, deposito.getCodCliente());
                ps.setInt(3, deposito.getCodEmpresa());
                ps.setInt(4, deposito.getIdBxC());
                ps.setBigDecimal(5, deposito.getImporte());
                ps.setString(6, deposito.getMoneda());
                ps.setInt(7, deposito.getEstado());
                ps.setString(8, deposito.getFotoPath());
                ps.setBigDecimal(9, deposito.getACuenta());
                ps.setDate(10, deposito.getFechaI() != null
                        ? new java.sql.Date(deposito.getFechaI().getTime()) : null);
                ps.setString(11, deposito.getNroTransaccion());
                ps.setString(12, deposito.getObs());
                ps.setInt(13, deposito.getAudUsuario());
                ps.setString(14, accion);
            });

            return affectedRows > 0;

        } catch (DataAccessException ex) {
            logDataAccessException(ex, "Error al registrar depósito de cheque");
            return false;
        }
    }


    /**
     * Obtiene el último ID de depósito registrado por el usuario indicado.
     *
     * @param audUsuario ID del usuario que registró el depósito
     * @return el último ID registrado, o 0 si no existe ninguno
     */
    @Override
    public int obtenerUltimoId(int audUsuario) {
        try {
            Integer result = this.jdbcTemplate.queryForObject(
                    "EXEC p_list_tdep_DepositoCheques @audUsuario=?, @ACCION=?",
                    new Object[]{audUsuario, "A"},
                    new int[]{Types.INTEGER, Types.VARCHAR},
                    Integer.class);
            return result != null ? result : 0;
        } catch (EmptyResultDataAccessException e) {
            logger.warn("No se encontró último ID para audUsuario={}", audUsuario);
            return 0;
        } catch (BadSqlGrammarException e) {
            logger.error("Error de SQL al obtener último ID: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Lista los últimos depósitos de cheque registrados.
     *
     * @return lista de depósitos, o lista vacía si no hay resultados
     */
    @Override
    public List<DepositoCheque> listarDepositosCheque() {
        return new ArrayList<>();
    }

    /**
     * Listar todos los depósitos cheque solo los ultimos X registros
     *
     */
        public List<DepositoCheque> listarDepositosChequeReconciliado(int codEmpresa, int idBxC, Date fechaInicio, Date fechaFin, String codCliente, String estadoFiltro){
            List<DepositoCheque> lstTemp = new ArrayList<>();

            try {
                lstTemp = this.jdbcTemplate.query(
                        "execute p_list_tdep_DepositoCheques @codEmpresa=?, @idBxC=?, @fechaInicio=?, @fechaFin=?, @codCliente=?, @estadoFiltro=? , @ACCION=?",
                        new Object[] { codEmpresa, idBxC, fechaInicio, fechaFin, codCliente, estadoFiltro ,"C" },
                        new int[] {Types.INTEGER, Types.INTEGER, Types.VARCHAR,Types.VARCHAR, Types.VARCHAR, Types.VARCHAR ,Types.VARCHAR },
                        (rs, rowNum) -> {
                            DepositoCheque temp = new DepositoCheque();

                            temp.setIdDeposito(rs.getInt(1));
                            temp.setCodCliente(rs.getString(2));
                            temp.setNombreBanco(rs.getString(3));
                            temp.setNombreEmpresa(rs.getString(4));
                            temp.setImporte(rs.getBigDecimal(5));
                            temp.setMoneda(rs.getString(6));
                            temp.setACuenta(rs.getBigDecimal(7));
                            temp.setNumeroDeDocumentos(rs.getString(8));
                            temp.setFechasDeDepositos(rs.getString(9));
                            temp.setNumeroDeFacturas(rs.getString(10));
                            temp.setTotalMontos(rs.getString(11));
                            temp.setEsPendiente(rs.getString(12));
                            temp.setFechaI(rs.getDate(13));
                            temp.setNroTransaccion(rs.getString(14));
                            temp.setCodEmpresa( rs.getInt(15) );
                            temp.setIdBxC( rs.getInt(16) );
                            temp.setNombreCompleto(rs.getString(17));
                            temp.setNombreVendedor(rs.getString(18));

                            return temp;
                        });
            } catch (DataAccessException ex) {  // Cambiamos a DataAccessException
                logDataAccessException(ex, "Error al listar depósitos que fueron o no reconciliados"); // Usamos el método auxiliar
                lstTemp = new ArrayList<>(); // Inicializamos lista vacía
            }

            return lstTemp;
        }

    /**
     * Listar todos los depósitos cheque por identificar con el idBxC, fecha inicio, fecha fin y código de cliente
     *
     * @param idBxC
     * @param fechaInicio
     * @param fechaFin
     * @param codCliente
     * @return
     */
    @Override
    public List<DepositoCheque> lstDepositxIdentificar(int idBxC, Date fechaInicio, Date fechaFin, String codCliente) {
        List<DepositoCheque> lstTemp = new ArrayList<>();

        try {
            lstTemp = this.jdbcTemplate.query(
                    "execute p_list_tdep_DepositoCheques @idBxC=?, @fechaInicio=?, @fechaFin=?, @codCliente=?, @ACCION=?",
                    new Object[] { idBxC, fechaInicio, fechaFin, codCliente ,"B" },
                    new int[] { Types.INTEGER, Types.VARCHAR,Types.VARCHAR, Types.VARCHAR ,Types.VARCHAR },
                    (rs, rowNum) -> {
                        DepositoCheque temp = new DepositoCheque();

                        temp.setIdDeposito(rs.getInt(1));
                        temp.setCodCliente(rs.getString(2));
                        temp.setCodEmpresa(rs.getInt(3));
                        temp.setNombreEmpresa(rs.getString(4));
                        temp.setIdBxC(rs.getInt(5));
                        temp.setNombreBanco(rs.getString(6));
                            temp.setImporte(rs.getBigDecimal(7));
                            temp.setMoneda(rs.getString(8));
                            temp.setACuenta(rs.getBigDecimal(9));
                        temp.setObs(rs.getString(10));
                        temp.setFechaI(rs.getDate(11));
                        temp.setEsPendiente(rs.getString(12));

                        return temp;
                    });
        } catch (DataAccessException ex) {  // Cambiamos a DataAccessException
            logDataAccessException(ex, "Error al listar lstDepositxIdentificar"); // Usamos el método auxiliar
            lstTemp = new ArrayList<>(); // Inicializamos lista vacía
        }

        return lstTemp;
    }


    /**
     * Método auxiliar para registrar errores de acceso a datos
     * @param ex Excepción ocurrida
     * @param mensaje Mensaje descriptivo del error
     */
    private void logDataAccessException(DataAccessException ex, String mensaje) {
        logger.error(mensaje);
        Throwable rootCause = ex.getRootCause();
        if (rootCause instanceof SQLException) {
            SQLException sqlEx = (SQLException) rootCause;
            logger.error("Código de error SQL: {}, Estado SQL: {}",
                    sqlEx.getErrorCode(),
                    sqlEx.getSQLState());
        }
        logger.error("Detalle del error:", ex);
    }
}