package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Login;
import bo.bosque.com.impexpap.security.jwt.UsuarioToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.core.userdetails.UserDetails;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** relacionActiva de p_list_Usuario 'C' (verifyUser) y relacionActiva + bloqueoAdministrador de 'B' (loadUserByUsername, sql/06). */
class LoginDaoImplTest {

    private LoginDaoImpl dao;
    private JdbcTemplate jdbc;
    private ResultSet fila;

    @BeforeEach
    void setUp() throws SQLException {
        jdbc = mock(JdbcTemplate.class);
        dao = new LoginDaoImpl();
        dao.jdbcTemplate = jdbc;
        fila = mock(ResultSet.class);
        when(fila.getInt(1)).thenReturn(5);
        when(fila.getInt(2)).thenReturn(7);
        when(jdbc.queryForObject(anyString(), any(Object[].class), any(int[].class), any(RowMapper.class)))
                .thenAnswer(inv -> ((RowMapper<?>) inv.getArgument(3)).mapRow(fila, 0));
    }

    @Test
    void relacionInactivaLlegaComoFalse() throws SQLException {
        when(fila.getInt("relacionActiva")).thenReturn(0);

        assertFalse(dao.verifyUser("jperez", "10.0.0.1").isRelacionActiva());
    }

    @Test
    void relacionActivaLlegaComoTrue() throws SQLException {
        when(fila.getInt("relacionActiva")).thenReturn(1);

        assertTrue(dao.verifyUser("jperez", "10.0.0.1").isRelacionActiva());
    }

    @Test
    void siLaBaseNoRespondeVerifyUserNoLoDisfrazaDeUsuarioInexistente() {
        when(jdbc.queryForObject(anyString(), any(Object[].class), any(int[].class), any(RowMapper.class)))
                .thenThrow(new CannotGetJdbcConnectionException("sin conexión"));

        assertThrows(CannotGetJdbcConnectionException.class, () -> dao.verifyUser("jperez", "10.0.0.1"));
    }

    @Test
    void elUsuarioDelTokenDeUnExEmpleadoQuedaDeshabilitado() throws SQLException {
        prepararFilaB(0, 0);

        UserDetails u = dao.loadUserByUsername("jperez");

        assertFalse(u.isEnabled());
        assertTrue(u.isAccountNonLocked());
    }

    @Test
    void elUsuarioDelTokenDeUnBloqueadoPorUnAdministradorQuedaBloqueado() throws SQLException {
        prepararFilaB(1, 1);

        UserDetails u = dao.loadUserByUsername("jperez");

        assertTrue(u.isEnabled());
        assertFalse(u.isAccountNonLocked());
    }

    @Test
    void unBloqueoPorIntentosNoCierraLaSesionAbierta() throws SQLException {
        // Tres claves falsas de un tercero bloquean el login, pero no deben sacar al dueño que está trabajando.
        prepararFilaB(1, 0);

        UserDetails u = dao.loadUserByUsername("jperez");

        assertTrue(u.isEnabled());
        assertTrue(u.isAccountNonLocked());
    }

    @Test
    void siLaRamaBAunNoDevuelveLasColumnasNadieQuedaAfuera() throws SQLException {
        prepararFilaB(1, 0);
        when(fila.getInt("relacionActiva")).thenThrow(new SQLException("Invalid column name 'relacionActiva'."));

        UserDetails u = dao.loadUserByUsername("jperez");

        assertTrue(u.isEnabled());
        assertTrue(u.isAccountNonLocked());
    }

    @Test
    void conElSidDelTokenUnaSolaConsultaTraeTambienSiLaSesionSigueAbierta() throws SQLException {
        prepararFilaB(1, 0);
        when(fila.getInt("sesionCerrada")).thenReturn(1);
        when(fila.getString("motivoCierreSesion")).thenReturn("A");

        UsuarioToken u = dao.cargarParaToken("jperez", 33);

        assertTrue(u.isSesionCerrada());
        assertEquals("A", u.getMotivoCierreSesion());
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).queryForObject(sql.capture(), args.capture(), any(int[].class), any(RowMapper.class));
        assertTrue(sql.getValue().contains("@idSesion"));
        assertEquals(33L, args.getValue()[1]);
    }

    @Test
    void sinSidNoSeMandaLaSesionYNuncaFiguraCerrada() throws SQLException {
        // Login y tokens anteriores a sql/07: así no dependen de que el SP ya reciba @idSesion.
        prepararFilaB(1, 0);
        when(fila.getInt("sesionCerrada")).thenReturn(1);

        UsuarioToken u = dao.cargarParaToken("jperez", 0);

        assertFalse(u.isSesionCerrada());
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).queryForObject(sql.capture(), any(Object[].class), any(int[].class), any(RowMapper.class));
        assertFalse(sql.getValue().contains("@idSesion"));
    }

    private void prepararFilaB(int relacionActiva, int bloqueoAdministrador) throws SQLException {
        when(fila.getString(1)).thenReturn("jperez");
        when(fila.getString(2)).thenReturn("ROLE_ADM");
        when(fila.getString(3)).thenReturn("hash");
        when(fila.getInt("relacionActiva")).thenReturn(relacionActiva);
        when(fila.getInt("bloqueoAdministrador")).thenReturn(bloqueoAdministrador);
    }

    @Test
    void siElSpAunNoDevuelveLaColumnaSeAsumeActiva() throws SQLException {
        when(fila.getInt("relacionActiva")).thenThrow(new SQLException("Invalid column name 'relacionActiva'."));

        Login l = dao.verifyUser("jperez", "10.0.0.1");

        assertEquals(5, l.getCodUsuario());
        assertTrue(l.isRelacionActiva());
    }
}
