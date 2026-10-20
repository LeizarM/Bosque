package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.TcpeLoteDto;
import bo.bosque.com.impexpap.model.TcpeRolExamen;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.BeanPropertyRowMapper;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Types;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Cómo llegan {@code date} y {@code time(7)} por jTDS con {@code BeanPropertyRowMapper}: jTDS los da como texto y su
 * {@code getObject(int, Class)} lanza {@code AbstractMethodError}, así que Spring cae a {@code getDate}. La prueba
 * fija ese camino con un {@code ResultSet} que imita a jTDS; la hora queda en {@code HH:mm:ss}.
 */
class TcpeMapeoJtdsTest {

    private static ResultSet filaComoJtds() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        ResultSetMetaData md = mock(ResultSetMetaData.class);
        when(rs.getMetaData()).thenReturn(md);
        when(md.getColumnCount()).thenReturn(3);
        when(md.getColumnLabel(1)).thenReturn("codRolExamen");
        when(md.getColumnLabel(2)).thenReturn("fecha");
        when(md.getColumnLabel(3)).thenReturn("horaInicio");
        when(md.getColumnType(anyInt())).thenReturn(Types.VARCHAR);

        when(rs.getLong(1)).thenReturn(15L);
        when(rs.getObject(eq(2), eq(LocalDate.class))).thenThrow(new AbstractMethodError());
        when(rs.getDate(2)).thenReturn(java.sql.Date.valueOf("2026-10-19"));
        when(rs.getString(3)).thenReturn("08:30:00.0000000");
        return rs;
    }

    @Test
    @DisplayName("modelo: date a LocalDate y time(7) a HH:mm:ss")
    void modelo() throws Exception {
        TcpeRolExamen r = BeanPropertyRowMapper.newInstance(TcpeRolExamen.class).mapRow(filaComoJtds(), 0);
        assertEquals(15L, r.getCodRolExamen());
        assertEquals(LocalDate.of(2026, 10, 19), r.getFecha());
        assertEquals("08:30:00", r.getHoraInicio());
    }

    @Test
    @DisplayName("DTO de proceso: mismo mapeo")
    void dto() throws Exception {
        TcpeLoteDto l = BeanPropertyRowMapper.newInstance(TcpeLoteDto.class).mapRow(filaComoJtds(), 0);
        assertEquals(LocalDate.of(2026, 10, 19), l.getFecha());
        assertEquals("08:30:00", l.getHoraInicio());
    }
}
