package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.commons.AccesoModuloHelper;
import bo.bosque.com.impexpap.dao.IAfiliacionSeguro;
import bo.bosque.com.impexpap.dao.IArea;
import bo.bosque.com.impexpap.dao.ICargo;
import bo.bosque.com.impexpap.dao.ICargoSucursal;
import bo.bosque.com.impexpap.dao.ICiudad;
import bo.bosque.com.impexpap.dao.IEducacion;
import bo.bosque.com.impexpap.dao.IEmail;
import bo.bosque.com.impexpap.dao.IEmpleado;
import bo.bosque.com.impexpap.dao.IEmpleadoCargo;
import bo.bosque.com.impexpap.dao.IEmpresa;
import bo.bosque.com.impexpap.dao.IExperienciaLaboral;
import bo.bosque.com.impexpap.dao.IFormacion;
import bo.bosque.com.impexpap.dao.ILicencia;
import bo.bosque.com.impexpap.dao.INivelJerarquico;
import bo.bosque.com.impexpap.dao.INroCuentaBancaria;
import bo.bosque.com.impexpap.dao.IPais;
import bo.bosque.com.impexpap.dao.IPersona;
import bo.bosque.com.impexpap.dao.IRelEmpEmpr;
import bo.bosque.com.impexpap.dao.ISeguro;
import bo.bosque.com.impexpap.dao.ISucursal;
import bo.bosque.com.impexpap.dao.ITelefono;
import bo.bosque.com.impexpap.dao.IZona;
import bo.bosque.com.impexpap.model.Empleado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <b>Ancla la forma de {@code POST /rrhh/obtenerLstEmpleados}.</b> No prueba una función nueva:
 * prueba que una vieja no se "arregle".
 *
 * <h3>Qué se está protegiendo</h3>
 * Ese endpoint es la <b>excepción documentada al envelope</b> del backend: devuelve un
 * {@code List<Empleado>} <b>crudo</b> —sin {@code ApiResponse}— y, sin resultados, responde
 * <b>200 con {@code []}</b> en vez del 204 que devuelven todos los demás. Del lado Flutter
 * funciona porque {@code BaseApiRepository.postAndReturnList} tiene la rama
 * {@code raw is List ? raw : (raw as Map)['data']}.
 *
 * <p>Es la <b>puerta de entrada</b> del módulo de RR.HH. y de la consola de Permisos y
 * Vacaciones: es el buscador con el que se elige al empleado. Si alguien lo "normaliza" al
 * envelope por consistencia —un cambio de dos líneas que parece una mejora— el buscador deja de
 * traer resultados <b>sin ningún error</b>, ni en el servidor ni en el cliente. Este test es lo
 * que hace ruido antes de que eso llegue a producción.
 *
 * <p>Se arma con {@code standaloneSetup} y no con {@code @WebMvcTest} a propósito: acá no se
 * prueba seguridad ni serialización de errores, sólo la forma de la respuesta, y levantar un
 * contexto para eso costaría más de lo que aporta.
 *
 * <p><b>Las peticiones mandan {@code Accept: application/json}</b> y no es decoración: con JAXB
 * en el classpath y sin ese header, la negociación de contenido elige <b>XML</b> y el endpoint
 * devuelve un {@code <List>} de 6 KB. Vale saberlo: cualquier cliente que no mande {@code Accept}
 * recibe eso mismo en producción.
 */
class BuscadorEmpleadosShapeTest {

    private static final String BUSCADOR = "/rrhh/obtenerLstEmpleados";
    private static final String FILTRO   = "{\"search\":\"perez\",\"esActivo\":1,"
                                         + "\"pageNumber\":1,\"pageSize\":10,\"codEmpresa\":1}";

    private IEmpleado empDao;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        empDao = mock(IEmpleado.class);
        RrhhController controller = new RrhhController(
                null, mock(IEmail.class), mock(ITelefono.class), empDao,
                mock(IPersona.class), mock(IExperienciaLaboral.class), mock(IFormacion.class),
                mock(ILicencia.class), mock(IRelEmpEmpr.class), mock(ICiudad.class),
                mock(IEmpleadoCargo.class), mock(IPais.class), mock(IZona.class),
                mock(ISucursal.class), mock(ICargoSucursal.class), mock(IEmpresa.class),
                mock(ICargo.class), mock(INivelJerarquico.class), mock(IEducacion.class),
                mock(INroCuentaBancaria.class), mock(ISeguro.class), mock(IAfiliacionSeguro.class),
                mock(IArea.class), mock(AccesoModuloHelper.class));
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("con resultados: array CRUDO en la raíz, sin envelope")
    void listaCruda() throws Exception {
        Empleado e = new Empleado();
        e.setCodEmpleado(130);
        when(empDao.obtenerLstEmpleados(any(), any(), anyInt(), anyInt(), any()))
                .thenReturn(new ArrayList<>(Collections.singletonList(e)));

        mvc.perform(post(BUSCADOR).contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON).content(FILTRO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].codEmpleado").value(130))
                // Si aparecen estos dos, alguien envolvió la respuesta y rompió el buscador.
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.message").doesNotExist());
    }

    @Test
    @DisplayName("sin resultados: 200 con [] — NO 204, que es lo que devuelve el resto del backend")
    void vacioDevuelve200() throws Exception {
        when(empDao.obtenerLstEmpleados(any(), any(), anyInt(), anyInt(), any()))
                .thenReturn(new ArrayList<>());

        mvc.perform(post(BUSCADOR).contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON).content(FILTRO))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }
}
