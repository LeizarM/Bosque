package bo.bosque.com.impexpap.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.RowMapper;

import bo.bosque.com.impexpap.dto.ChequePendienteFilaDto;
import bo.bosque.com.impexpap.dto.VerificacionDepositoFilaDto;
import bo.bosque.com.impexpap.model.ChVerificacionDeposito;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;

/**
 * El DAO de verificaciones sin base de datos: que llame a la rama correcta del procedimiento con los parametros
 * correctos (la confusion {@code @estad} / {@code @estado} es la trampa de este SP) y que lea las 13 columnas por
 * posicion.
 */
class ChVerificacionDepositoDAOTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 3);

    private SpHelper sp;
    private ChVerificacionDepositoDAO dao;

    @BeforeEach
    void preparar() {
        sp = mock(SpHelper.class);
        dao = new ChVerificacionDepositoDAO(sp);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Map<String, Object> paramsDeLaLectura(String rama) {
        ArgumentCaptor<Map> cap = ArgumentCaptor.forClass(Map.class);
        verify(sp).ejecutarListadoPorPosicion(eq("p_list_VerificacionDeposito"), cap.capture(), eq(rama), any(RowMapper.class));
        return cap.getValue();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Map<String, Object> paramsDeLaEscritura(String accion) {
        ArgumentCaptor<Map> cap = ArgumentCaptor.forClass(Map.class);
        verify(sp).ejecutarAbmMap(eq("p_abm_VerificacionDeposito"), cap.capture(), eq(accion));
        return cap.getValue();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void lecturaDevuelve(List filas) {
        when(sp.ejecutarListadoPorPosicion(anyString(), anyMap(), anyString(), any(RowMapper.class))).thenReturn(filas);
    }

    // ------------------------------------------------------------------ lecturas

    @Test
    @DisplayName("listar: rama A con SOLO @fechaBanco (un java.sql.Date del dia); sin fecha, sin parametros")
    void listar() {
        lecturaDevuelve(new ArrayList<>());
        dao.listar(HOY);
        Map<String, Object> p = paramsDeLaLectura("A");
        assertEquals(Collections.singleton("fechaBanco"), p.keySet());
        assertTrue(p.get("fechaBanco") instanceof java.sql.Date, "un java.sql.Date: el dia exacto, sin depender de la zona de la JVM");
        assertEquals(HOY, ((java.sql.Date) p.get("fechaBanco")).toLocalDate());
    }

    @Test
    @DisplayName("listar sin fecha: rama A sin ningun parametro (todas)")
    void listarTodas() {
        lecturaDevuelve(new ArrayList<>());
        dao.listar(null);
        assertTrue(paramsDeLaLectura("A").isEmpty());
    }

    @Test
    @DisplayName("obtener: rama A con @codvd; devuelve null si no hay fila o el id no es valido")
    void obtener() {
        lecturaDevuelve(new ArrayList<>());
        assertNull(dao.obtener(55));
        assertEquals(Collections.singleton("codvd"), paramsDeLaLectura("A").keySet());
        assertNull(dao.obtener(0), "no consulta con un id invalido");
    }

    @Test
    @DisplayName("sinRegularizarDelDia: rama C con @fechaBanco (la fecha de COBRANZA) y @estado (el del CHEQUE), nunca @estad")
    void sinRegularizarDelDia() {
        lecturaDevuelve(new ArrayList<>());
        dao.sinRegularizarDelDia(HOY, "PEN");
        Map<String, Object> p = paramsDeLaLectura("C");
        assertEquals(new TreeSet<>(Arrays.asList("fechaBanco", "estado")), new TreeSet<>(p.keySet()));
        assertEquals("PEN", p.get("estado"));
        assertFalse(p.containsKey("estad"), "@estad es el estado de la VERIFICACION y aqui no se manda");
        assertEquals(HOY, ((java.sql.Date) p.get("fechaBanco")).toLocalDate());
    }

    @Test
    @DisplayName("sinRegularizarHastaHoy: rama D con solo @estado; sin estado, sin parametros")
    void sinRegularizarHastaHoy() {
        lecturaDevuelve(new ArrayList<>());
        dao.sinRegularizarHastaHoy("CER");
        assertEquals(Collections.singletonMap("estado", "CER"), paramsDeLaLectura("D"));
    }

    @Test
    @DisplayName("sinRegularizarHastaHoy sin estado (todos): rama D sin parametros")
    void sinRegularizarHastaHoyTodos() {
        lecturaDevuelve(new ArrayList<>());
        dao.sinRegularizarHastaHoy(null);
        assertTrue(paramsDeLaLectura("D").isEmpty());
    }

    @Test
    @DisplayName("sinRegularizarDeUnCheque: rama C con solo @codCheque; null si no devuelve fila")
    void sinRegularizarDeUnCheque() {
        lecturaDevuelve(new ArrayList<>());
        assertNull(dao.sinRegularizarDeUnCheque(321L));
        assertEquals(Collections.singletonMap("codCheque", 321L), paramsDeLaLectura("C"));
    }

    @Test
    @DisplayName("tieneVerificacionValida: rama A con @estad='Y' (la de la verificacion) y @codCheque; verdadero si hay alguna fila")
    void tieneVerificacionValida() {
        lecturaDevuelve(Arrays.asList(55));
        assertTrue(dao.tieneVerificacionValida(321L));
        Map<String, Object> p = paramsDeLaLectura("A");
        assertEquals("Y", p.get("estad"));
        assertEquals(321L, p.get("codCheque"));
        assertEquals(2, p.size());
        assertFalse(p.containsKey("estado"));
    }

    @Test
    @DisplayName("tieneVerificacionValida: sin filas es falso")
    void noTieneVerificacionValida() {
        lecturaDevuelve(new ArrayList<>());
        assertFalse(dao.tieneVerificacionValida(321L));
    }

    // ------------------------------------------------------------------ escrituras

    @Test
    @DisplayName("registrar: I con codCheque, codBanco, fechaBanco (java.sql.Date), observacion, estado y audUsuario; sin codvd")
    void registrar() {
        when(sp.ejecutarAbmMap(anyString(), anyMap(), anyString())).thenReturn(new RespuestaSp(0, "", 77L));
        ChVerificacionDeposito v = new ChVerificacionDeposito();
        v.setCodCheque(321L);
        v.setCodBanco(2L);
        v.setFechaBanco(HOY);
        v.setObservacion("");
        v.setEstado("Y");
        v.setAudUsuario(7);

        assertEquals(77L, dao.registrar(v).getIdGenerado());

        Map<String, Object> p = paramsDeLaEscritura("I");
        assertEquals(new TreeSet<>(Arrays.asList("codCheque", "codBanco", "fechaBanco", "observacion", "estado", "audUsuario")),
                new TreeSet<>(p.keySet()));
        assertEquals("", p.get("observacion"), "vacio, no NULL: el legacy no guarda NULL");
        assertEquals("Y", p.get("estado"));
        assertEquals(HOY, ((java.sql.Date) p.get("fechaBanco")).toLocalDate());
        assertEquals(7, p.get("audUsuario"));
    }

    @Test
    @DisplayName("actualizar: U con codvd, codBanco, fechaBanco, observacion, estado y audUsuario; NO manda codCheque (el SP no lo cambia)")
    void actualizar() {
        when(sp.ejecutarAbmMap(anyString(), anyMap(), anyString())).thenReturn(new RespuestaSp(0, "", 55L));
        ChVerificacionDeposito v = new ChVerificacionDeposito();
        v.setCodvd(55);
        v.setCodCheque(321L);
        v.setCodBanco(2L);
        v.setFechaBanco(HOY);
        v.setObservacion("corregida");
        v.setEstado("N");
        v.setAudUsuario(7);

        dao.actualizar(v);

        Map<String, Object> p = paramsDeLaEscritura("U");
        assertEquals(new TreeSet<>(Arrays.asList("codvd", "codBanco", "fechaBanco", "observacion", "estado", "audUsuario")),
                new TreeSet<>(p.keySet()));
        assertEquals(55, p.get("codvd"));
        assertEquals("N", p.get("estado"));
    }

    // ------------------------------------------------------------------ lectura por posicion

    @Test
    @DisplayName("la rama A se lee por posicion: nroCheque como TEXTO, monto double, las dos fechas como dia")
    void mapaDeLaRamaA() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getInt(1)).thenReturn(55);
        when(rs.getLong(2)).thenReturn(321L);
        when(rs.getLong(3)).thenReturn(2L);
        when(rs.getDate(4)).thenReturn(java.sql.Date.valueOf(HOY));
        when(rs.getString(5)).thenReturn("Deposito OK");
        when(rs.getString(6)).thenReturn("Union BU");
        when(rs.getString(7)).thenReturn("12-345");
        when(rs.getDouble(8)).thenReturn(1500.5);
        when(rs.getString(9)).thenReturn("Mercantil Santa Cruz");
        when(rs.getString(10)).thenReturn("PENDIENTE");
        when(rs.getDate(11)).thenReturn(java.sql.Date.valueOf(HOY.plusDays(2)));
        when(rs.getString(12)).thenReturn("Y");
        when(rs.getString(13)).thenReturn("Valido");

        VerificacionDepositoFilaDto f = ChVerificacionDepositoDAO.MAPA_VERIFICACION.mapRow(rs, 0);

        assertEquals(55, f.getCodvd().intValue());
        assertEquals(321L, f.getCodCheque().longValue());
        assertEquals(2L, f.getCodBanco().longValue());
        assertEquals(HOY, f.getFechaBanco());
        assertEquals("Deposito OK", f.getObservacion());
        assertEquals("Union BU", f.getDatoBanco());
        assertEquals("12-345", f.getNroCheque());
        assertEquals(1500.5, f.getMontoCheque(), 0.0001);
        assertEquals("Mercantil Santa Cruz", f.getDatoBancoCheque());
        assertEquals("PENDIENTE", f.getDatoEstadoCheque());
        assertEquals(HOY.plusDays(2), f.getFechaCobrarCheque());
        assertEquals("Y", f.getEstado());
        assertEquals("Valido", f.getDatoEstado());
        assertNull(f.getAudUsuario(), "la rama A no trae auditoria");
        verify(rs, org.mockito.Mockito.never()).getInt(7);   // el numero de cheque NO se lee como entero
    }

    @Test
    @DisplayName("las ramas C y D se leen por posicion: solo las columnas del cheque; las fechas y el monto nulos no revientan")
    void mapaDeLasRamasCyD() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong(2)).thenReturn(321L);
        when(rs.getLong(3)).thenReturn(2L);
        when(rs.getString(7)).thenReturn("5551");
        when(rs.getDouble(8)).thenReturn(0.0);
        when(rs.wasNull()).thenReturn(false, false, true, true);   // codCheque, codBanco, monto (nulo)... ver abajo
        when(rs.getString(9)).thenReturn("Union BU");
        when(rs.getString(10)).thenReturn("CERRADO");
        when(rs.getDate(11)).thenReturn(null);

        ChequePendienteFilaDto f = ChVerificacionDepositoDAO.MAPA_PENDIENTE.mapRow(rs, 0);

        assertEquals(321L, f.getCodCheque().longValue());
        assertEquals(2L, f.getCodBanco().longValue(), "el banco DEL CHEQUE (columna 3, elCodBanco)");
        assertEquals("5551", f.getNroCheque());
        assertNull(f.getMontoCheque(), "un monto nulo llega como null");
        assertEquals("CERRADO", f.getDatoEstadoCheque());
        assertNull(f.getFechaCobrarCheque());
    }
}
