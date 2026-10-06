package bo.bosque.com.impexpap.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.BeanPropertyRowMapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import bo.bosque.com.impexpap.model.ChNotaRemision;
import bo.bosque.com.impexpap.model.ChPostergacion;
import bo.bosque.com.impexpap.model.ChTransaccionBancaria;

/**
 * Los nombres y formatos de los tres paneles que el frontend recibe y manda, y que las columnas de las tablas llegan a los
 * modelos espejo. Si fallan, la pantalla lee campos que no existen: el error seria silencioso (un campo en null).
 */
class PanelesChequeJsonTest {

    private static final ZoneId LA_PAZ = ZoneId.of("America/La_Paz");
    private final ObjectMapper json = new ObjectMapper().setTimeZone(TimeZone.getTimeZone(LA_PAZ));

    private static Date dia(int a, int m, int d) {
        return Date.from(LocalDate.of(a, m, d).atStartOfDay(LA_PAZ).toInstant());
    }

    private static LocalDate comoDia(Date d) {
        return d.toInstant().atZone(LA_PAZ).toLocalDate();
    }

    private static Set<String> claves(JsonNode n) {
        Set<String> k = new TreeSet<>();
        for (Iterator<String> it = n.fieldNames(); it.hasNext(); ) k.add(it.next());
        return k;
    }

    private static Set<String> set(String... s) {
        return new TreeSet<>(Arrays.asList(s));
    }

    @Test
    @DisplayName("la nota se serializa con los nombres exactos de la tabla, mas fila; fechaFactura yyyy-MM-dd y audFecha con hora")
    void notaSerializa() throws Exception {
        NotaRemisionDto n = new NotaRemisionDto();
        n.setFila(1);
        n.setCodCheque(9731);
        n.setNotaRemision("262211881");
        n.setNroFactura(1856);
        n.setFechaFactura(dia(2026, 8, 31));
        n.setAudUsuario(66);
        n.setAudFecha(Date.from(LocalDateTime.of(2026, 9, 1, 9, 5, 17).atZone(LA_PAZ).toInstant()));

        JsonNode j = json.readTree(json.writeValueAsString(n));

        assertEquals(set("fila", "codCheque", "notaRemision", "nroFactura", "fechaFactura", "audUsuario", "audFecha"), claves(j));
        assertEquals("2026-08-31", j.get("fechaFactura").asText());
        assertEquals("2026-09-01T09:05:17", j.get("audFecha").asText());
    }

    @Test
    @DisplayName("la transaccion no manda audUsuario ni audFecha (NON_NULL) pero si fila y datoBanco")
    void transaccionSerializa() throws Exception {
        TransaccionBancariaDto t = new TransaccionBancariaDto();
        t.setFila(1);
        t.setCodCheque(9731);
        t.setNroTransaccion("14910211612");
        t.setCodBanco(7);
        t.setFechaTransaccion(dia(2026, 8, 31));
        t.setDatoBanco("BANCO GANADERO");

        JsonNode j = json.readTree(json.writeValueAsString(t));

        assertEquals(set("fila", "codCheque", "nroTransaccion", "codBanco", "fechaTransaccion", "datoBanco"), claves(j));
        assertEquals("2026-08-31", j.get("fechaTransaccion").asText());
    }

    @Test
    @DisplayName("la postergacion se serializa con fila y tienePdf (true, false o null, siempre presente)")
    void postergacionSerializa() throws Exception {
        PostergacionDto p = new PostergacionDto();
        p.setFila(1);
        p.setCodPostergacion(40);
        p.setCodCheque(8981);
        p.setFecha(dia(2025, 10, 30));
        p.setObservacion("cambio de cheque 277");
        p.setNombreArchivo("");
        p.setAudUsuario(30);

        JsonNode j = json.readTree(json.writeValueAsString(p));

        assertEquals(set("fila", "codPostergacion", "codCheque", "fecha", "observacion", "nombreArchivo", "audUsuario", "audFecha", "tienePdf"),
                claves(j));
        assertTrue(j.get("tienePdf").isNull());
        p.setTienePdf(true);
        assertTrue(json.readTree(json.writeValueAsString(p)).get("tienePdf").asBoolean());
        p.setTienePdf(false);
        assertFalse(json.readTree(json.writeValueAsString(p)).get("tienePdf").asBoolean());
        assertEquals("2025-10-30", j.get("fecha").asText());
    }

