package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.commons.AccesoModuloHelper;
import bo.bosque.com.impexpap.commons.TcpePermisos;
import bo.bosque.com.impexpap.config.SinPermisoException;
import bo.bosque.com.impexpap.dao.ITcpeCalificacion;
import bo.bosque.com.impexpap.dao.ITcpePreguntaSemana;
import bo.bosque.com.impexpap.dao.ITcpeRendir;
import bo.bosque.com.impexpap.dao.ITcpeResultado;
import bo.bosque.com.impexpap.dao.ITcpeRsptaSemana;
import bo.bosque.com.impexpap.dao.IVistaDao;
import bo.bosque.com.impexpap.dto.TcpePreguntaSemanaDto;
import bo.bosque.com.impexpap.dto.TcpePreguntaSemanaFiltroDto;
import bo.bosque.com.impexpap.dto.TcpePreguntaSemanaVigenteDto;
import bo.bosque.com.impexpap.dto.TcpeRankingPreguntaDto;
import bo.bosque.com.impexpap.dto.TcpeResultadoFiltroDto;
import bo.bosque.com.impexpap.model.TcpePreguntaSemana;
import bo.bosque.com.impexpap.model.Vista;
import bo.bosque.com.impexpap.security.jwt.DatosToken;
import bo.bosque.com.impexpap.utils.ApiResponse;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Permisos de las preguntas de la semana (contrato 15.3), con la regla real de {@link TcpePermisos}: el inicio
 * (vigentes y responder) no pide pantalla y usa el usuario del token; la gestión pide {@code PreguntaSemana}.
 */
class TcpePreguntaSemanaControllerTest {

    private IVistaDao vistas;
    private ITcpePreguntaSemana preguntas;
    private ITcpeRsptaSemana respuestas;
    private TcpePermisos permisos;
    private TcpePreguntaSemanaController controlador;

    @BeforeEach
    void preparar() {
        vistas = mock(IVistaDao.class);
        preguntas = mock(ITcpePreguntaSemana.class);
        respuestas = mock(ITcpeRsptaSemana.class);
        permisos = new TcpePermisos(vistas, new AccesoModuloHelper(null, null));
        controlador = new TcpePreguntaSemanaController(preguntas, respuestas, permisos);
    }

    private static Authentication usuario(String rol) {
        UsernamePasswordAuthenticationToken t = new UsernamePasswordAuthenticationToken(
                "empleado", null, Collections.singletonList(new SimpleGrantedAuthority(rol)));
        t.setDetails(new DatosToken(34, 1, 1, rol));
        return t;
    }

    private void conPantalla(String direccion) {
        when(vistas.obtainRoutes(34)).thenReturn(Collections.singletonList(new Vista().setDireccion(direccion)));
    }

    private static TcpePreguntaSemanaFiltroDto pregunta(long cod) {
        TcpePreguntaSemanaFiltroDto d = new TcpePreguntaSemanaFiltroDto();
        d.setCodPreguntaSemana(cod);
        return d;
    }

    @Test
    @DisplayName("vigentes: cualquier empleado, sin mirar sus pantallas, con el codUsuario del token")
    void vigentesSinPantalla() {
        when(preguntas.vigentes(34L)).thenReturn(Collections.singletonList(new TcpePreguntaSemanaVigenteDto()));

        assertEquals(200, controlador.vigentes(usuario("ROLE_LIM")).getStatusCodeValue());
        verify(preguntas).vigentes(34L);
        verify(vistas, never()).obtainRoutes(anyInt());
    }

    @Test
    @DisplayName("vigentes sin preguntas: 204")
    void vigentesVacias() {
        when(preguntas.vigentes(34L)).thenReturn(Collections.<TcpePreguntaSemanaVigenteDto>emptyList());
        assertEquals(204, controlador.vigentes(usuario("ROLE_LIM")).getStatusCodeValue());
    }

    @Test
    @DisplayName("responder: sin pantalla; message = errormsg del SP y data = codRsptaSemana")
    void responder() {
        TcpePreguntaSemanaFiltroDto d = pregunta(1L);
        d.setCodRsptaOp(3L);
        when(respuestas.responder(1L, 3L, 34L)).thenReturn(new RespuestaSp(0, "¡Correcto!", 11L));

        ResponseEntity<ApiResponse<?>> r = controlador.responder(d, usuario("ROLE_LIM"));

        assertEquals(201, r.getStatusCodeValue());
        assertEquals("¡Correcto!", r.getBody().getMessage());
        assertEquals(11L, r.getBody().getData());
        verify(vistas, never()).obtainRoutes(anyInt());
    }

