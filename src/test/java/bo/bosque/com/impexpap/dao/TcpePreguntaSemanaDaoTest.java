package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.dto.TcpePreguntaSemanaBancoDto;
import bo.bosque.com.impexpap.dto.TcpePreguntaSemanaDto;
import bo.bosque.com.impexpap.dto.TcpePreguntaSemanaEstadisticaDto;
import bo.bosque.com.impexpap.dto.TcpePreguntaSemanaOpcionDto;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * v11 (contrato 15): cada llamada a {@code p_list/p_abm_tcpe_PreguntaSemana}, {@code p_abm_tcpe_RsptaSemana} y
 * {@code p_list_tcpe_Resultado 'P'} lleva sus parámetros y su ACCION; las vigentes se arman con V + O.
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

    private static TcpePreguntaSemanaVigenteDto vigente(long cod) {
        TcpePreguntaSemanaVigenteDto v = new TcpePreguntaSemanaVigenteDto();
        v.setCodPreguntaSemana(cod);
        return v;
    }

    private static TcpePreguntaSemanaOpcionDto opcion(long codPreguntaSemana, long codRsptaOp) {
        TcpePreguntaSemanaOpcionDto o = new TcpePreguntaSemanaOpcionDto();
        o.setCodPreguntaSemana(codPreguntaSemana);
        o.setCodRsptaOp(codRsptaOp);
        return o;
    }

    @Test
    @DisplayName("vigentes: V y O con el codUsuario del token; cada vigente recibe sus opciones en el orden del SP")
    void vigentesConOpciones() {
        when(sp.ejecutarListado(eq("p_list_tcpe_PreguntaSemana"), anyMap(), eq("V"), eq(TcpePreguntaSemanaVigenteDto.class)))
                .thenReturn(Arrays.asList(vigente(2L), vigente(1L), vigente(5L)));
        when(sp.ejecutarListado(eq("p_list_tcpe_PreguntaSemana"), anyMap(), eq("O"), eq(TcpePreguntaSemanaOpcionDto.class)))
                .thenReturn(Arrays.asList(opcion(1L, 1L), opcion(1L, 3L), opcion(2L, 258L), opcion(2L, 259L)));

        List<TcpePreguntaSemanaVigenteDto> r = preguntas.vigentes(34L);

        assertEquals(Collections.singletonMap("codUsuario", 34L),
                listado("p_list_tcpe_PreguntaSemana", "V", TcpePreguntaSemanaVigenteDto.class));
        assertEquals(Collections.singletonMap("codUsuario", 34L),
                listado("p_list_tcpe_PreguntaSemana", "O", TcpePreguntaSemanaOpcionDto.class));
        assertEquals(Arrays.asList(2L, 1L, 5L), Arrays.asList(r.get(0).getCodPreguntaSemana(),
                r.get(1).getCodPreguntaSemana(), r.get(2).getCodPreguntaSemana()), "se respeta el orden de V");
        assertEquals(Arrays.asList(258L, 259L), Arrays.asList(r.get(0).getOpciones().get(0).getCodRsptaOp(),
                r.get(0).getOpciones().get(1).getCodRsptaOp()));
        assertEquals(Arrays.asList(1L, 3L), Arrays.asList(r.get(1).getOpciones().get(0).getCodRsptaOp(),
                r.get(1).getOpciones().get(1).getCodRsptaOp()));
        assertTrue(r.get(2).getOpciones().isEmpty(), "sin opciones en O: lista vacía, no null");
    }

    @Test
    @DisplayName("vigentes: si V no trae nada, no se consulta O")
    void sinVigentesNoPideOpciones() {
        when(sp.ejecutarListado(eq("p_list_tcpe_PreguntaSemana"), anyMap(), eq("V"), eq(TcpePreguntaSemanaVigenteDto.class)))
                .thenReturn(Collections.<TcpePreguntaSemanaVigenteDto>emptyList());

        assertTrue(preguntas.vigentes(34L).isEmpty());
        verify(sp, never()).ejecutarListado(anyString(), anyMap(), eq("O"), any());
    }

    @Test
    @DisplayName("gestión L: activo como filtro (false se manda), nulo = todas")
    void gestion() {
        preguntas.gestion(false);
        assertEquals(Collections.singletonMap("activo", false),
                listado("p_list_tcpe_PreguntaSemana", "L", TcpePreguntaSemanaDto.class));
    }

    @Test
    @DisplayName("E y R: solo codPreguntaSemana; nulo o 0 no se manda y el SP no devuelve filas")
    void estadisticaYRespuestas() {
        preguntas.estadistica(7L);
        assertEquals(Collections.singletonMap("codPreguntaSemana", 7L),
                listado("p_list_tcpe_PreguntaSemana", "E", TcpePreguntaSemanaEstadisticaDto.class));
        preguntas.respuestas(0L);
        assertTrue(listado("p_list_tcpe_PreguntaSemana", "R", TcpeRsptaSemanaDetalleDto.class).isEmpty());
    }

    @Test
    @DisplayName("B: categoría y texto recortado; sin ninguno, el mapa va vacío")
    void banco() {
        preguntas.banco(3L, "  cuenta  ");
        Map<String, Object> p = listado("p_list_tcpe_PreguntaSemana", "B", TcpePreguntaSemanaBancoDto.class);
        assertEquals(2, p.size());
        assertEquals(3L, p.get("codClasificacion"));
        assertEquals("cuenta", p.get("texto"));
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
    @DisplayName("guardar genérico: sin código es I, las fechas van como java.sql.Date y lo nulo no se manda")
    void guardarPreguntaSemana() {
        TcpePreguntaSemana m = new TcpePreguntaSemana();
        m.setCodPregunta(26L);
        m.setFechaInicio(LocalDate.of(2026, 10, 20));
        m.setFechaFin(LocalDate.of(2026, 10, 26));

        preguntas.guardar(m, 7L);

        Map<String, Object> p = abm("p_abm_tcpe_PreguntaSemana", "I");
        assertEquals(0L, p.get("codPreguntaSemana"));
        assertEquals(26L, p.get("codPregunta"));
        assertEquals(java.sql.Date.valueOf("2026-10-20"), p.get("fechaInicio"));
        assertEquals(java.sql.Date.valueOf("2026-10-26"), p.get("fechaFin"));
        assertFalse(p.containsKey("titulo"));
        assertFalse(p.containsKey("mostrarRespuesta"), "nulo: el SP pone 1");
        assertEquals(7L, p.get("audUsuario"));
    }

    @Test
    @DisplayName("responder: I con codUsuario del token también como audUsuario; idGenerado y mensaje del SP")
    void responder() {
        RespuestaSp r = respuestas.responder(1L, 3L, 34L);
        Map<String, Object> p = abm("p_abm_tcpe_RsptaSemana", "I");
        assertEquals(4, p.size());
        assertEquals(1L, p.get("codPreguntaSemana"));
        assertEquals(3L, p.get("codRsptaOp"));
        assertEquals(34L, p.get("codUsuario"));
        assertEquals(34L, p.get("audUsuario"));
        assertFalse(p.containsKey("esCorrecta"), "la calcula el SP");
        assertEquals(9L, r.getIdGenerado());
    }

    @Test
    @DisplayName("responder sin opción: no se manda codRsptaOp y el SP responde 'Elige una opción.'")
    void responderSinOpcion() {
        respuestas.responder(1L, null, 34L);
        assertFalse(abm("p_abm_tcpe_RsptaSemana", "I").containsKey("codRsptaOp"));
    }

    @Test
    @DisplayName("el guardar genérico de rsptaSemana se rechaza sin llamar al SP")
    void guardarGenericoRechazado() {
        TcpeRsptaSemana m = new TcpeRsptaSemana();
        m.setCodPreguntaSemana(1L);
        m.setCodUsuario(99L);
        m.setCodRsptaOp(3L);

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
