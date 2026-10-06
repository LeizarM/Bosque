package bo.bosque.com.impexpap.commons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import bo.bosque.com.impexpap.config.SinPermisoException;
import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.dao.IChPostergacion;
import bo.bosque.com.impexpap.dto.PostergacionDto;
import bo.bosque.com.impexpap.dto.PostergacionRequest;
import bo.bosque.com.impexpap.model.ChPostergacion;
import bo.bosque.com.impexpap.utils.RespuestaSp;

/**
 * El panel de postergaciones con el DAO simulado y una carpeta de PDF real temporal: reglas (permisos, orden de
 * savePostePostrgcn, observacion, pertenencia al cheque) y mensajes, no SQL.
 */
class PostergacionChequeServiceTest extends PanelesChequeBase {

    @TempDir
    Path carpeta;

    private IChPostergacion postergaciones;
    private PostergacionPdfService pdfs;
    private PostergacionChequeService servicio;

    @BeforeEach
    void preparar() {
        postergaciones = mock(IChPostergacion.class);
        pdfs = new PostergacionPdfService(postergaciones, acceso, carpeta.toString(), AlmacenPdf.RELOJ);
        servicio = new PostergacionChequeService(postergaciones, pdfs, acceso);
        when(postergaciones.registrar(any())).thenReturn(new RespuestaSp(0, "", 41L));
        when(postergaciones.eliminar(anyInt(), anyInt())).thenReturn(new RespuestaSp(0, "", 40L));
    }

    private static PostergacionRequest pr(Date fecha, String obs) {
        PostergacionRequest r = new PostergacionRequest();
        r.setCodCheque(COD);
        r.setFecha(fecha);
        r.setObservacion(obs);
        return r;
    }

    private static PostergacionRequest valida() {
        return pr(d(2025, 10, 30), "cambio de cheque 277");
    }

    private static PostergacionDto fila(int cod, int codCheque) {
        PostergacionDto f = new PostergacionDto();
        f.setCodPostergacion(cod);
        f.setCodCheque(codCheque);
        f.setFecha(d(2025, 10, 30));
        f.setObservacion("cambio de cheque 277");
        f.setNombreArchivo("");
        return f;
    }

    private static ChPostergacion existente(int cod, int codCheque) {
        ChPostergacion p = new ChPostergacion();
        p.setCodPostergacion(cod);
        p.setCodCheque(codCheque);
        return p;
    }

    private static String mensaje(Runnable accion) {
        return assertThrows(SpBusinessException.class, accion::run).getMessage();
    }

    // ------------------------------------------------------------------ listar

    @Test
    @DisplayName("listar: las postergaciones del cheque con tienePdf true/false segun exista <codPostergacion>.pdf; sin boton propio (solo btnDetalleCH)")
    void listarConTienePdf() throws Exception {
        Files.write(carpeta.resolve("40.pdf"), "%PDF-1.4".getBytes(StandardCharsets.ISO_8859_1));
        when(postergaciones.listarPorCheque(COD)).thenReturn(Arrays.asList(fila(38, COD), fila(40, COD)));

        List<PostergacionDto> r = servicio.listar(usuario, COD);

        assertEquals(2, r.size());
        assertEquals(Boolean.FALSE, r.get(0).getTienePdf());
        assertEquals(Boolean.TRUE, r.get(1).getTienePdf());
        verify(chequeService).exigirSucursalLectura(usuario, SUCURSAL);
        verificarSoloSePidioElDetalle(usuario);
    }

    @Test
    @DisplayName("listar: una carpeta con el mismo nombre que un PDF (40.pdf) no cuenta como documento")
    void listarCarpetaConNombreDePdf() throws Exception {
        Files.createDirectory(carpeta.resolve("40.pdf"));
        when(postergaciones.listarPorCheque(COD)).thenReturn(Collections.singletonList(fila(40, COD)));

        assertEquals(Boolean.FALSE, servicio.listar(usuario, COD).get(0).getTienePdf());
    }

