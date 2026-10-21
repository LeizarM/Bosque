package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.dto.TcpePreguntaSemanaDto;
import bo.bosque.com.impexpap.dto.TcpePreguntaSemanaEscalaDto;
import bo.bosque.com.impexpap.dto.TcpePreguntaSemanaVigenteDto;
import bo.bosque.com.impexpap.dto.TcpeRankingPreguntaDto;
import bo.bosque.com.impexpap.dto.TcpeRsptaSemanaDetalleDto;
import bo.bosque.com.impexpap.model.TcpePreguntaSemana;
import bo.bosque.com.impexpap.model.TcpeRsptaSemana;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * v11 a v13 (contratos 15 a 17): cada llamada a {@code p_list/p_abm_tcpe_PreguntaSemana}, {@code p_abm_tcpe_RsptaSemana}
 * y {@code p_list_tcpe_Resultado 'P'} lleva sus parámetros y su ACCION; se responde con un texto o un valor.
 */
class TcpePreguntaSemanaDaoTest {

    private SpHelper sp;
    private TcpePreguntaSemanaDao preguntas;
    private TcpeRsptaSemanaDao respuestas;

    @BeforeEach
    void preparar() {
        sp = mock(SpHelper.class);
        when(sp.ejecutarAbmMap(anyString(), anyMap(), anyString())).thenReturn(new RespuestaSp(0, "ok", 9L));
        preguntas = new TcpePreguntaSemanaDao(sp);
        respuestas = new TcpeRsptaSemanaDao(sp);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> listado(String spName, String accion, Class<?> tipo) {
        ArgumentCaptor<Map<String, Object>> p = ArgumentCaptor.forClass(Map.class);
        verify(sp).ejecutarListado(eq(spName), p.capture(), eq(accion), eq(tipo));
        return p.getValue();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> abm(String spName, String accion) {
        ArgumentCaptor<Map<String, Object>> p = ArgumentCaptor.forClass(Map.class);
        verify(sp).ejecutarAbmMap(eq(spName), p.capture(), eq(accion));
        return p.getValue();
    }

    @Test
    @DisplayName("vigentes: solo V con el codUsuario del token; las filas del SP vuelven tal cual")
    void vigentes() {
        List<TcpePreguntaSemanaVigenteDto> filas = Collections.singletonList(new TcpePreguntaSemanaVigenteDto());
        when(sp.ejecutarListado(eq("p_list_tcpe_PreguntaSemana"), anyMap(), eq("V"), eq(TcpePreguntaSemanaVigenteDto.class)))
                .thenReturn(filas);

        assertSame(filas, preguntas.vigentes(34L));
        assertEquals(Collections.singletonMap("codUsuario", 34L),
                listado("p_list_tcpe_PreguntaSemana", "V", TcpePreguntaSemanaVigenteDto.class));
    }

    @Test
    @DisplayName("gestión L: activo como filtro (false se manda), nulo = todas")
    void gestion() {
        preguntas.gestion(false);
        assertEquals(Collections.singletonMap("activo", false),
                listado("p_list_tcpe_PreguntaSemana", "L", TcpePreguntaSemanaDto.class));
    }

    @Test
    @DisplayName("R: solo codPreguntaSemana")
    void respuestas() {
        preguntas.respuestas(7L);
        assertEquals(Collections.singletonMap("codPreguntaSemana", 7L),
                listado("p_list_tcpe_PreguntaSemana", "R", TcpeRsptaSemanaDetalleDto.class));
    }

    @Test
    @DisplayName("E: solo codPreguntaSemana; las filas del SP vuelven tal cual")
    void distribucion() {
        List<TcpePreguntaSemanaEscalaDto> filas = Collections.singletonList(new TcpePreguntaSemanaEscalaDto());
        when(sp.ejecutarListado(eq("p_list_tcpe_PreguntaSemana"), anyMap(), eq("E"), eq(TcpePreguntaSemanaEscalaDto.class)))
                .thenReturn(filas);

        assertSame(filas, preguntas.distribucion(7L));
        assertEquals(Collections.singletonMap("codPreguntaSemana", 7L),
                listado("p_list_tcpe_PreguntaSemana", "E", TcpePreguntaSemanaEscalaDto.class));
    }

    @Test
    @DisplayName("R con código 0: no se manda y el SP no devuelve filas")
    void respuestasSinCodigo() {
        preguntas.respuestas(0L);
        assertTrue(listado("p_list_tcpe_PreguntaSemana", "R", TcpeRsptaSemanaDetalleDto.class).isEmpty());
    }

    @Test
    @DisplayName("A: código, activo y audUsuario; el mensaje del SP vuelve tal cual")
    void activar() {
        RespuestaSp r = preguntas.activar(4L, false, 7L);
        Map<String, Object> p = abm("p_abm_tcpe_PreguntaSemana", "A");
        assertEquals(3, p.size());
        assertEquals(4L, p.get("codPreguntaSemana"));
        assertEquals(false, p.get("activo"));
        assertEquals(7L, p.get("audUsuario"));
        assertEquals("ok", r.getErrormsg());
    }

    @Test
    @DisplayName("guardar genérico: sin código es I, el texto y las fechas viajan y lo nulo no se manda")
    void guardarPreguntaSemana() {
        TcpePreguntaSemana m = new TcpePreguntaSemana();
        m.setPregunta("¿Qué tal tu día?");
        m.setFechaInicio(LocalDate.of(2026, 10, 20));
        m.setFechaFin(LocalDate.of(2026, 10, 26));

        preguntas.guardar(m, 7L);

        Map<String, Object> p = abm("p_abm_tcpe_PreguntaSemana", "I");
        assertEquals(0L, p.get("codPreguntaSemana"));
        assertEquals("¿Qué tal tu día?", p.get("pregunta"));
        assertEquals(java.sql.Date.valueOf("2026-10-20"), p.get("fechaInicio"));
        assertEquals(java.sql.Date.valueOf("2026-10-26"), p.get("fechaFin"));
        assertFalse(p.containsKey("titulo"));
        assertFalse(p.containsKey("activo"), "nulo: el SP pone 1");
        assertFalse(p.containsKey("codPregunta"), "v12: ya no sale del banco");
        assertFalse(p.containsKey("tipo"), "nulo: el SP la deja abierta");
        assertEquals(7L, p.get("audUsuario"));
    }

    @Test
    @DisplayName("guardar de escala: tipo, rango y etiquetas viajan; la etiqueta nula no se manda")
    void guardarEscala() {
        TcpePreguntaSemana m = new TcpePreguntaSemana();
        m.setCodPreguntaSemana(4L);
        m.setPregunta("¿Cómo ves el ambiente laboral?");
        m.setTipo("E");
        m.setEscalaMin(0);
        m.setEscalaMax(10);
        m.setEtiquetaMax("Excelente");

        preguntas.guardar(m, 7L);

        Map<String, Object> p = abm("p_abm_tcpe_PreguntaSemana", "U");
        assertEquals(4L, p.get("codPreguntaSemana"));
        assertEquals("E", p.get("tipo"));
        assertEquals(0, p.get("escalaMin"), "el 0 es un valor de la escala, no un vacío");
        assertEquals(10, p.get("escalaMax"));
        assertEquals("Excelente", p.get("etiquetaMax"));
        assertFalse(p.containsKey("etiquetaMin"));
    }

    @Test
    @DisplayName("responder: I con el texto y el codUsuario del token también como audUsuario; idGenerado del SP")
    void responder() {
        RespuestaSp r = respuestas.responder(1L, "Muy bien, gracias \uD83D\uDE0A", null, 34L);
        Map<String, Object> p = abm("p_abm_tcpe_RsptaSemana", "I");
        assertEquals(4, p.size());
        assertEquals(1L, p.get("codPreguntaSemana"));
        assertEquals("Muy bien, gracias \uD83D\uDE0A", p.get("respuesta"), "el emoji viaja tal cual");
        assertEquals(34L, p.get("codUsuario"));
        assertEquals(34L, p.get("audUsuario"));
        assertEquals(9L, r.getIdGenerado());
    }

    @Test
    @DisplayName("responder sin texto: no se manda respuesta y el SP responde 'Escribe tu respuesta.'")
    void responderSinTexto() {
        respuestas.responder(1L, null, null, 34L);
        assertFalse(abm("p_abm_tcpe_RsptaSemana", "I").containsKey("respuesta"));
    }

    @Test
    @DisplayName("responder una de escala: el valor viaja, también el 0; sin texto")
    void responderEscala() {
        respuestas.responder(1L, null, 0, 34L);
        Map<String, Object> p = abm("p_abm_tcpe_RsptaSemana", "I");
        assertEquals(4, p.size());
        assertEquals(0, p.get("valor"));
        assertFalse(p.containsKey("respuesta"));
    }

    @Test
    @DisplayName("el guardar genérico de rsptaSemana se rechaza sin llamar al SP")
    void guardarGenericoRechazado() {
        TcpeRsptaSemana m = new TcpeRsptaSemana();
        m.setCodPreguntaSemana(1L);
        m.setCodUsuario(99L);
        m.setRespuesta("Bien");

        SpBusinessException e = assertThrows(SpBusinessException.class, () -> respuestas.guardar(m, 7L));
        assertTrue(e.getMessage().contains("desde el inicio"));
        verify(sp, never()).ejecutarAbmMap(anyString(), anyMap(), anyString());
    }

    @Test
    @DisplayName("eliminar genérico de rsptaSemana: D con codRsptaSemana y audUsuario")
    void eliminarRespuesta() {
        respuestas.eliminar(5L, 7L);
        Map<String, Object> p = abm("p_abm_tcpe_RsptaSemana", "D");
        assertEquals(5L, p.get("codRsptaSemana"));
        assertEquals(7L, p.get("audUsuario"));
    }

    @Test
    @DisplayName("ranking P: los cuatro filtros de C más codRolExamen; los nulos y ceros no viajan")
    void ranking() {
        TcpeResultadoDao resultados = new TcpeResultadoDao(sp);
        List<TcpeRankingPreguntaDto> filas = Collections.singletonList(new TcpeRankingPreguntaDto());
        when(sp.ejecutarListado(eq("p_list_tcpe_Resultado"), anyMap(), eq("P"), eq(TcpeRankingPreguntaDto.class)))
                .thenReturn(filas);

        assertSame(filas, resultados.ranking(2L, 0L, LocalDate.of(2026, 1, 1), null));

        Map<String, Object> p = listado("p_list_tcpe_Resultado", "P", TcpeRankingPreguntaDto.class);
        assertEquals(2, p.size());
        assertEquals(2L, p.get("codClasificacion"));
        assertEquals(java.sql.Date.valueOf("2026-01-01"), p.get("fechaDesde"));
    }
}
