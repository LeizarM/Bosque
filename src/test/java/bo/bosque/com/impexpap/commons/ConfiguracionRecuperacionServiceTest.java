package bo.bosque.com.impexpap.commons;

import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.dao.IConfiguracionRecuperacion;
import bo.bosque.com.impexpap.model.ConfiguracionRecuperacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Parámetros de la recuperación: el secreto no sale hacia la pantalla, se descifra para enviar y se cifra al guardar. */
class ConfiguracionRecuperacionServiceTest {

    private IConfiguracionRecuperacion dao;
    private CifradoConfiguracion cifrado;
    private ConfiguracionRecuperacionService servicio;

    @BeforeEach
    void setUp() {
        dao = mock(IConfiguracionRecuperacion.class);
        byte[] b = new byte[32];
        for (int i = 0; i < 32; i++) b[i] = (byte) i;
        cifrado = new CifradoConfiguracion(Base64.getEncoder().encodeToString(b));
        servicio = new ConfiguracionRecuperacionService(dao, cifrado);
    }

    private static ConfiguracionRecuperacion fila(String clave, String tipo, String valor) {
        ConfiguracionRecuperacion f = new ConfiguracionRecuperacion();
        f.setClave(clave);
        f.setTipo(tipo);
        f.setValor(valor);
        return f;
    }

    @Test
    void laPantallaNoRecibeElSecretoSoloSiEstaCargado() {
        when(dao.listar()).thenReturn(Arrays.asList(
                fila("remitente", "correo", "bosque@esppapel.com"),
                fila("smtpClave", "secreto", cifrado.cifrar("la-clave"))));

        List<ConfiguracionRecuperacion> filas = servicio.paraPantalla();

        assertEquals("bosque@esppapel.com", filas.get(0).getValor());
        assertNull(filas.get(1).getValor());
        assertTrue(filas.get(1).isConfigurado());
    }

    @Test
    void unSecretoVacioFiguraComoNoCargado() {
        when(dao.listar()).thenReturn(Arrays.asList(fila("smtpClave", "secreto", null)));

        assertFalse(servicio.paraPantalla().get(0).isConfigurado());
    }

    @Test
    void losValoresVigentesTraenElSecretoDescifradoYOmitenLosVacios() {
        when(dao.listar()).thenReturn(Arrays.asList(
                fila("smtpServidor", "texto", "mail.esppapel.com"),
                fila("smtpClave", "secreto", cifrado.cifrar("la-clave")),
                fila("otro", "texto", "  ")));

        Map<String, String> v = servicio.vigente();

        assertEquals("mail.esppapel.com", v.get("smtpServidor"));
        assertEquals("la-clave", v.get("smtpClave"));
        assertFalse(v.containsKey("otro"));
    }

    @Test
    void guardarUnSecretoLoCifraSinRecortarlo() {
        when(dao.obtener("smtpClave")).thenReturn(fila("smtpClave", "secreto", null));

        servicio.guardar("smtpClave", " con espacios ", 28);

        ArgumentCaptor<String> guardado = ArgumentCaptor.forClass(String.class);
        verify(dao).actualizar(eq("smtpClave"), guardado.capture(), eq(28L));
        assertEquals(" con espacios ", cifrado.descifrar(guardado.getValue()));
    }

    @Test
    void unSecretoQueNoEntraCifradoEnLaColumnaNoSeGuarda() {
        when(dao.obtener("smtpClave")).thenReturn(fila("smtpClave", "secreto", null));
        StringBuilder larga = new StringBuilder();
        for (int i = 0; i < 400; i++) larga.append('x');

        SpBusinessException e = assertThrows(SpBusinessException.class,
                () -> servicio.guardar("smtpClave", larga.toString(), 28));

        assertEquals(ConfiguracionRecuperacionService.MSG_SECRETO_LARGO, e.getMessage());
        verify(dao, never()).actualizar(anyString(), anyString(), anyLong());
    }

    @Test
    void guardarUnValorComunLoMandaTalCualAlSp() {
        when(dao.obtener("smtpPuerto")).thenReturn(fila("smtpPuerto", "numero", "587"));

        servicio.guardar("smtpPuerto", "465", 28);

        verify(dao).actualizar("smtpPuerto", "465", 28L);
    }

    @Test
    void claveInexistenteOValorVacioNoLleganAlSp() {
        when(dao.obtener("smtpClave")).thenReturn(fila("smtpClave", "secreto", null));

        assertEquals(ConfiguracionRecuperacionService.MSG_NO_EXISTE,
                assertThrows(SpBusinessException.class, () -> servicio.guardar("noExiste", "x", 28)).getMessage());
        assertEquals(ConfiguracionRecuperacionService.MSG_SIN_VALOR,
                assertThrows(SpBusinessException.class, () -> servicio.guardar("smtpClave", "", 28)).getMessage());
        verify(dao, never()).actualizar(anyString(), anyString(), anyLong());
    }
}
