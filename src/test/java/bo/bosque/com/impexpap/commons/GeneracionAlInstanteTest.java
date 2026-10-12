package bo.bosque.com.impexpap.commons;

import bo.bosque.com.impexpap.dao.IGeneradorTareasRutinarias;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Al entrar a Mis tareas se generan las de hoy de esa persona y nada más
 * (Marcelo, 2026-10-09; archivo SQL 90).
 */
class GeneracionAlInstanteTest {

    private IGeneradorTareasRutinarias generador;
    private GeneracionAlInstante generacion;

    @BeforeEach
    void preparar() {
        generador = mock(IGeneradorTareasRutinarias.class);
        when(generador.generarPara(anyLong())).thenReturn(new RespuestaSp(0, "ok", 1));
        generacion = new GeneracionAlInstante(generador);
    }

    @Test
    @DisplayName("genera solo para la persona que entra, no para todos")
    void soloParaEsaPersona() {
        generacion.generarPara(56L);

        verify(generador).generarPara(56L);
        verify(generador, never()).generarOcurrencias();
    }

    @Test
    @DisplayName("sin empleado en el token no genera nada")
    void sinEmpleadoNoGenera() {
        generacion.generarPara(null);
        generacion.generarPara(0L);

        verify(generador, never()).generarPara(anyLong());
    }

    @Test
    @DisplayName("si el generador falla, la lista sale igual")
    void unaFallaNoCortaLaLista() {
        when(generador.generarPara(anyLong())).thenThrow(new RuntimeException("la base no responde"));

        assertDoesNotThrow(() -> generacion.generarPara(56L));
    }
}
