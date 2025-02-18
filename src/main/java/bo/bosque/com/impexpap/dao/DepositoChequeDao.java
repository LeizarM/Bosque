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
import java.util.List;


@Repository
public class DepositoChequeDao implements IDepositoCheque {

    private static final Logger logger = LoggerFactory.getLogger(DepositoChequeDao.class);

    private static final String SQL_STORED_PROCEDURE =
            "execute p_abm_tdep_DepositoCheques " +
                    "@idDeposito = ?, " +
                    "@codCliente = ?, " +
                    "@docNum = ?, " +
                    "@numFact = ?, "+
                    "@anioFact = ?, "+
                    "@codEmpresa = ?, " +
                    "@codBanco = ?, " +
                    "@importe = ?, " +
                    "@moneda = ?, " +
                    "@fotoPath = ?, " +
                    "@audUsuario = ?, " +
                    "@ACCION = ?";

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public DepositoChequeDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Registra un depósito de cheque utilizando un procedimiento almacenado.
     *
     * @param mb Objeto con los datos del depósito
     * @param acc Acción a realizar en el procedimiento almacenado
     * @return true si la operación fue exitosa, false en caso contrario
     */
    @Override
    public boolean registrarDepositoCheque(DepositoCheque mb, String acc) {
        try {
            int affectedRows = jdbcTemplate.update(SQL_STORED_PROCEDURE, ps -> {
                ps.setEscapeProcessing(true);
                ps.setInt(1, mb.getIdDeposito());
                ps.setString(2, mb.getCodCliente());
                ps.setInt(3, mb.getDocNum());
                ps.setInt(4, mb.getNumFact());
                ps.setInt(5, mb.getAnioFact());
                ps.setInt(6, mb.getCodEmpresa());
                ps.setInt(7, mb.getCodBanco());
                ps.setFloat(8, mb.getImporte());
                ps.setString(9, mb.getMoneda());
                ps.setString(10, mb.getFotoPath());
                ps.setInt(11, mb.getAudUsuario());
                ps.setString(12, acc);
            });

            return affectedRows > 0;

        } catch (DataAccessException ex) {
            logDataAccessException(ex, "Error al registrar depósito de cheque");
            return false;
        }
    }


    /**
     * Obtiene el último ID registrado en la tabla
     * @param audUsuario
     * @return
     */
    public int obtenerUltimoId(int audUsuario) {

        try {
            return this.jdbcTemplate.queryForObject("EXEC p_list_tdep_DepositoCheques @audUsuario=?, @ACCION=?",
                    new Object[]{audUsuario, "A"},
                    new int[]{Types.INTEGER, Types.VARCHAR},
                    Integer.class // Espera un entero como resultado
            );
        } catch (EmptyResultDataAccessException e) {
            // Si no hay resultados, retorna 0
            return 0;
        } catch (BadSqlGrammarException e) {
            System.err.println("Error de SQL: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Validar si existe un registro con los mismos datos
     *
     * @param mb
     * @return
     */
    @Override
    public int existeRegistro(DepositoCheque mb) {
        try {
            return this.jdbcTemplate.queryForObject("execute p_list_tdep_DepositoCheques @codEmpresa = ?, @numFact = ?, @codCliente = ?, @docNum = ?, @ACCION = ?",
                    new Object[]{ mb.getCodEmpresa(), mb.getNumFact(), mb.getCodCliente(), mb.getDocNum()  , "B"},
                    new int[]{ Types.INTEGER, Types.INTEGER, Types.VARCHAR, Types.INTEGER, Types.VARCHAR } ,
                    Integer.class // Espera un entero como resultado
            );
        } catch (EmptyResultDataAccessException e) {
            // Si no hay resultados, retorna 0
            return 0;
        } catch (BadSqlGrammarException e) {
            System.err.println("Error de SQL: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Listar todos los depósitos cheque solo los ultimos X registros
     *
     * @return
     */
    @Override
    public List<DepositoCheque> listarDepositosCheque() {
        List<DepositoCheque> lstTemp = new ArrayList<>();

        try {
            lstTemp = this.jdbcTemplate.query(
                    "execute p_list_tdep_DepositoCheques @ACCION = ?",
                    new Object[] { "C" },
                    new int[] { Types.VARCHAR },
                    (rs, rowNum) -> {
                        DepositoCheque temp = new DepositoCheque();

                        temp.setIdDeposito(rs.getInt(1));
                        temp.setCodCliente(rs.getString(2));
                        temp.setDocNum(rs.getInt(3));
                        temp.setNumFact(rs.getInt(4));
                        temp.setAnioFact(rs.getInt(5));
                        temp.setCodEmpresa(rs.getInt(6));
                        temp.setCodBanco(rs.getInt(7));
                        temp.setImporte(rs.getFloat(8));
                        temp.setMoneda(rs.getString(9));
                        temp.setEstado(rs.getInt(10));
                        temp.setFotoPath(rs.getString(11));
                        temp.setAudUsuario(rs.getInt(12));
                        temp.setNombreBanco(rs.getString(13));
                        temp.setNombreEmpresa(rs.getString(14));

                        return temp;
                    });
        } catch (DataAccessException ex) {  // Cambiamos a DataAccessException
            logDataAccessException(ex, "Error al listar depósitos de cheque"); // Usamos el método auxiliar
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