    @Test
    @DisplayName("los cuerpos se deserializan con sus fechas yyyy-MM-dd, y un audUsuario que el cliente mande se ignora (no existe en el cuerpo)")
    void cuerposDeserializan() throws Exception {
        NotaRemisionRequest n = json.readValue(
                "{\"codCheque\":1,\"notaRemision\":\"262211881\",\"nroFactura\":1856,\"fechaFactura\":\"2026-08-31\"}", NotaRemisionRequest.class);
        assertEquals(LocalDate.of(2026, 8, 31), comoDia(n.getFechaFactura()));
        assertEquals(Integer.valueOf(1856), n.getNroFactura());

        TransaccionBancariaRequest t = json.readValue(
                "{\"codCheque\":1,\"nroTransaccion\":\"TT262318S5JD\",\"codBanco\":2,\"fechaTransaccion\":\"2026-08-19\"}", TransaccionBancariaRequest.class);
        assertEquals(LocalDate.of(2026, 8, 19), comoDia(t.getFechaTransaccion()));

        PostergacionRequest p = json.readValue("{\"codCheque\":1,\"codPostergacion\":40,\"fecha\":\"2025-10-30\",\"observacion\":\"x\"}",
                PostergacionRequest.class);
        assertEquals(LocalDate.of(2025, 10, 30), comoDia(p.getFecha()));
        assertEquals(Integer.valueOf(40), p.getCodPostergacion());

        assertNull(json.readValue("{}", NotaRemisionRequest.class).getFechaFactura());
        assertNull(json.readValue("{}", ChequeReferenciaRequest.class).getCodCheque());
        assertNull(json.readValue("{}", PostergacionPdfRequest.class).getCodPostergacion());
        assertEquals(Integer.valueOf(40), json.readValue("{\"codPostergacion\":40}", PostergacionPdfRequest.class).getCodPostergacion());
    }

    private static ResultSetMetaData columnas(String... nombres) throws Exception {
        ResultSetMetaData md = mock(ResultSetMetaData.class);
        when(md.getColumnCount()).thenReturn(nombres.length);
        for (int i = 0; i < nombres.length; i++) when(md.getColumnLabel(i + 1)).thenReturn(nombres[i]);
        return md;
    }

    @Test
    @DisplayName("BeanPropertyRowMapper llena ChNotaRemision / NotaRemisionDto desde las columnas de p_list_NotaRemision 'L', con los NULL de la tabla")
    void mapeoDeNotas() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        ResultSetMetaData md = columnas("codCheque", "notaRemision", "nroFactura", "fechaFactura", "audUsuario", "audFecha");
        when(rs.getMetaData()).thenReturn(md);
        when(rs.getInt(1)).thenReturn(9731);
        when(rs.getString(2)).thenReturn("262211881");
        when(rs.getInt(3)).thenReturn(0);
        when(rs.wasNull()).thenReturn(false, true, false);   // se pregunta por cada Integer, en orden: codCheque, nroFactura (NULL), audUsuario
        when(rs.getTimestamp(4)).thenReturn(Timestamp.valueOf("2026-08-31 00:00:00"));
        when(rs.getInt(5)).thenReturn(66);
        when(rs.getTimestamp(6)).thenReturn(Timestamp.valueOf("2026-09-01 09:05:17"));

        NotaRemisionDto n = BeanPropertyRowMapper.newInstance(NotaRemisionDto.class).mapRow(rs, 0);