    @Test
    @DisplayName("listar: con la carpeta de PDF en blanco, que es un archivo o con una ruta imposible, tienePdf es null y el listado NO falla; si la carpeta aun no existe, tienePdf es false")
    void listarSinCarpeta() throws Exception {
        when(postergaciones.listarPorCheque(COD)).thenReturn(Arrays.asList(fila(38, COD), fila(40, COD)));
        Path archivo = Files.createFile(carpeta.resolve("soy-un-archivo"));

        for (String ruta : new String[] {"", "   ", null, archivo.toString(), "ruta\u0000mala"}) {
            PostergacionChequeService s = new PostergacionChequeService(postergaciones,
                    new PostergacionPdfService(postergaciones, acceso, ruta, AlmacenPdf.RELOJ), acceso);
            List<PostergacionDto> r = s.listar(usuario, COD);
            assertEquals(2, r.size(), String.valueOf(ruta));
            assertNull(r.get(0).getTienePdf(), String.valueOf(ruta));
            assertNull(r.get(1).getTienePdf(), String.valueOf(ruta));
        }

        // La carpeta por defecto (uploads/postergacionCheques) no existe hasta que se sube el primer PDF: ninguna tiene PDF.
        PostergacionChequeService sinCarpetaAun = new PostergacionChequeService(postergaciones,
                new PostergacionPdfService(postergaciones, acceso, carpeta.resolve("no-existe").toString(), AlmacenPdf.RELOJ), acceso);
        List<PostergacionDto> r = sinCarpetaAun.listar(usuario, COD);
        assertEquals(Boolean.FALSE, r.get(0).getTienePdf());
        assertEquals(Boolean.FALSE, r.get(1).getTienePdf());
        assertFalse(Files.exists(carpeta.resolve("no-existe")), "listar no crea la carpeta");
    }

    @Test
    @DisplayName("listar: cheque inexistente, sin codigo o de una sucursal que no se ve: no consulta la tabla")
    void listarSinAcceso() {
        assertEquals(MensajesCheque.chequeNoEncontrado(9), mensaje(() -> servicio.listar(usuario, 9)));
        assertEquals(MensajesCheque.FALTA_CHEQUE_DEL_PANEL, mensaje(() -> servicio.listar(usuario, null)));
        doThrow(new SinPermisoException(MensajesCheque.SOLO_SU_SUCURSAL)).when(chequeService).exigirSucursalLectura(any(), any());
        assertThrows(SinPermisoException.class, () -> servicio.listar(usuario, COD));
        verify(postergaciones, never()).listarPorCheque(anyInt());
    }

    // ------------------------------------------------------------------ registrar

    @Test
    @DisplayName("registrar: guarda con el cheque y el usuario del TOKEN y devuelve el codPostergacion nuevo; orden: btnNuevoNRCH, btnDetalleCH, cheque, sucursal visible, sucursal de escritura")
    void registrar() {
        conBotones("btnNuevoNRCH");

        assertEquals(41L, servicio.registrar(usuario, valida()));

        InOrder o = inOrder(helper, cheques, chequeService, postergaciones);
        o.verify(helper).exigirBoton(usuario, ChequeService.VISTA_CHEQUES, "btnNuevoNRCH");
        o.verify(helper).exigirBoton(usuario, ChequeService.VISTA_CHEQUES, "btnDetalleCH");
        o.verify(cheques).obtener(COD);
        o.verify(chequeService).exigirSucursalLectura(usuario, SUCURSAL);
        o.verify(chequeService).exigirSucursalEscritura(usuario, SUCURSAL);
        ArgumentCaptor<ChPostergacion> c = ArgumentCaptor.forClass(ChPostergacion.class);
        o.verify(postergaciones).registrar(c.capture());
        ChPostergacion g = c.getValue();
        assertEquals(Integer.valueOf(COD), g.getCodCheque());
        assertEquals(d(2025, 10, 30), g.getFecha());
        assertEquals("cambio de cheque 277", g.getObservacion());
        assertEquals(Integer.valueOf(7), g.getAudUsuario(), "el usuario de auditoria sale del token (codUsuario 7)");
    }

    @Test
    @DisplayName("registrar sin btnNuevoNRCH: 403 que dice cual boton falta y no se toca nada")
    void registrarSinBoton() {
        SinPermisoException e = assertThrows(SinPermisoException.class, () -> servicio.registrar(usuario, valida()));

        assertEquals(MensajesCheque.sinPermiso("btnNuevoNRCH"), e.getMessage());
        verify(cheques, never()).obtener(anyInt());
        verify(postergaciones, never()).registrar(any());
    }

