package bo.bosque.com.impexpap.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.RowMapper;

import bo.bosque.com.impexpap.dto.NotaRemisionDto;
import bo.bosque.com.impexpap.dto.PostergacionDto;
import bo.bosque.com.impexpap.dto.TransaccionBancariaDto;
import bo.bosque.com.impexpap.model.ChNotaRemision;
import bo.bosque.com.impexpap.model.ChPostergacion;
import bo.bosque.com.impexpap.model.ChTransaccionBancaria;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;

/**
 * Los tres DAO de los paneles contra un {@code SpHelper} simulado: lo que se prueba es el CONTRATO con los procedimientos (que
 * rama se llama y con que parametros, que son los nombres reales de los {@code p_abm_} y {@code p_list_}), la lectura por
 * posicion de las transacciones y el orden y la numeracion de las filas. El SQL real lo cubren los scripts de pruebas en
 * {@code BOSQUE2PRUEBA}.
 */
class ChSatelitesDaoTest {

    private SpHelper sp;
    private ChNotaRemisionDAO notas;
    private ChTransaccionBancariaDAO transacciones;
    private ChPostergacionDAO postergaciones;

    @BeforeEach
    void preparar() {
        sp = mock(SpHelper.class);
        notas = new ChNotaRemisionDAO(sp);
        transacciones = new ChTransaccionBancariaDAO(sp);
        postergaciones = new ChPostergacionDAO(sp);
    }