        assertEquals(Integer.valueOf(9731), n.getCodCheque());
        assertEquals("262211881", n.getNotaRemision());
        assertNull(n.getNroFactura(), "una columna NULL llega como null: el modelo usa wrappers");
        assertEquals(LocalDate.of(2026, 8, 31), new java.sql.Date(n.getFechaFactura().getTime()).toLocalDate());
        assertEquals(Integer.valueOf(66), n.getAudUsuario());
        assertEquals(LocalDateTime.of(2026, 9, 1, 9, 5, 17), new Timestamp(n.getAudFecha().getTime()).toLocalDateTime());
        assertTrue(ChNotaRemision.class.isAssignableFrom(NotaRemisionDto.class));
    }

    @Test
    @DisplayName("BeanPropertyRowMapper llena ChPostergacion / PostergacionDto desde las columnas de p_list_ChPostergacion 'L'")
    void mapeoDePostergaciones() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        ResultSetMetaData md = columnas("codPostergacion", "codCheque", "fecha", "observacion", "nombreArchivo", "audUsuario", "audFecha");
        when(rs.getMetaData()).thenReturn(md);
        when(rs.getInt(1)).thenReturn(40);
        when(rs.getInt(2)).thenReturn(8981);
        when(rs.getTimestamp(3)).thenReturn(Timestamp.valueOf("2025-10-30 00:00:00"));
        when(rs.getString(4)).thenReturn("cambio de cheque 277");
        when(rs.getString(5)).thenReturn("");
        when(rs.getInt(6)).thenReturn(30);
        when(rs.getTimestamp(7)).thenReturn(Timestamp.valueOf("2025-10-30 16:52:19"));
        when(rs.wasNull()).thenReturn(false);

        ChPostergacion p = BeanPropertyRowMapper.newInstance(ChPostergacion.class).mapRow(rs, 0);
        PostergacionDto dto = BeanPropertyRowMapper.newInstance(PostergacionDto.class).mapRow(rs, 0);

        assertEquals(Integer.valueOf(40), p.getCodPostergacion());
        assertEquals(Integer.valueOf(8981), p.getCodCheque());
        assertEquals("cambio de cheque 277", p.getObservacion());
        assertEquals("", p.getNombreArchivo());
        assertEquals(LocalDate.of(2025, 10, 30), new java.sql.Date(p.getFecha().getTime()).toLocalDate());
        assertEquals(Integer.valueOf(40), dto.getCodPostergacion());
        assertNull(dto.getTienePdf(), "lo pone el servicio, no la consulta");
        assertNull(dto.getFila(), "lo pone el DAO");
    }

    @Test
    @DisplayName("el modelo de la transaccion espeja las 6 columnas de la tabla (los nombres exactos)")
    void modeloDeTransaccion() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        ResultSetMetaData md = columnas("codCheque", "nroTransaccion", "codBanco", "fechaTransaccion", "audUsuario", "audFecha");
        when(rs.getMetaData()).thenReturn(md);
        when(rs.getInt(1)).thenReturn(9731);
        when(rs.getString(2)).thenReturn("14910211612");
        when(rs.getInt(3)).thenReturn(7);
        when(rs.getTimestamp(4)).thenReturn(Timestamp.valueOf("2026-08-31 00:00:00"));
        when(rs.getInt(5)).thenReturn(66);
        when(rs.getTimestamp(6)).thenReturn(Timestamp.valueOf("2026-09-01 09:06:35"));
        when(rs.wasNull()).thenReturn(false);

        ChTransaccionBancaria t = BeanPropertyRowMapper.newInstance(ChTransaccionBancaria.class).mapRow(rs, 0);

        assertEquals(Integer.valueOf(9731), t.getCodCheque());
        assertEquals("14910211612", t.getNroTransaccion());
        assertEquals(Integer.valueOf(7), t.getCodBanco());
        assertEquals(Integer.valueOf(66), t.getAudUsuario());
        assertEquals(LocalDate.of(2026, 8, 31), new java.sql.Date(t.getFechaTransaccion().getTime()).toLocalDate());
    }
}
