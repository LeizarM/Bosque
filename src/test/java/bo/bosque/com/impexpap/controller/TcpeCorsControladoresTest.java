package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.commons.TcpeClaveService;
import bo.bosque.com.impexpap.dao.ITcpeAcceso;
import bo.bosque.com.impexpap.dao.ITcpeRendir;
import bo.bosque.com.impexpap.security.jwt.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CORS del módulo: cada {@code Tcpe*Controller} declara {@code @CrossOrigin} con el placeholder
 * {@code tcpe.cors.origenes} (por defecto {@code *}) y solo POST. El preflight prueba que Spring 5.3 lo resuelve.
 */
class TcpeCorsControladoresTest {

    private static final String ORIGENES = "${tcpe.cors.origenes:*}";
    private static final String WEB = "https://web.bosque.bo";

    @Test
    @DisplayName("los 12 controladores Tcpe* llevan @CrossOrigin(originPatterns = placeholder, methods = POST)")
    void todosConCrossOrigin() throws Exception {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        Set<String> nombres = new TreeSet<>();
        for (BeanDefinition bd : scanner.findCandidateComponents("bo.bosque.com.impexpap.controller")) {
            Class<?> c = Class.forName(bd.getBeanClassName());
            if (!c.getSimpleName().startsWith("Tcpe")) continue;
            nombres.add(c.getSimpleName());
            CrossOrigin co = c.getAnnotation(CrossOrigin.class);
            assertNotNull(co, c.getSimpleName() + " no tiene @CrossOrigin");
            assertArrayEquals(new String[] { ORIGENES }, co.originPatterns(), c.getSimpleName());
            assertArrayEquals(new RequestMethod[] { RequestMethod.POST }, co.methods(), c.getSimpleName());
            assertEquals(0, co.origins().length, c.getSimpleName() + ": origins y originPatterns no van juntos");
        }
        assertEquals(new TreeSet<>(Arrays.asList(
                "TcpeBancoController", "TcpeCatalogoBancoController", "TcpeCatalogoExamenController",
                "TcpeCatalogoPostulacionController", "TcpeConfigController", "TcpeExamenPublicoAuthController",
                "TcpeExamenPublicoController", "TcpeLotesController", "TcpeMisExamenesController",
                "TcpeMonitorController", "TcpeResultadosController", "TcpeUsuariosController")), nombres);
    }

    /** MVC real (con el adaptador del preflight) y solo el controlador público; la propiedad es opcional. */
    @Configuration
    @EnableWebMvc
    static class Mvc {
        @Bean
        TcpeExamenPublicoAuthController controlador() {
            return new TcpeExamenPublicoAuthController(mock(ITcpeAcceso.class), mock(ITcpeRendir.class),
                    mock(TcpeClaveService.class), mock(JwtProvider.class));
        }
    }

    private static MockMvc publico(String origenes) {
        AnnotationConfigWebApplicationContext ctx = new AnnotationConfigWebApplicationContext();
        ctx.setServletContext(new MockServletContext());
        if (origenes != null) {
            ctx.getEnvironment().getPropertySources().addFirst(new MapPropertySource("prueba",
                    Collections.<String, Object>singletonMap("tcpe.cors.origenes", origenes)));
        }
        ctx.register(Mvc.class);
        ctx.refresh();
        return MockMvcBuilders.webAppContextSetup(ctx).build();
    }

    @Test
    @DisplayName("sin la propiedad, el valor por defecto (*) deja pasar el preflight de cualquier origen")
    void porDefectoCualquierOrigen() throws Exception {
        MockMvc mvc = publico(null);
        mvc.perform(options("/examen-publico/auth/login")
                        .header("Origin", WEB)
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", WEB));
    }

    @Test
    @DisplayName("con tcpe.cors.origenes solo pasa ese origen, y nunca un método distinto de POST")
    void conPropiedadSoloEseOrigen() throws Exception {
        MockMvc mvc = publico(WEB);
        mvc.perform(options("/examen-publico/auth/login")
                        .header("Origin", WEB)
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", WEB));
        mvc.perform(options("/examen-publico/auth/login")
                        .header("Origin", "https://otro.ejemplo")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
        mvc.perform(options("/examen-publico/auth/login")
                        .header("Origin", WEB)
                        .header("Access-Control-Request-Method", "DELETE"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
