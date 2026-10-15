package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.commons.AccesoModuloHelper;
import bo.bosque.com.impexpap.commons.GeneracionAlInstante;
import bo.bosque.com.impexpap.config.GlobalExceptionHandler;
import bo.bosque.com.impexpap.dao.IBitTareaRuti;
import bo.bosque.com.impexpap.model.BitTareaRuti;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Mis tareas (archivo SQL 104): el endpoint devuelve solo las de quien entra, por el token,
 * con el mismo pedido que hacen la app actual y la anterior (POST sin body). Antes devolvía
 * la tabla entera y por LTE no llegaba en 30 s.
 */
class MisTareasSoloLoPropioTest {

    private static final String URL = "/tareas-rutinarias/obtener-bit-tarea-ruti";

    private IBitTareaRuti dao;
    private AccesoModuloHelper acceso;
    private GeneracionAlInstante generacion;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        dao = mock(IBitTareaRuti.class);
        acceso = mock(AccesoModuloHelper.class);
        generacion = mock(GeneracionAlInstante.class);
        TareasRutinariasController controlador = mock(TareasRutinariasController.class, CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(controlador, "bitTareaRutiDao", dao);
        ReflectionTestUtils.setField(controlador, "accesoModuloHelper", acceso);
        ReflectionTestUtils.setField(controlador, "generacionAlInstante", generacion);
        mvc = MockMvcBuilders.standaloneSetup(controlador)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    private static BitTareaRuti fila(long id) {
        BitTareaRuti b = new BitTareaRuti();
        b.setIdBitTarea(id);
        b.setCodEmpleado(24L);
        return b;
    }

    @Test
    void sinBodyTraeSoloLasDeQuienEntra() throws Exception {
        when(acceso.codEmpleadoDelToken(any())).thenReturn(24L);
        when(dao.misTareas(24L)).thenReturn(Collections.singletonList(fila(176128L)));

        mvc.perform(post(URL).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].idBitTarea").value(176128));

        verify(generacion).generarPara(24L);
        verify(dao).misTareas(24L);
        verify(dao, never()).listar(any());
    }

    @Test
    void otroCodEmpleadoEnElBodyNoCambiaDeQuienSon() throws Exception {
        when(acceso.codEmpleadoDelToken(any())).thenReturn(24L);
        when(dao.misTareas(24L)).thenReturn(Collections.singletonList(fila(176128L)));

        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{\"codEmpleado\": 999}"))
                .andExpect(status().isOk());

        verify(dao).misTareas(24L);
        verify(dao, never()).misTareas(999L);
        verify(dao, never()).listar(any());
    }

    @Test
    void sinEmpleadoEnElTokenNoTraeNada() throws Exception {
        when(acceso.codEmpleadoDelToken(any())).thenReturn(null);

        mvc.perform(post(URL)).andExpect(status().isNoContent());

        verify(dao, never()).misTareas(anyLong());
        verify(dao, never()).listar(any());
    }
}
