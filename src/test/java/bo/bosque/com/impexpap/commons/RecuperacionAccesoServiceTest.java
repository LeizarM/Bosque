package bo.bosque.com.impexpap.commons;

import bo.bosque.com.impexpap.commons.RecuperacionAccesoService.CorreoNoEnviadoException;
import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.dao.ICodigoRecuperacion;
import bo.bosque.com.impexpap.dto.RecuperacionEnviadaDto;
import bo.bosque.com.impexpap.model.CodigoRecuperacion;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.mail.MailSendException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Recuperación de acceso: generación y envío del código, y validación antes de cambiar la contraseña. */
class RecuperacionAccesoServiceTest {

    private ICodigoRecuperacion dao;
    private CorreoRecuperacion correo;
    private SesionService sesiones;
    // Costo distinto del 10 de los códigos: así se ve que el código no usa el encoder de las contraseñas.
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private RecuperacionAccesoService servicio;

    @BeforeEach
    void setUp() {
        dao = mock(ICodigoRecuperacion.class);
        correo = mock(CorreoRecuperacion.class);
        sesiones = mock(SesionService.class);
        servicio = new RecuperacionAccesoService(dao, encoder, correo, sesiones);
    }

    private void registrado(String correos) {
        when(dao.solicitar(anyString(), anyString(), anyString())).thenReturn(new RespuestaSp(0, "", 10));
        CodigoRecuperacion c = new CodigoRecuperacion();
        c.setIdCodigo(10);
        c.setCodUsuario(5);
        c.setCorreo(correos);
        c.setMinutosVigencia(15);
        c.setSegundosParaReenviar(60);
        when(dao.obtenerPorId(10)).thenReturn(c);
    }

    private void vigente(String codigo) {
        CodigoRecuperacion c = new CodigoRecuperacion();
        c.setIdCodigo(20);
        c.setCodUsuario(5);
        c.setCodigoHash(encoder.encode(codigo));
        when(dao.obtenerVigente("jperez")).thenReturn(c);
        when(dao.reservarIntento(20)).thenReturn(new RespuestaSp(0, "Código incorrecto. Te quedan 4 intentos.", 0));
    }

    @Test
    void solicitarEnviaElMismoCodigoQueGuardaComoHash() {
        registrado("jperez@esppapel.com;juan.perez@impexpap.com");

        RecuperacionEnviadaDto r = servicio.solicitar("  jperez ", "10.0.0.1");

        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        verify(dao).solicitar(eq("jperez"), hash.capture(), eq("10.0.0.1"));
        ArgumentCaptor<String[]> para = ArgumentCaptor.forClass(String[].class);
        ArgumentCaptor<String> texto = ArgumentCaptor.forClass(String.class);
        verify(correo).enviar(para.capture(), eq(RecuperacionAccesoService.ASUNTO), texto.capture());

        assertArrayEquals(new String[]{"jperez@esppapel.com", "juan.perez@impexpap.com"}, para.getValue());
        Matcher m = Pattern.compile("es: (\\d{6})").matcher(texto.getValue());
        assertTrue(m.find(), "el correo lleva un código de 6 dígitos");
        assertTrue(encoder.matches(m.group(1), hash.getValue()), "lo guardado es el hash de ese mismo código");
        assertTrue(hash.getValue().startsWith("$2a$10$"), "el código usa su propio costo (10), no el del encoder de las contraseñas");
        assertEquals("j****z@esppapel.com, j****z@impexpap.com", r.getCorreo());
        assertEquals(15, r.getMinutosVigencia());
        assertEquals(60, r.getSegundosParaReenviar());
    }

    @Test
    void siElSpRechazaLaSolicitudNoSaleNingunCorreo() {
        when(dao.solicitar(anyString(), anyString(), anyString())).thenThrow(new SpBusinessException("Usuario inactivo."));

        assertThrows(SpBusinessException.class, () -> servicio.solicitar("jperez", "10.0.0.1"));
        verifyNoInteractions(correo);
    }

    @Test
    void siElCorreoNoSaleElCodigoSeDaDeBaja() {
        registrado("jperez@esppapel.com");
        doThrow(new MailSendException("SMTP caído")).when(correo).enviar(any(), anyString(), anyString());

        CorreoNoEnviadoException e = assertThrows(CorreoNoEnviadoException.class,
                () -> servicio.solicitar("jperez", "10.0.0.1"));

        assertEquals(RecuperacionAccesoService.MSG_ENVIO_FALLIDO, e.getMessage());
        verify(dao).eliminar(10);
    }

