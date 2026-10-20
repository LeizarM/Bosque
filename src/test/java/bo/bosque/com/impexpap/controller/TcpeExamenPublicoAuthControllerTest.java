package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.commons.TcpeClaveService;
import bo.bosque.com.impexpap.dao.ITcpeAcceso;
import bo.bosque.com.impexpap.dao.ITcpeRendir;
import bo.bosque.com.impexpap.dto.TcpeAccesoDto;
import bo.bosque.com.impexpap.dto.TcpeLoginPostulanteDto;
import bo.bosque.com.impexpap.dto.TcpeSesionPostulanteDto;
import bo.bosque.com.impexpap.security.jwt.JwtProvider;
import bo.bosque.com.impexpap.utils.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.http.ResponseEntity;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Login del postulante (contrato 7.4): con la clave correcta, {@code Rendir 'V'} de ese usuario y después
 * {@code puedeIngresar} de SQL decide, y su {@code mensajeIngreso} va en el 401. Sin ingreso no hay token ni migración.
 */
class TcpeExamenPublicoAuthControllerTest {

    private static final String PLANO = "Abc2345xyz";
    private static final String MSG_SQL = "Su examen ya fue calificado y el plazo para ver la nota terminó.";

    private ITcpeAcceso acceso;
    private ITcpeRendir rendir;
    private TcpeClaveService claves;
    private JwtProvider jwt;
    private TcpeExamenPublicoAuthController controlador;
    private TcpeAccesoDto u;

    @BeforeEach
    void preparar() {
        acceso = mock(ITcpeAcceso.class);
        rendir = mock(ITcpeRendir.class);
        claves = mock(TcpeClaveService.class);
        jwt = mock(JwtProvider.class);
        controlador = new TcpeExamenPublicoAuthController(acceso, rendir, claves, jwt);

        u = new TcpeAccesoDto();
        u.setCodUsuario(42L);
        u.setLogin("IPX-1");
        u.setPassword2(PLANO);   // texto plano del legacy: sería candidata a la migración perezosa
        u.setActivo(true);
        u.setCodTipoUsuario(2L);
        u.setDatosCompletos(true);
        when(acceso.porLogin("IPX-1")).thenReturn(u);
        when(claves.coincide(eq(PLANO), any())).thenReturn(true);
        when(claves.hash(PLANO)).thenReturn("$2a$14$hash");
        when(jwt.expiracionDesdeAhora()).thenReturn(new Date());
        when(jwt.generarTokenPostulante(anyLong(), anyString(), any())).thenReturn("token");
    }

    private ResponseEntity<ApiResponse<?>> login(String clave) {
        TcpeLoginPostulanteDto d = new TcpeLoginPostulanteDto();
        d.setLogin(" IPX-1 ");
        d.setPassword2(clave);
        return controlador.login(d);
    }

    @Test
    @DisplayName("puedeIngresar = 0 con la clave correcta: 401 con mensajeIngreso de SQL, sin token ni migración")
    void noPuedeIngresar() {
        u.setPuedeIngresar(false);
        u.setMensajeIngreso(MSG_SQL);
        u.setExamenesPendientes(1);   // ya es solo un dato: no decide

        ResponseEntity<ApiResponse<?>> r = login(PLANO);

        assertEquals(401, r.getStatusCodeValue());
        assertEquals(MSG_SQL, r.getBody().getMessage());
        assertNull(r.getBody().getData());
        verify(acceso, never()).guardarHash(anyLong(), any(), any());
        verify(jwt, never()).generarTokenPostulante(anyLong(), anyString(), any());
    }

    @Test
    @DisplayName("usuario desactivado: lo decide SQL (puedeIngresar = 0 y su mensaje), no una regla en Java")
    void inactivoLoDecideSql() {
        u.setActivo(false);
        u.setPuedeIngresar(false);
        u.setMensajeIngreso("Su usuario esta desactivado.");

        ResponseEntity<ApiResponse<?>> r = login(PLANO);

        assertEquals(401, r.getStatusCodeValue());
        assertEquals("Su usuario esta desactivado.", r.getBody().getMessage());
    }

    @Test
    @DisplayName("sin puedeIngresar (SP anterior a v4): no habilita")
    void sinColumnaNoHabilita() {
        assertEquals(401, login(PLANO).getStatusCodeValue());
        verify(jwt, never()).generarTokenPostulante(anyLong(), anyString(), any());
    }

    @Test
    @DisplayName("con la clave correcta cierra solo los vencidos y ausentes de ese usuario (Rendir V con @codUsuario)")
    void rendirVSoloDelUsuario() {
        u.setPuedeIngresar(true);

        login(PLANO);

        InOrder orden = inOrder(acceso, rendir);
        orden.verify(acceso).porLogin("IPX-1");
        orden.verify(rendir).entregarVencidosDe(42L);
        verify(rendir, never()).entregarVencidos(any(), any());
    }

    @Test
    @DisplayName("clave errada: no escribe nada (sin Rendir V)")
    void claveErradaSinRendirV() {
        when(claves.coincide(eq("otraClave9"), any())).thenReturn(false);

        login("otraClave9");

        verify(rendir, never()).entregarVencidosDe(anyLong());
    }

    @Test
    @DisplayName("si Rendir V falla, el login sigue")
    void rendirVFallaYSigue() {
        u.setPuedeIngresar(true);
        when(rendir.entregarVencidosDe(anyLong())).thenThrow(new RuntimeException("sin conexión"));

        assertEquals(200, login(PLANO).getStatusCodeValue());
    }

    @Test
    @DisplayName("clave errada y sin ingreso: el 401 genérico, sin revelar el estado del examen")
    void claveErradaNoRevela() {
        u.setPuedeIngresar(false);
        u.setMensajeIngreso(MSG_SQL);
        when(claves.coincide(eq("otraClave9"), any())).thenReturn(false);

        ResponseEntity<ApiResponse<?>> r = login("otraClave9");

        assertEquals(401, r.getStatusCodeValue());
        assertEquals(TcpeExamenPublicoAuthController.MSG_CREDENCIALES, r.getBody().getMessage());
    }

    @Test
    @DisplayName("puedeIngresar = 1: entra, recibe el token y la clave en texto plano se migra a hash")
    void puedeIngresarEntraYMigra() {
        u.setPuedeIngresar(true);
        u.setExamenesPendientes(0);   // POR_REVISAR o calificado reciente: vuelve a ver su nota

        ResponseEntity<ApiResponse<?>> r = login(PLANO);

        assertEquals(200, r.getStatusCodeValue());
        assertEquals("token", ((TcpeSesionPostulanteDto) r.getBody().getData()).getToken());
        verify(acceso).guardarHash(42L, "$2a$14$hash", null);
    }
}
