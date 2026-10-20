package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.commons.TcpeClaveService;
import bo.bosque.com.impexpap.commons.TcpePermisos;
import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.dao.ITcpeAcceso;
import bo.bosque.com.impexpap.dao.ITcpeUsuario;
import bo.bosque.com.impexpap.dto.TcpeClaveGeneradaDto;
import bo.bosque.com.impexpap.dto.TcpeUsuarioClaveDto;
import bo.bosque.com.impexpap.model.TcpeUsuario;
import bo.bosque.com.impexpap.security.jwt.DatosToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Regenerar la clave de un externo: siempre al azar (contrato 6.1, sin "restablecer al CI"). */
class TcpeUsuariosControllerTest {

    private ITcpeUsuario usuarios;
    private ITcpeAcceso acceso;
    private TcpeClaveService claves;
    private TcpeUsuariosController controlador;
    private Authentication auth;
    private TcpeUsuario u;

    @BeforeEach
    void preparar() {
        usuarios = mock(ITcpeUsuario.class);
        acceso = mock(ITcpeAcceso.class);
        claves = mock(TcpeClaveService.class);
        controlador = new TcpeUsuariosController(usuarios, acceso, claves, mock(TcpePermisos.class));
        UsernamePasswordAuthenticationToken t = new UsernamePasswordAuthenticationToken(
                "evaluador", null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_LIM")));
        t.setDetails(new DatosToken(34, 1, 1, "ROLE_LIM"));
        auth = t;

        u = new TcpeUsuario();
        u.setCodUsuario(5L);
        u.setLogin("IPX-12");
        u.setCi("1234567");
        u.setNombres("Ana");
        u.setApellidoPaterno("Paz");
        u.setApellidoMaterno("Mena");
        when(usuarios.obtener(5L)).thenReturn(u);
        when(claves.hash(anyString())).thenAnswer(inv -> "hash:" + inv.getArgument(0));
        when(claves.generar()).thenReturn("Azar234567");
    }

    private TcpeClaveGeneradaDto regenerar() {
        TcpeUsuarioClaveDto d = new TcpeUsuarioClaveDto();
        d.setCodUsuario(5L);
        return (TcpeClaveGeneradaDto) controlador.regenerarClave(d, auth).getBody().getData();
    }

    @Test
    @DisplayName("clave al azar: guarda el hash con el auditor del token y devuelve login, ci y nombre")
    void alAzar() {
        TcpeClaveGeneradaDto r = regenerar();

        verify(acceso).guardarHash(5L, "hash:Azar234567", 34L);
        assertEquals("Azar234567", r.getPassword());
        assertEquals("IPX-12", r.getLogin());
        assertEquals("1234567", r.getCi());
        assertEquals("Paz Mena Ana", r.getNombreCompleto());
    }

    @Test
    @DisplayName("a quién se le puede cambiar lo decide el SP: su error llega tal cual y no se lee el usuario")
    void reglaEnElSp() {
        SpBusinessException delSp = new SpBusinessException("El usuario indicado no existe.");
        when(acceso.guardarHash(anyLong(), any(), any())).thenThrow(delSp);

        SpBusinessException e = assertThrows(SpBusinessException.class, this::regenerar);

        assertEquals("El usuario indicado no existe.", e.getMessage());
        verify(usuarios, never()).obtener(anyLong());
    }
}