    @Test
    void unaConfiguracionIncompletaSeTrataComoCorreoQueNoSalio() {
        registrado("jperez@esppapel.com");
        doThrow(new SpBusinessException(CifradoConfiguracion.MSG_SIN_CLAVE)).when(correo).enviar(any(), anyString(), anyString());

        CorreoNoEnviadoException e = assertThrows(CorreoNoEnviadoException.class,
                () -> servicio.solicitar("jperez", "10.0.0.1"));

        assertEquals(RecuperacionAccesoService.MSG_ENVIO_FALLIDO, e.getMessage());
        verify(dao).eliminar(10);
    }

    @Test
    void siLaBajaTambienFallaIgualSaleElAvisoDeCorreo() {
        registrado("jperez@esppapel.com");
        doThrow(new MailSendException("SMTP caído")).when(correo).enviar(any(), anyString(), anyString());
        doThrow(new RuntimeException("sin conexión")).when(dao).eliminar(10);

        CorreoNoEnviadoException e = assertThrows(CorreoNoEnviadoException.class,
                () -> servicio.solicitar("jperez", "10.0.0.1"));

        assertEquals(RecuperacionAccesoService.MSG_ENVIO_FALLIDO, e.getMessage());
    }

    @Test
    void claveCortaOPorDefectoSeRechazaSinGastarIntentos() {
        SpBusinessException corta = assertThrows(SpBusinessException.class,
                () -> servicio.restablecer("jperez", "123456", "  abc12 ", "10.0.0.1"));
        SpBusinessException porDefecto = assertThrows(SpBusinessException.class,
                () -> servicio.restablecer("jperez", "123456", "123456789", "10.0.0.1"));

        assertEquals(RecuperacionAccesoService.MSG_CONTRASENA_CORTA, corta.getMessage());
        assertEquals(RecuperacionAccesoService.MSG_CONTRASENA_DEFECTO, porDefecto.getMessage());
        verifyNoInteractions(dao);
    }

    @Test
    void sinCodigoVigenteSePideUnoNuevo() {
        SpBusinessException e = assertThrows(SpBusinessException.class,
                () -> servicio.restablecer("jperez", "123456", "nueva-clave", "10.0.0.1"));

        assertEquals(RecuperacionAccesoService.MSG_SIN_CODIGO, e.getMessage());
    }

    @Test
    void codigoIncorrectoMuestraElAvisoDelIntentoReservado() {
        vigente("654321");

        SpBusinessException e = assertThrows(SpBusinessException.class,
                () -> servicio.restablecer("jperez", "123456", "nueva-clave", "10.0.0.1"));

        assertEquals("Código incorrecto. Te quedan 4 intentos.", e.getMessage());
        verify(dao, never()).restablecer(anyLong(), anyString(), anyString());
        verifyNoInteractions(sesiones);
    }

    @Test
    void sinIntentosElSpRechazaAntesDeComparar() {
        vigente("654321");
        when(dao.reservarIntento(20)).thenThrow(new SpBusinessException("Ya no quedan intentos para este código. Pide uno nuevo."));

        SpBusinessException e = assertThrows(SpBusinessException.class,
                () -> servicio.restablecer("jperez", "654321", "nueva-clave", "10.0.0.1"));

        assertEquals("Ya no quedan intentos para este código. Pide uno nuevo.", e.getMessage());
        verify(dao, never()).restablecer(anyLong(), anyString(), anyString());
    }

    @Test
    void codigoCorrectoReservaElIntentoYGuardaElHashDeLaClaveRecortada() {
        vigente("654321");

        servicio.restablecer(" jperez", " 654321 ", "  nueva-clave  ", "10.0.0.1");

        InOrder orden = inOrder(dao);
        orden.verify(dao).reservarIntento(20);
        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        orden.verify(dao).restablecer(eq(20L), hash.capture(), eq("10.0.0.1"));
        assertTrue(encoder.matches("nueva-clave", hash.getValue()), "el login recorta la clave, así que se guarda recortada");
    }

    @Test
    void alRestablecerSeCierranLasSesionesAbiertasConLaClaveAnterior() {
        vigente("654321");

        servicio.restablecer("jperez", "654321", "nueva-clave", "10.0.0.1");

        verify(sesiones).cerrarTodas(5, SesionService.MOTIVO_CLAVE, null);
    }

    @Test
    void enmascarar() {
        assertEquals("a****@esppapel.com", RecuperacionAccesoService.enmascarar("a@esppapel.com"));
        assertEquals("a****@esppapel.com", RecuperacionAccesoService.enmascarar("ab@esppapel.com"));
        assertEquals("m****s@impexpap.com", RecuperacionAccesoService.enmascarar("mjaimes@impexpap.com"));
    }
}
