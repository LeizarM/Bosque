package bo.bosque.com.impexpap.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.PreparedStatementCreator;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * {@link SpHelper#ejecutarSinSalidas}: la escritura por nombre para procedimientos que no se pueden alterar (sin
 * {@code @error / @errormsg / @idGenerado}). El {@code JdbcTemplate} se simula de modo que ejecute de verdad el creador y la
 * funcion de retorno sobre un {@code PreparedStatement} simulado.
 */
class SpHelperSinSalidasTest {

    private Connection con;
    private PreparedStatement ps;
    private SpHelper helper;

    @BeforeEach
    @SuppressWarnings({"unchecked", "rawtypes"})
    void preparar() throws Exception {
        con = mock(Connection.class);
        ps = mock(PreparedStatement.class);
        when(con.prepareStatement(any(String.class))).thenReturn(ps);

        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.execute(any(PreparedStatementCreator.class), any(PreparedStatementCallback.class))).thenAnswer(inv -> {
            PreparedStatementCreator creador = inv.getArgument(0);
            PreparedStatementCallback<?> funcion = inv.getArgument(1);
            PreparedStatement p = creador.createPreparedStatement(con);
            try {
                return funcion.doInPreparedStatement(p);
            } catch (SQLException e) {
                // el JdbcTemplate real traduce la SQLException a una DataAccessException
                throw new UncategorizedSQLException("PreparedStatementCallback", "EXEC", e);
            }
        });
        helper = new SpHelper(jdbc, new ObjectMapper());
    }

    private static Map<String, Object> usuario(int cod) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("audUsuario", cod);
        return p;
    }

    @Test
    @DisplayName("arma EXEC por nombre con los parametros y @ACCION, sin ningun OUTPUT, y los bindea en orden")
    void armaLaLlamada() throws Exception {
        when(ps.execute()).thenReturn(false);
        when(ps.getUpdateCount()).thenReturn(-1);

        helper.ejecutarSinSalidas("p_abm_SocioNegocio", usuario(7), "B");

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(con).prepareStatement(sql.capture());
        assertEquals("EXEC p_abm_SocioNegocio @audUsuario=?, @ACCION=?", sql.getValue());
        assertFalse(sql.getValue().toUpperCase().contains("OUTPUT"), "no declara ni pide salidas");
        verify(ps).setObject(1, 7);
        verify(ps).setObject(2, "B");
    }

    @Test
    @DisplayName("recorre TODOS los resultados (result sets y conteos), no solo el primero")
    void recorreTodo() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.next()).thenReturn(true, false);
        when(ps.execute()).thenReturn(true);
        when(ps.getResultSet()).thenReturn(rs);
        // despues del result set: un conteo de 3 filas y despues el fin (-1)
        when(ps.getMoreResults()).thenReturn(false, false);
        when(ps.getUpdateCount()).thenReturn(3, -1);

        helper.ejecutarSinSalidas("p_abm_Algo", usuario(1), "X");

        verify(rs).close();
        verify(ps, times(2)).getMoreResults();
    }

    @Test
    @DisplayName("solo conteos de filas (el caso de p_abm_SocioNegocio con NOCOUNT OFF): los avanza todos")
    void soloConteos() throws Exception {
        when(ps.execute()).thenReturn(false);
        when(ps.getUpdateCount()).thenReturn(12768, 12768, 51, -1);
        when(ps.getMoreResults()).thenReturn(false, false, false);

        helper.ejecutarSinSalidas("p_abm_SocioNegocio", usuario(7), "B");

        verify(ps, times(3)).getMoreResults();
    }

    @Test
    @DisplayName("un error que llega al pedir el siguiente resultado (despues de un conteo) NO se pierde: sale como RuntimeException con la causa")
    void errorTardio() throws Exception {
        SQLException sap = new SQLException("OLE DB provider returned message \"Login timeout expired\"", "42000", 7399);
        when(ps.execute()).thenReturn(false);
        when(ps.getUpdateCount()).thenReturn(5);
        when(ps.getMoreResults()).thenThrow(sap);

        RuntimeException e = assertThrows(RuntimeException.class,
                () -> helper.ejecutarSinSalidas("p_abm_SocioNegocio", usuario(7), "B"));

        assertEquals("Error de conexión o sintaxis en la base de datos.", e.getMessage());
        Throwable causa = e;
        while (causa.getCause() != null && causa.getCause() != causa) {
            causa = causa.getCause();
        }
        assertSame(sap, causa);
    }

    @Test
    @DisplayName("un error del primer statement (execute) tambien sale con la causa original")
    void errorInmediato() throws Exception {
        SQLException e1 = new SQLException("falla", "42000", 208);
        when(ps.execute()).thenThrow(e1);

        RuntimeException e = assertThrows(RuntimeException.class,
                () -> helper.ejecutarSinSalidas("p_abm_Algo", usuario(1), "B"));

        assertTrue(e.getCause() instanceof UncategorizedSQLException);
        assertSame(e1, e.getCause().getCause());
    }
}