    private static Date d(int a, int m, int dia) {
        return java.sql.Date.valueOf(LocalDate.of(a, m, dia));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ArgumentCaptor<Map<String, Object>> mapa() {
        return (ArgumentCaptor) ArgumentCaptor.forClass(Map.class);
    }

    private static TreeSet<String> claves(Map<String, Object> m) {
        return new TreeSet<>(m.keySet());
    }

    // ------------------------------------------------------------------ notas de remision

    @Test
    @DisplayName("notas: listar llama p_list_NotaRemision 'L' con SOLO @codCheque (el 0 seria un filtro real) y numera las filas 1..n en el orden del SP")
    void notasListar() {
        NotaRemisionDto a = new NotaRemisionDto();
        a.setNotaRemision("262211881");
        NotaRemisionDto b = new NotaRemisionDto();
        b.setNotaRemision("262211882");
        when(sp.ejecutarListado(eq("p_list_NotaRemision"), anyMap(), eq("L"), eq(NotaRemisionDto.class))).thenReturn(new ArrayList<>(Arrays.asList(a, b)));

        List<NotaRemisionDto> r = notas.listarPorCheque(18129);

        assertEquals(Arrays.asList(1, 2), Arrays.asList(r.get(0).getFila(), r.get(1).getFila()));
        assertEquals("262211881", r.get(0).getNotaRemision());
        ArgumentCaptor<Map<String, Object>> c = mapa();
        verify(sp).ejecutarListado(eq("p_list_NotaRemision"), c.capture(), eq("L"), eq(NotaRemisionDto.class));
        assertEquals(Collections.singletonMap("codCheque", 18129), c.getValue());
    }

    @Test
    @DisplayName("notas: registrar llama p_abm_NotaRemision 'I' con los parametros reales (codCheque, notaRemision, nroFactura, fechaFactura, audUsuario)")
    void notasRegistrar() {
        RespuestaSp ok = new RespuestaSp(0, "", 1L);
        when(sp.ejecutarAbmMap(eq("p_abm_NotaRemision"), anyMap(), eq("I"))).thenReturn(ok);
        ChNotaRemision n = new ChNotaRemision();
        n.setCodCheque(18129);
        n.setNotaRemision("262211881");
        n.setNroFactura(1856);
        n.setFechaFactura(d(2026, 8, 31));
        n.setAudUsuario(7);

        assertSame(ok, notas.registrar(n));

        ArgumentCaptor<Map<String, Object>> c = mapa();
        verify(sp).ejecutarAbmMap(eq("p_abm_NotaRemision"), c.capture(), eq("I"));
        assertEquals(new TreeSet<>(Arrays.asList("codCheque", "notaRemision", "nroFactura", "fechaFactura", "audUsuario")), claves(c.getValue()));
        assertEquals("262211881", c.getValue().get("notaRemision"));
        assertEquals(7, c.getValue().get("audUsuario"));
    }

    @Test
    @DisplayName("notas: eliminar llama p_abm_NotaRemision 'D' por (codCheque, notaRemision) con el usuario")
    void notasEliminar() {
        notas.eliminar(18129, " 17330357", 7);

        ArgumentCaptor<Map<String, Object>> c = mapa();
        verify(sp).ejecutarAbmMap(eq("p_abm_NotaRemision"), c.capture(), eq("D"));
        assertEquals(new TreeSet<>(Arrays.asList("codCheque", "notaRemision", "audUsuario")), claves(c.getValue()));
        assertEquals(" 17330357", c.getValue().get("notaRemision"), "el texto tal cual, con su espacio inicial");
    }

    // ------------------------------------------------------------------ transacciones

    @Test
    @DisplayName("transacciones: listar llama p_list_ChTransaccionBancaria 'A' con @codCheque y lee POR POSICION las 6 columnas; numera 1..n")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void transaccionesListar() throws Exception {
        TransaccionBancariaDto fila = new TransaccionBancariaDto();
        when(sp.ejecutarListadoPorPosicion(eq("p_list_ChTransaccionBancaria"), anyMap(), eq("A"), any(RowMapper.class)))
                .thenReturn(new ArrayList<>(Arrays.asList(fila, new TransaccionBancariaDto())));

        List<TransaccionBancariaDto> r = transacciones.listarPorCheque(18129);

        assertEquals(Arrays.asList(1, 2), Arrays.asList(r.get(0).getFila(), r.get(1).getFila()));
        ArgumentCaptor<Map<String, Object>> c = mapa();
        ArgumentCaptor<RowMapper> m = ArgumentCaptor.forClass(RowMapper.class);
        verify(sp).ejecutarListadoPorPosicion(eq("p_list_ChTransaccionBancaria"), c.capture(), eq("A"), m.capture());
        assertEquals(Collections.singletonMap("codCheque", 18129), c.getValue());

        // el mapeador por posicion: 1 codCheque, 2 nroTransaccion, 3 codBanco, 4 fechaTransaccion, 5 datoBanco
        ResultSet rs = mock(ResultSet.class);
        when(rs.getInt(1)).thenReturn(18129);
        when(rs.getString(2)).thenReturn("TT262318S5JD");
        when(rs.getInt(3)).thenReturn(2);
        when(rs.getDate(4)).thenReturn(new java.sql.Date(d(2026, 8, 19).getTime()));
        when(rs.getString(5)).thenReturn("BANCO UNION");
        when(rs.wasNull()).thenReturn(false);
        TransaccionBancariaDto leida = (TransaccionBancariaDto) m.getValue().mapRow(rs, 0);
        assertEquals(Integer.valueOf(18129), leida.getCodCheque());
        assertEquals("TT262318S5JD", leida.getNroTransaccion());
        assertEquals(Integer.valueOf(2), leida.getCodBanco());
        assertEquals(d(2026, 8, 19), leida.getFechaTransaccion());
        assertEquals("BANCO UNION", leida.getDatoBanco());
        assertNull(leida.getAudUsuario(), "la rama 'A' no trae audUsuario ni audFecha");
        assertNull(leida.getAudFecha());
    }

    @Test
    @DisplayName("transacciones: registrar y eliminar llaman p_abm_ChTransaccionBancaria con los parametros reales")
    void transaccionesEscribir() {
        ChTransaccionBancaria t = new ChTransaccionBancaria();
        t.setCodCheque(18129);
        t.setNroTransaccion("TT262318S5JD");
        t.setCodBanco(2);
        t.setFechaTransaccion(d(2026, 8, 19));
        t.setAudUsuario(7);
        transacciones.registrar(t);
        transacciones.eliminar(18129, "TT262318S5JD", 7);

        ArgumentCaptor<Map<String, Object>> alta = mapa();
        verify(sp).ejecutarAbmMap(eq("p_abm_ChTransaccionBancaria"), alta.capture(), eq("I"));
        assertEquals(new TreeSet<>(Arrays.asList("codCheque", "nroTransaccion", "codBanco", "fechaTransaccion", "audUsuario")), claves(alta.getValue()));
        ArgumentCaptor<Map<String, Object>> baja = mapa();
        verify(sp).ejecutarAbmMap(eq("p_abm_ChTransaccionBancaria"), baja.capture(), eq("D"));
        assertEquals(new TreeSet<>(Arrays.asList("codCheque", "nroTransaccion", "audUsuario")), claves(baja.getValue()));
    }

    // ------------------------------------------------------------------ postergaciones

    private static PostergacionDto p(int cod) {
        PostergacionDto x = new PostergacionDto();
        x.setCodPostergacion(cod);
        return x;
    }

    @Test
    @DisplayName("postergaciones: listar llama p_list_ChPostergacion 'L' por @codCheque, ordena por codPostergacion (el SP no ordena) y numera 1..n")
    void postergacionesListar() {
        when(sp.ejecutarListado(eq("p_list_ChPostergacion"), anyMap(), eq("L"), eq(PostergacionDto.class)))
                .thenReturn(Arrays.asList(p(40), p(38), p(39)));

        List<PostergacionDto> r = postergaciones.listarPorCheque(8981);

        assertEquals(Arrays.asList(38, 39, 40), Arrays.asList(r.get(0).getCodPostergacion(), r.get(1).getCodPostergacion(), r.get(2).getCodPostergacion()));
        assertEquals(Arrays.asList(1, 2, 3), Arrays.asList(r.get(0).getFila(), r.get(1).getFila(), r.get(2).getFila()));
        ArgumentCaptor<Map<String, Object>> c = mapa();
        verify(sp).ejecutarListado(eq("p_list_ChPostergacion"), c.capture(), eq("L"), eq(PostergacionDto.class));
        assertEquals(Collections.singletonMap("codCheque", 8981), c.getValue());
    }

    @Test
    @DisplayName("postergaciones: obtener llama 'L' por @codPostergacion; sin filas es null; un codigo <= 0 ni consulta")
    void postergacionesObtener() {
        ChPostergacion fila = new ChPostergacion();
        fila.setCodPostergacion(40);
        when(sp.ejecutarListado(eq("p_list_ChPostergacion"), anyMap(), eq("L"), eq(ChPostergacion.class)))
                .thenReturn(Collections.singletonList(fila), Collections.<ChPostergacion>emptyList());

        assertSame(fila, postergaciones.obtener(40));
        assertNull(postergaciones.obtener(41));
        assertNull(postergaciones.obtener(0));
        assertNull(postergaciones.obtener(-1));

        ArgumentCaptor<Map<String, Object>> c = mapa();
        verify(sp, org.mockito.Mockito.times(2)).ejecutarListado(eq("p_list_ChPostergacion"), c.capture(), eq("L"), eq(ChPostergacion.class));
        assertEquals(Collections.singletonMap("codPostergacion", 40), c.getAllValues().get(0));
        assertEquals(Collections.singletonMap("codPostergacion", 41), c.getAllValues().get(1));   // el 0 y el -1 ni consultaron
    }

    @Test
    @DisplayName("postergaciones: registrar llama 'I' SIN codPostergacion ni nombreArchivo (el SP escribe '' a proposito) y devuelve el id generado")
    void postergacionesRegistrar() {
        RespuestaSp ok = new RespuestaSp(0, "", 41L);
        when(sp.ejecutarAbmMap(eq("p_abm_ChPostergacion"), anyMap(), eq("I"))).thenReturn(ok);
        ChPostergacion x = new ChPostergacion();
        x.setCodCheque(8981);
        x.setFecha(d(2025, 10, 30));
        x.setObservacion("cambio de cheque 277");
        x.setNombreArchivo("ignorado.pdf");
        x.setAudUsuario(7);

        assertSame(ok, postergaciones.registrar(x));

        ArgumentCaptor<Map<String, Object>> c = mapa();
        verify(sp).ejecutarAbmMap(eq("p_abm_ChPostergacion"), c.capture(), eq("I"));
        assertEquals(new TreeSet<>(Arrays.asList("codCheque", "fecha", "observacion", "audUsuario")), claves(c.getValue()));
    }

    @Test
    @DisplayName("postergaciones: eliminar llama 'D' por @codPostergacion con el usuario")
    void postergacionesEliminar() {
        postergaciones.eliminar(40, 7);

        ArgumentCaptor<Map<String, Object>> c = mapa();
        verify(sp).ejecutarAbmMap(eq("p_abm_ChPostergacion"), c.capture(), eq("D"));
        assertEquals(new TreeSet<>(Arrays.asList("codPostergacion", "audUsuario")), claves(c.getValue()));
    }
}