    @Test
    @DisplayName("gestión con la pantalla PreguntaSemana: lista, guarda, activa, elimina y consulta")
    void gestionConPantalla() {
        conPantalla(TcpePermisos.PREGUNTA_SEMANA);
        when(preguntas.gestion(null)).thenReturn(Collections.singletonList(new TcpePreguntaSemanaDto()));
        when(preguntas.guardar(any(), anyLong())).thenReturn(new RespuestaSp(0, "Pregunta de la semana registrada.", 4L));
        when(preguntas.activar(eq(4L), eq(false), anyLong())).thenReturn(new RespuestaSp(0, "", 4L));
        when(preguntas.eliminar(eq(4L), anyLong())).thenReturn(new RespuestaSp(0, "", 4L));
        Authentication lim = usuario("ROLE_LIM");

        assertEquals(200, controlador.listar(null, lim).getStatusCodeValue());
        ResponseEntity<ApiResponse<?>> g = controlador.guardar(new TcpePreguntaSemana(), lim);
        assertEquals("Pregunta de la semana registrada.", g.getBody().getMessage());
        TcpePreguntaSemanaFiltroDto a = pregunta(4L);
        a.setActivo(false);
        assertEquals(201, controlador.activar(a, lim).getStatusCodeValue());
        assertEquals(201, controlador.eliminar(pregunta(4L), lim).getStatusCodeValue());
        assertEquals(204, controlador.respuestas(pregunta(4L), lim).getStatusCodeValue());
        assertEquals(204, controlador.estadistica(pregunta(4L), lim).getStatusCodeValue());
        assertEquals(204, controlador.banco(new TcpePreguntaSemanaFiltroDto(), lim).getStatusCodeValue());

        verify(preguntas).guardar(any(), eq(34L));
        verify(preguntas).activar(4L, false, 34L);
        verify(preguntas).eliminar(4L, 34L);
        verify(preguntas).respuestas(4L);
        verify(preguntas).estadistica(4L);
    }

    @Test
    @DisplayName("gestión con otra pantalla del módulo (Banco): 403 en todo y no se toca nada")
    void gestionSinPantalla() {
        conPantalla(TcpePermisos.BANCO);
        Authentication lim = usuario("ROLE_LIM");

        assertThrows(SinPermisoException.class, () -> controlador.listar(null, lim));
        assertThrows(SinPermisoException.class, () -> controlador.guardar(new TcpePreguntaSemana(), lim));
        assertThrows(SinPermisoException.class, () -> controlador.activar(pregunta(4L), lim));
        assertThrows(SinPermisoException.class, () -> controlador.eliminar(pregunta(4L), lim));
        assertThrows(SinPermisoException.class, () -> controlador.respuestas(pregunta(4L), lim));
        assertThrows(SinPermisoException.class, () -> controlador.estadistica(pregunta(4L), lim));
        assertThrows(SinPermisoException.class, () -> controlador.banco(pregunta(4L), lim));
        verify(preguntas, never()).gestion(any());
        verify(preguntas, never()).guardar(any(), anyLong());
        verify(preguntas, never()).eliminar(any(), anyLong());
    }

    @Test
    @DisplayName("el administrador gestiona sin consultar sus pantallas")
    void administrador() {
        when(preguntas.gestion(true)).thenReturn(Collections.singletonList(new TcpePreguntaSemanaDto()));
        TcpePreguntaSemanaFiltroDto f = new TcpePreguntaSemanaFiltroDto();
        f.setActivo(true);
        assertEquals(200, controlador.listar(f, usuario("ROLE_ADM")).getStatusCodeValue());
        verify(vistas, never()).obtainRoutes(anyInt());
    }

    @Test
    @DisplayName("ranking de preguntas: pasa con Informes o Resultados; con Lotes, 403")
    void ranking() {
        ITcpeResultado resultados = mock(ITcpeResultado.class);
        when(resultados.ranking(any(), any(), any(), any()))
                .thenReturn(Collections.singletonList(new TcpeRankingPreguntaDto()));
        TcpeResultadosController r = new TcpeResultadosController(resultados, mock(ITcpeCalificacion.class),
                mock(ITcpeRendir.class), permisos);
        TcpeResultadoFiltroDto f = new TcpeResultadoFiltroDto();
        f.setCodClasificacion(2L);

        conPantalla(TcpePermisos.INFORMES);
        assertEquals(200, r.rankingPreguntas(f, usuario("ROLE_LIM")).getStatusCodeValue());
        verify(resultados).ranking(2L, null, null, null);

        conPantalla(TcpePermisos.RESULTADOS);
        assertEquals(200, r.rankingPreguntas(null, usuario("ROLE_LIM")).getStatusCodeValue());

        conPantalla(TcpePermisos.LOTES);
        assertThrows(SinPermisoException.class, () -> r.rankingPreguntas(f, usuario("ROLE_LIM")));
    }
}