    @Test
    @DisplayName("registrar: sin cuerpo, sin cheque, cheque inexistente o de una sucursal que no se ve")
    void registrarCheque() {
        conBotones("btnNuevoNRCH");
        assertEquals(MensajesCheque.SIN_DATOS, mensaje(() -> servicio.registrar(usuario, null)));
        PostergacionRequest sinCheque = valida();
        sinCheque.setCodCheque(null);
        assertEquals(MensajesCheque.FALTA_CHEQUE_DEL_PANEL, mensaje(() -> servicio.registrar(usuario, sinCheque)));
        PostergacionRequest otro = valida();
        otro.setCodCheque(12);
        assertEquals(MensajesCheque.chequeNoEncontrado(12), mensaje(() -> servicio.registrar(usuario, otro)));
        doThrow(new SinPermisoException(MensajesCheque.SOLO_SU_SUCURSAL)).when(chequeService).exigirSucursalLectura(any(), any());
        assertThrows(SinPermisoException.class, () -> servicio.registrar(usuario, valida()));
        verify(postergaciones, never()).registrar(any());
    }

    @Test
    @DisplayName("registrar: la fecha es obligatoria y se informa ANTES del permiso de sucursal; la observacion va DESPUES (orden de savePostePostrgcn)")
    void registrarOrden() {
        conBotones("btnNuevoNRCH");
        doThrow(new SpBusinessException(MensajesCheque.SUCURSAL_AJENA)).when(chequeService).exigirSucursalEscritura(any(), any());

        assertEquals(MensajesCheque.FALTA_FECHA_POSTERGACION, mensaje(() -> servicio.registrar(usuario, pr(null, "abc"))));
        // con fecha y sin permiso de sucursal, el error es el de la sucursal aunque la observacion este mal
        assertEquals(MensajesCheque.SUCURSAL_AJENA, mensaje(() -> servicio.registrar(usuario, pr(d(2026, 1, 1), "ab"))));
        verify(postergaciones, never()).registrar(any());
    }

    @Test
    @DisplayName("registrar: observacion de mas de 2 caracteres (2 no, 3 si; tal cual llega) y hasta 200 (201 se rechaza en vez de truncar)")
    void registrarObservacion() {
        conBotones("btnNuevoNRCH");

        assertEquals(MensajesCheque.FALTA_OBSERVACION_POSTERGACION, mensaje(() -> servicio.registrar(usuario, pr(d(2026, 1, 1), null))));
        assertEquals(MensajesCheque.FALTA_OBSERVACION_POSTERGACION, mensaje(() -> servicio.registrar(usuario, pr(d(2026, 1, 1), ""))));
        assertEquals(MensajesCheque.observacionPostergacionCorta("ab", 2), mensaje(() -> servicio.registrar(usuario, pr(d(2026, 1, 1), "ab"))));
        assertEquals(41L, servicio.registrar(usuario, pr(d(2026, 1, 1), "abc")));
        assertEquals(41L, servicio.registrar(usuario, pr(d(2026, 1, 1), "   ")), "3 espacios cumplen length() > 2 (se conserva)");
        String doscientos = String.join("", Collections.nCopies(200, "x"));
        assertEquals(41L, servicio.registrar(usuario, pr(d(2026, 1, 1), doscientos)));
        assertEquals(MensajesCheque.observacionPostergacionLarga(201, 200),
                mensaje(() -> servicio.registrar(usuario, pr(d(2026, 1, 1), doscientos + "x"))));
    }

    @Test
    @DisplayName("registrar: sin regex, los acentos, saltos de linea y simbolos de los datos reales se aceptan tal cual")
    void registrarSinRegex() {
        conBotones("btnNuevoNRCH");
        String real = "cliente solicitó postergación hasta el 04 de marzo (Nº 12)\nautorizada por gerencia - 100%";

        servicio.registrar(usuario, pr(d(2026, 1, 1), real));

        ArgumentCaptor<ChPostergacion> c = ArgumentCaptor.forClass(ChPostergacion.class);
        verify(postergaciones).registrar(c.capture());
        assertEquals(real, c.getValue().getObservacion());
    }

    @Test
    @DisplayName("registrar no mira si el cheque esta cerrado (el boton 'Nuevo' no dependia del estado)")
    void registrarEnChequeCerrado() {
        cheque(chequeDe(COD, SUCURSAL, "CER"));
        conBotones("btnNuevoNRCH");

        assertEquals(41L, servicio.registrar(usuario, valida()));
    }

    // ------------------------------------------------------------------ eliminar

