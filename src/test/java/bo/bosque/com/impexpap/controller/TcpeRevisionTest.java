package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.dao.ITcpeAcceso;
import bo.bosque.com.impexpap.dao.ITcpeRendir;
import bo.bosque.com.impexpap.dto.TcpeLatidoDto;
import bo.bosque.com.impexpap.dto.TcpeRendirAccionDto;
import bo.bosque.com.impexpap.dto.TcpeRevisionItemDto;
import bo.bosque.com.impexpap.security.jwt.DatosPostulante;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Devolución al terminar ({@code p_list_tcpe_Rendir 'F'}): el dueño del examen sale del token (postulante o el
 * {@code tcpe_usuario} del empleado), nunca del cuerpo; vacía responde 204.
 */
class TcpeRevisionTest {

    private ITcpeRendir dao;
    private ITcpeAcceso acceso;
    private TcpeRendicion rendicion;

    @BeforeEach
    void preparar() {
        dao = mock(ITcpeRendir.class);
        acceso = mock(ITcpeAcceso.class);
        rendicion = new TcpeRendicion(dao);
    }

    private static TcpeRendirAccionDto examen(long codExamen) {
        TcpeRendirAccionDto d = new TcpeRendirAccionDto();
        d.setCodExamen(codExamen);
        return d;
    }

    private static Authentication postulante(long codUsuario) {
        UsernamePasswordAuthenticationToken t = new UsernamePasswordAuthenticationToken(
                "IPX-1", null, Collections.singletonList(new SimpleGrantedAuthority(DatosPostulante.ROL)));
        t.setDetails(new DatosPostulante(codUsuario, "IPX-1"));
        return t;
    }

    private static List<TcpeRevisionItemDto> unaFila() {
        TcpeRevisionItemDto f = new TcpeRevisionItemDto();
        f.setCodPreguntaH(1L);
        f.setOrden(1);
        f.setMarcada(true);
        return Collections.singletonList(f);
    }

    @Test
    @DisplayName("postulante: revisión con el codUsuario del token y 200 con las filas")
    void postulanteConCodUsuarioDelToken() {
        when(dao.revision(10L, 42L)).thenReturn(unaFila());

        ResponseEntity<ApiResponse<?>> r = new TcpeExamenPublicoController(acceso, rendicion)
                .revision(examen(10L), postulante(42L));

        assertEquals(200, r.getStatusCodeValue());
        assertEquals(1, ((List<?>) r.getBody().getData()).size());
        verify(dao).revision(10L, 42L);
    }

    @Test
    @DisplayName("revisión vacía (no es suyo o no se entregó): 204")
    void vaciaEs204() {
        when(dao.revision(eq(10L), anyLong())).thenReturn(Collections.<TcpeRevisionItemDto>emptyList());

        ResponseEntity<ApiResponse<?>> r = new TcpeExamenPublicoController(acceso, rendicion)
                .revision(examen(10L), postulante(42L));

        assertEquals(204, r.getStatusCodeValue());
    }

    @Test
    @DisplayName("empleado: el dueño es el tcpe_usuario de su codEmpleado, no su codUsuario de tb_usuario")
    void empleadoConSuTcpeUsuario() {
        UsernamePasswordAuthenticationToken t = new UsernamePasswordAuthenticationToken(
                "jperez", null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_LIM")));
        t.setDetails(new DatosToken(34, 3, 1, "ROLE_LIM"));
        when(acceso.codUsuarioDeEmpleado(3L)).thenReturn(77L);
        when(dao.revision(10L, 77L)).thenReturn(unaFila());

        ResponseEntity<ApiResponse<?>> r = new TcpeMisExamenesController(acceso, rendicion).revision(examen(10L), t);

        assertEquals(200, r.getStatusCodeValue());
        verify(dao).revision(10L, 77L);
        verify(dao, never()).revision(10L, 34L);
    }

    @Test
    @DisplayName("latido del empleado: Rendir A con su tcpe_usuario y data = segundos restantes")
    void latidoEmpleado() {
        UsernamePasswordAuthenticationToken t = new UsernamePasswordAuthenticationToken(
                "jperez", null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_LIM")));
        t.setDetails(new DatosToken(34, 3, 1, "ROLE_LIM"));
        when(acceso.codUsuarioDeEmpleado(3L)).thenReturn(77L);
        when(dao.latido(10L, 77L, 5L, true)).thenReturn(new RespuestaSp(0, "", 540L));
        TcpeLatidoDto d = new TcpeLatidoDto();
        d.setCodExamen(10L);
        d.setCodPreguntaH(5L);
        d.setSalioPestana(true);

        ResponseEntity<ApiResponse<?>> r = new TcpeMisExamenesController(acceso, rendicion).latido(d, t);

        assertEquals(540L, r.getBody().getData());
        verify(dao).latido(10L, 77L, 5L, true);
    }

    @Test
    @DisplayName("empleado sin tcpe_usuario: va 0 como dueño y el SP no encuentra nada suyo (204)")
    void empleadoSinUsuarioDeExamenes() {
        UsernamePasswordAuthenticationToken t = new UsernamePasswordAuthenticationToken(
                "jperez", null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_LIM")));
        t.setDetails(new DatosToken(34, 3, 1, "ROLE_LIM"));
        when(dao.revision(10L, 0L)).thenReturn(Collections.<TcpeRevisionItemDto>emptyList());

        ResponseEntity<ApiResponse<?>> r = new TcpeMisExamenesController(acceso, rendicion).revision(examen(10L), t);

        assertEquals(204, r.getStatusCodeValue());
        verify(dao).revision(10L, 0L);
    }
}
