package bo.bosque.com.impexpap.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Date;
import java.util.TimeZone;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.BeanPropertyRowMapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import bo.bosque.com.impexpap.model.ChCheque;

/**
 * Los nombres y formatos que el frontend recibe y manda. Si fallan, la pantalla lee campos que no existen:
 * el error seria silencioso (un campo en null), por eso se prueban.
 *
 * <p>El caso delicado es {@code aOrdenDe}: con el getter que genera Lombok ({@code getAOrdenDe}) Jackson
 * lo serializa como {@code aordenDe}.
 */
class ChequeJsonTest {

    private final ObjectMapper json = new ObjectMapper().setTimeZone(TimeZone.getTimeZone("America/La_Paz"));

    private static Date d(int a, int m, int dia) {
        return java.sql.Date.valueOf(LocalDate.of(a, m, dia));
    }

    @Test
    @DisplayName("la fila de la grilla se serializa con aOrdenDe (no aordenDe) y con fechas yyyy-MM-dd")
    void serializacion() throws Exception {
        ChequeFilaDto f = new ChequeFilaDto();
        f.setCodCheque(7);
        f.setaOrdenDe("IMPEXPAP SRL");
        f.setNrocheque("5551");
        f.setFechaCheque(d(2026, 10, 1));
        f.setFechaCobrar(d(2026, 10, 15));
        f.setFechaRecepcion(d(2026, 10, 2));
        f.setAudFecha("2026-08-31 14:54:51.5670000 +00:00");
        f.setDescTipo(null);

        JsonNode n = json.readTree(json.writeValueAsString(f));

        assertEquals("IMPEXPAP SRL", n.get("aOrdenDe").asText());
        assertFalse(n.has("aordenDe"), "el nombre mal derivado por Jackson no debe existir");
        assertEquals("5551", n.get("nrocheque").asText());
        assertEquals("2026-10-01", n.get("fechaCheque").asText());
        assertEquals("2026-10-15", n.get("fechaCobrar").asText());
        assertEquals("2026-10-02", n.get("fechaRecepcion").asText());
        assertEquals("2026-08-31 14:54:51.5670000 +00:00", n.get("audFecha").asText(), "datetimeoffset va como texto");
        assertTrue(n.has("descTipo"));
        assertTrue(n.get("descTipo").isNull(), "descTipo puede ser null (tipo corrupto) y llega como null");
    }

    @Test
    @DisplayName("el cuerpo del formulario se deserializa con aOrdenDe, fechas yyyy-MM-dd y modo")
    void deserializacion() throws Exception {
        String cuerpo = "{\"codCheque\":0,\"modo\":\"ADMIN\",\"nrocheque\":\"123\",\"aOrdenDe\":\"ACME\","
                + "\"fechaCheque\":\"2026-10-01\",\"fechaCobrar\":\"2026-10-10\",\"monto\":99.5,"
                + "\"codEmpleado\":0,\"observacion\":\"ok\"}";

        ChequeRegistroDto r = json.readValue(cuerpo, ChequeRegistroDto.class);

        assertEquals("ACME", r.getaOrdenDe());
        assertEquals("ADMIN", r.modoEfectivo());
        assertTrue(r.esAlta());
        assertEquals(99.5, r.getMonto(), 0.0001);
        assertEquals(LocalDate.of(2026, 10, 1), new java.sql.Date(r.getFechaCheque().getTime()).toLocalDate());
        assertEquals(LocalDate.of(2026, 10, 10), new java.sql.Date(r.getFechaCobrar().getTime()).toLocalDate());
        assertEquals("ok", r.getObservacion());
    }

    @Test
    @DisplayName("el modo desconocido o ausente cuenta como ESTANDAR")
    void modoPorDefecto() throws Exception {
        assertEquals("ESTANDAR", json.readValue("{}", ChequeRegistroDto.class).modoEfectivo());
        assertEquals("ESTANDAR", json.readValue("{\"modo\":\"loquesea\"}", ChequeRegistroDto.class).modoEfectivo());
        assertEquals("TALONARIO", json.readValue("{\"modo\":\"talonario\"}", ChequeRegistroDto.class).modoEfectivo());
    }