    @Test
    @DisplayName("eliminar NO tiene boton propio (como hoy): con solo btnDetalleCH (el del panel) elimina; devuelve el codPostergacion; el usuario sale del token")
    void eliminar() {
        when(postergaciones.obtener(40)).thenReturn(existente(40, COD));
        PostergacionRequest r = pr(null, null);
        r.setCodPostergacion(40);

        assertEquals(40L, servicio.eliminar(usuario, r));

        InOrder o = inOrder(cheques, chequeService, postergaciones);
        o.verify(cheques).obtener(COD);
        o.verify(chequeService).exigirSucursalLectura(usuario, SUCURSAL);
        o.verify(postergaciones).obtener(40);
        o.verify(postergaciones).eliminar(40, 7);
        verificarSoloSePidioElDetalle(usuario);
        verify(chequeService, never()).exigirSucursalEscritura(any(), any());
    }

    @Test
    @DisplayName("eliminar: la postergacion tiene que ser de ESE cheque; si es de otro, error con los datos y NO se borra")
    void eliminarDeOtroCheque() {
        when(postergaciones.obtener(40)).thenReturn(existente(40, 999));
        PostergacionRequest r = pr(null, null);
        r.setCodPostergacion(40);

        String m = mensaje(() -> servicio.eliminar(usuario, r));

        assertEquals("La postergación 40 no es del cheque 18129 (es de otro cheque), por eso no se puede eliminar desde aquí. "
                + "Actualiza la pantalla y vuelve a elegirla.", m);
        verify(postergaciones, never()).eliminar(anyInt(), anyInt());
    }

    @Test
    @DisplayName("eliminar: una postergacion que no existe se explica con su codigo")
    void eliminarInexistente() {
        PostergacionRequest r = pr(null, null);
        r.setCodPostergacion(41);

        assertEquals("No se encontró la postergación 41. Es posible que ya la hayan eliminado: actualiza la pantalla.",
                mensaje(() -> servicio.eliminar(usuario, r)));
        verify(postergaciones, never()).eliminar(anyInt(), anyInt());
    }

    @Test
    @DisplayName("eliminar: sin cuerpo, sin codigo (null o 0), sin cheque o con un cheque que no se ve")
    void eliminarParametros() {
        assertEquals(MensajesCheque.SIN_DATOS, mensaje(() -> servicio.eliminar(usuario, null)));
        PostergacionRequest sinCodigo = pr(null, null);
        assertEquals(MensajesCheque.FALTA_POSTERGACION, mensaje(() -> servicio.eliminar(usuario, sinCodigo)));
        sinCodigo.setCodPostergacion(0);
        assertEquals(MensajesCheque.FALTA_POSTERGACION, mensaje(() -> servicio.eliminar(usuario, sinCodigo)));
        PostergacionRequest sinCheque = pr(null, null);
        sinCheque.setCodCheque(null);
        sinCheque.setCodPostergacion(40);
        assertEquals(MensajesCheque.FALTA_CHEQUE_DEL_PANEL, mensaje(() -> servicio.eliminar(usuario, sinCheque)));
        doThrow(new SinPermisoException(MensajesCheque.SOLO_SU_SUCURSAL)).when(chequeService).exigirSucursalLectura(any(), any());
        PostergacionRequest r = pr(null, null);
        r.setCodPostergacion(40);
        assertThrows(SinPermisoException.class, () -> servicio.eliminar(usuario, r));
        verify(postergaciones, never()).eliminar(anyInt(), anyInt());
    }

    @Test
    @DisplayName("eliminar NO borra el PDF de la postergacion (el legacy tampoco): el archivo sigue en la carpeta")
    void eliminarNoBorraElPdf() throws Exception {
        Path pdf = carpeta.resolve("40.pdf");
        Files.write(pdf, "%PDF-1.4 contenido".getBytes(StandardCharsets.ISO_8859_1));
        when(postergaciones.obtener(40)).thenReturn(existente(40, COD));
        PostergacionRequest r = pr(null, null);
        r.setCodPostergacion(40);

        servicio.eliminar(usuario, r);

        assertTrue(Files.exists(pdf), "el PDF queda en la carpeta");
        assertEquals(Collections.singletonList("40.pdf"), nombresDe(carpeta));
    }

    private static List<String> nombresDe(Path dir) throws Exception {
        try (java.util.stream.Stream<Path> s = Files.list(dir)) {
            return s.map(p -> p.getFileName().toString()).sorted().collect(java.util.stream.Collectors.toList());
        }
    }
}
