package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.commons.AccesoPantalla;
import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.dao.ICoche;
import bo.bosque.com.impexpap.model.Coche;
import bo.bosque.com.impexpap.security.jwt.DatosToken;
import bo.bosque.com.impexpap.utils.ApiResponse;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Gestion de coches: la pantalla se exige en los tres endpoints y audUsuario sale del token. */
class CocheControllerTest {

    private ICoche dao;
    private AccesoPantalla pantallas;
    private CocheController controlador;
    private Authentication auth;

    @BeforeEach
    void setUp() {
        dao = mock(ICoche.class);
        pantallas = mock(AccesoPantalla.class);
        controlador = new CocheController(dao, pantallas);

        UsernamePasswordAuthenticationToken t = new UsernamePasswordAuthenticationToken(
                "usuario", null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_LIM")));
        t.setDetails(new DatosToken(28, 75, 1, "ROLE_LIM"));
        auth = t;
    }

    private static Coche coche(long id) {
        Coche c = new Coche();
        c.setIdCoche(id);
        c.setMarca("Toyota");
        c.setPlaca("917-SKP");
        c.setCodSucursal(20);
        return c;
    }

    @Test
    void laPantallaEsLaDireccionDeLaVista() {
        assertEquals("tacCoche/Coche", CocheController.PANTALLA,
                "tiene que ser el literal de tb_vista.direccion (y el de rutaCatalogoCoches en Flutter)");
    }

    @Test
    void sinLaPantallaLosTresEndpointsNiTocanElDao() {
        doThrow(new AccessDeniedException("sin pantalla")).when(pantallas).exigir(any(), eq(CocheController.PANTALLA));

        assertThrows(AccessDeniedException.class, () -> controlador.listar(null, auth));
        assertThrows(AccessDeniedException.class, () -> controlador.registrar(coche(0), auth));
        assertThrows(AccessDeniedException.class, () -> controlador.eliminar(coche(3), auth));

        verify(dao, never()).listarGestion(any());
        verify(dao, never()).registrar(any(), anyString());
    }

    @Test
    void listarConFilasDevuelve200() {
        List<Coche> filas = Arrays.asList(coche(1), coche(2));
        when(dao.listarGestion(any())).thenReturn(filas);

        ResponseEntity<ApiResponse<?>> r = controlador.listar(new Coche(), auth);

        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertSame(filas, r.getBody().getData());
        verify(pantallas).exigir(auth, CocheController.PANTALLA);
    }

    @Test
    void listarVacioDevuelve204() {
        when(dao.listarGestion(any())).thenReturn(Collections.emptyList());

        ResponseEntity<ApiResponse<?>> r = controlador.listar(null, auth);

        assertEquals(HttpStatus.NO_CONTENT, r.getStatusCode());
        assertNull(r.getBody().getData());
    }

    @Test
    void idCeroEsAltaYElAudUsuarioSaleDelTokenNoDelCuerpo() {
        when(dao.registrar(any(), anyString())).thenReturn(new RespuestaSp(0, "ok", 18L));
        Coche mb = coche(0);
        mb.setAudUsuario(999); // lo que intente mandar el cliente

        ResponseEntity<ApiResponse<?>> r = controlador.registrar(mb, auth);

        ArgumentCaptor<Coche> cap = ArgumentCaptor.forClass(Coche.class);
        verify(dao).registrar(cap.capture(), eq("I"));
        assertEquals(28, cap.getValue().getAudUsuario());
        assertEquals(HttpStatus.CREATED, r.getStatusCode());
        assertEquals(18L, r.getBody().getData());
    }

    @Test
    void idPositivoEsEdicion() {
        when(dao.registrar(any(), anyString())).thenReturn(new RespuestaSp(0, "ok", 5L));

        controlador.registrar(coche(5), auth);

        verify(dao).registrar(any(Coche.class), eq("U"));
    }

    @Test
    void unRechazoDelSpSePropagaParaQueElHandlerGlobalLoHagaCuatrocientos() {
        // SpHelper lanza SpBusinessException cuando error != 0; el handler global la hace 400.
        when(dao.registrar(any(), anyString())).thenThrow(new SpBusinessException("Ya existe un coche con la placa 917-SKP."));

        assertThrows(SpBusinessException.class, () -> controlador.registrar(coche(0), auth));
    }

    @Test
    void eliminarSoloMandaElIdYElUsuarioDelToken() {
        when(dao.registrar(any(), anyString())).thenReturn(new RespuestaSp(0, "eliminado", 0L));
        Coche mb = coche(3);
        mb.setAudUsuario(999);

        controlador.eliminar(mb, auth);

        ArgumentCaptor<Coche> cap = ArgumentCaptor.forClass(Coche.class);
        verify(dao).registrar(cap.capture(), eq("D"));
        assertEquals(3L, cap.getValue().getIdCoche());
        assertEquals(28, cap.getValue().getAudUsuario());
        assertNull(cap.getValue().getPlaca(), "una baja no lleva el resto del cuerpo");
    }
}
