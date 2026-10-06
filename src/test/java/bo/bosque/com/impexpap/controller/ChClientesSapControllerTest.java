package bo.bosque.com.impexpap.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import bo.bosque.com.impexpap.commons.ClientesSapService;
import bo.bosque.com.impexpap.security.jwt.DatosToken;
import bo.bosque.com.impexpap.utils.ApiResponse;

/**
 * "ACTUALIZAR DATOS SAP": la ruta (el frontend la tiene escrita como constante: {@code /cheque/clientes/actualizar-sap}),
 * el verbo, la seguridad de clase y la forma de la respuesta.
 */
class ChClientesSapControllerTest {

    private static Authentication auth() {
        UsernamePasswordAuthenticationToken t = new UsernamePasswordAuthenticationToken(
                "usuario", null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_LIM")));
        t.setDetails(new DatosToken(7, 3, 1, "lim"));
        return t;
    }

    @Test
    @DisplayName("la ruta completa es POST /cheque/clientes/actualizar-sap y es la unica de este controlador")
    void ruta() {
        assertEquals("/cheque/clientes", ChClientesSapController.class.getAnnotation(RequestMapping.class).value()[0]);

        Set<String> rutas = new TreeSet<>();
        for (Method m : ChClientesSapController.class.getDeclaredMethods()) {
            PostMapping pm = m.getAnnotation(PostMapping.class);
            if (pm != null) rutas.addAll(Arrays.asList(pm.value()));
        }
        assertEquals(new TreeSet<>(Collections.singletonList("/actualizar-sap")), rutas);
    }

    @Test
    @DisplayName("exige ROLE_ADM o ROLE_LIM, como el resto del modulo")
    void seguridad() {
        PreAuthorize pa = ChClientesSapController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(pa);
        assertTrue(pa.value().contains("ROLE_ADM") && pa.value().contains("ROLE_LIM"), pa.value());
    }

    @Test
    @DisplayName("responde 200 con la frase para el usuario en message y data nulo (no se sabe cuantos clientes trajo)")
    void respuesta() {
        ClientesSapService servicio = mock(ClientesSapService.class);
        Authentication auth = auth();

        ResponseEntity<ApiResponse<?>> r = new ChClientesSapController(servicio).actualizarDesdeSap(auth);

        verify(servicio).actualizarDesdeSap(auth);
        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertNull(r.getBody().getData());
        assertEquals(200, r.getBody().getStatus());
        assertEquals("Clientes actualizados desde SAP. Los cheques de los clientes que antes no estaban cargados ya aparecen en la lista.",
                r.getBody().getMessage());
    }
}
