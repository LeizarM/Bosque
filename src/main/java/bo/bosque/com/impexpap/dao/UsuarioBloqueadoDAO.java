package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.UsuarioBloqueado;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.batch.BatchProperties;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.sql.Types;

@Repository
public class UsuarioBloqueadoDAO implements IUsuarioBloqueado {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public boolean registrarAdvertencia(UsuarioBloqueado ub, String acc){
        int resp;
        try{
            resp = this.jdbcTemplate.update("execute p_abm_UsuarioBloqueado @codUsuario=?, @fechaAdvertencia=?, @fechaLimite=?, @bloqueado=?, @audUsuarioI=?, @ACCION=?",
                    ps -> {
                        ps.setInt(1, ub.getCodUsuario());
                        ps.setTimestamp(2, new java.sql.Timestamp(ub.getFechaAdvertencia().getTime()));
                        ps.setTimestamp(3, new java.sql.Timestamp(ub.getFechaLimite().getTime()));
                        ps.setInt(4, ub.getBloqueado());
                        ps.setInt(5, ub.getAudUsuario());
                        ps.setString(6, acc);

                    }
                    );
        } catch (BadSqlGrammarException e) {
            System.out.println("Error: UsuarioBloqueadoDAO en insertar advertencia, DataAccessException->" + e.getMessage() + ",SQL code->" + ((SQLException) e.getCause()).getErrorCode());
            this.jdbcTemplate = null;
            resp = 0;
        }
        return resp !=0;
    }

    // Actualizar advertencia/bloqueo
    public boolean actualizarAdvertencia(UsuarioBloqueado ub, String acc) {
        int resp;
        try {
            resp = this.jdbcTemplate.update(
                    "EXEC p_abm_UsuarioBloqueado @codUsuario=?, @fechaAdvertencia=?, @fechaLimite=?, @bloqueado=?, @audUsuarioI=?, @ACCION=?",
                    ps -> {
                        ps.setLong(1, ub.getCodUsuario());
                        ps.setTimestamp(2, new java.sql.Timestamp(ub.getFechaAdvertencia().getTime()));
                        ps.setTimestamp(3, new java.sql.Timestamp(ub.getFechaLimite().getTime()));
                        ps.setInt(4, ub.getBloqueado());
                        ps.setLong(5, ub.getAudUsuario());
                        ps.setString(6, "U");
                    }
            );
        } catch (BadSqlGrammarException e) {
            System.out.println("Error: UsuarioBloqueadoDAO en actualizarAdvertencia, DataAccessException->" + e.getMessage() + ", SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            this.jdbcTemplate = null;
            resp = 0;
        }
        return resp != 0;
    }
    // Eliminar advertencia/bloqueo
    public boolean eliminarAdvertencia(int codUsuario) {
        int resp;
        try {
            resp = this.jdbcTemplate.update(
                    "execute p_abm_UsuarioBloqueado @codUsuario=?, @ACCION=?",
                    ps -> {
                        ps.setInt(1, codUsuario);
                        ps.setString(2, "D");
                    }
            );
            return true;
        } catch (BadSqlGrammarException e) {
            System.out.println("Error: UsuarioBloqueadoDAO en eliminarAdvertencia, DataAccessException->" + e.getMessage() + ", SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            this.jdbcTemplate = null;
            return false;
        }
    }
    // Consultar advertencia/bloqueo por usuario
    public UsuarioBloqueado obtenerPorUsuario(int codUsuario) {
        UsuarioBloqueado ub = null;
        try {
            ub = this.jdbcTemplate.queryForObject("execute p_list_UsuarioBloqueado @codUsuario = ?, @ACCION=?",
                    new Object[]{codUsuario,"L"},
                    new int [] {Types.INTEGER,Types.VARCHAR},
                    (rs, rowNum) -> {
                        UsuarioBloqueado temp = new UsuarioBloqueado();
                        temp.setCodUsuario(rs.getInt("codUsuario"));
                        temp.setFechaAdvertencia(rs.getTimestamp("fechaAdvertencia"));
                        temp.setFechaLimite(rs.getTimestamp("fechaLimite"));
                        temp.setBloqueado(rs.getInt("bloqueado"));
                        temp.setAudUsuario(rs.getInt("audUsuarioI"));
                        return temp;
                    });
            // --- Lógica de bloqueo automático ---
            if (ub != null && ub.getBloqueado() == 0 && ub.getFechaLimite() != null) {
                java.util.Date ahora = new java.util.Date();
                if (ub.getFechaLimite().before(ahora)) {
                    ub.setBloqueado(1);
                    // Actualiza en la base de datos para persistir el cambio
                    this.registrarAdvertencia(ub, "U");
                }
            }
        } catch (Exception e) {
            // Si no existe, retorna null
            ub = null;
        }
        return ub;
    }
    // Verificar si usuario está bloqueado
    public boolean estaUsuarioBloqueado(int codUsuario) {
        UsuarioBloqueado ub = obtenerPorUsuario(codUsuario);
        return ub != null && ub.getBloqueado() == 1;
    }
}