    @Test
    @DisplayName("el filtro de la grilla y las acciones leen sus fechas yyyy-MM-dd")
    void fechasDePeticiones() throws Exception {
        ChequeFiltroDto f = json.readValue("{\"codSucursal\":3,\"fechaCobro\":\"2026-10-05\",\"orden\":\"COBRO\"}",
                ChequeFiltroDto.class);
        assertEquals(LocalDate.of(2026, 10, 5), new java.sql.Date(f.getFechaCobro().getTime()).toLocalDate());

        AccionChequeRequest a = json.readValue("{\"codCheque\":4,\"nuevaFechaCobro\":\"2026-10-20\",\"fecha\":\"2026-10-02\"}",
                AccionChequeRequest.class);
        assertEquals(LocalDate.of(2026, 10, 20), new java.sql.Date(a.getNuevaFechaCobro().getTime()).toLocalDate());
    }

    @Test
    @DisplayName("BeanPropertyRowMapper llena aOrdenDe desde la columna aOrdenDe y audFecha como texto")
    void mapeoDeFilas() throws Exception {
        ResultSetMetaData md = mock(ResultSetMetaData.class);
        when(md.getColumnCount()).thenReturn(5);
        when(md.getColumnLabel(1)).thenReturn("codCheque");
        when(md.getColumnLabel(2)).thenReturn("aOrdenDe");
        when(md.getColumnLabel(3)).thenReturn("audFecha");
        when(md.getColumnLabel(4)).thenReturn("fechaCheque");
        when(md.getColumnLabel(5)).thenReturn("monto");

        ResultSet rs = mock(ResultSet.class);
        when(rs.getMetaData()).thenReturn(md);
        when(rs.getInt(1)).thenReturn(7);
        when(rs.getString(2)).thenReturn("IMPEXPAP SRL");
        when(rs.getString(3)).thenReturn("2026-08-31 14:54:51.5670000 +00:00");
        when(rs.getTimestamp(4)).thenReturn(Timestamp.valueOf("2026-10-01 00:00:00"));
        when(rs.getDouble(5)).thenReturn(1234.5);

        ChCheque c = BeanPropertyRowMapper.newInstance(ChCheque.class).mapRow(rs, 0);

        assertEquals(7, c.getCodCheque().intValue());
        assertEquals("IMPEXPAP SRL", c.getaOrdenDe(), "la columna aOrdenDe tiene que llegar a la propiedad");
        assertEquals("2026-08-31 14:54:51.5670000 +00:00", c.getAudFecha());
        assertEquals(LocalDate.of(2026, 10, 1), new java.sql.Date(c.getFechaCheque().getTime()).toLocalDate());
        assertEquals(1234.5, c.getMonto(), 0.0001);
    }

    @Test
    @DisplayName("el filtro de la grilla recibe el rango de recepcion como fechaRecepcionDesde / fechaRecepcionHasta (yyyy-MM-dd)")
    void filtroConRangoDeRecepcion() throws Exception {
        String cuerpo = "{\"codSucursal\":1,\"estado\":\"PEN\","
                + "\"fechaRecepcionDesde\":\"2026-07-03\",\"fechaRecepcionHasta\":\"2026-10-03\"}";

        ChequeFiltroDto f = json.readValue(cuerpo, ChequeFiltroDto.class);

        assertEquals(LocalDate.of(2026, 7, 3), new java.sql.Date(f.getFechaRecepcionDesde().getTime()).toLocalDate());
        assertEquals(LocalDate.of(2026, 10, 3), new java.sql.Date(f.getFechaRecepcionHasta().getTime()).toLocalDate());
        assertEquals(null, f.getFechaRecepcion(), "el dia suelto antiguo sigue existiendo pero no viaja");

        // Sin las claves: ningun extremo (no filtra).
        ChequeFiltroDto sin = json.readValue("{\"codSucursal\":1}", ChequeFiltroDto.class);
        assertEquals(null, sin.getFechaRecepcionDesde());
        assertEquals(null, sin.getFechaRecepcionHasta());
    }
}
