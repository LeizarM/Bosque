package bo.bosque.com.impexpap.commons;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import bo.bosque.com.impexpap.config.SinPermisoException;
import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.dao.IChPostergacion;
import bo.bosque.com.impexpap.dto.ChequePdfEstadoDto;
import bo.bosque.com.impexpap.dto.ChequePdfSubidoDto;
import bo.bosque.com.impexpap.model.ChPostergacion;

/**
 * El documento PDF de la postergacion, con una carpeta real temporal ({@code @TempDir}): sin red, sin base y sin tocar ninguna
 * carpeta del equipo. Son las mismas reglas que {@link ChequePdfServiceTest} (paridad con el legacy y seguridad de la escritura),
 * con la postergacion como entidad y sus propios textos.
 */
class PostergacionPdfServiceTest extends PanelesChequeBase {

    private static final int POST = 40;
    private static final ZoneId LA_PAZ = ZoneId.of("America/La_Paz");
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-10-03T18:00:00Z"), LA_PAZ);

    @TempDir
    Path carpeta;

    private IChPostergacion postergaciones;
    private PostergacionPdfService servicio;

    @BeforeEach
    void preparar() {
        postergaciones = mock(IChPostergacion.class);
        ChPostergacion p = new ChPostergacion();
        p.setCodPostergacion(POST);
        p.setCodCheque(COD);
        when(postergaciones.obtener(POST)).thenReturn(p);
        servicio = new PostergacionPdfService(postergaciones, acceso, carpeta.toString(), RELOJ);
    }

    // ------------------------------------------------------------------ utilidades

    private static byte[] pdf(int bytes) {
        byte[] b = new byte[bytes];
        byte[] marca = "%PDF-1.4\n".getBytes(StandardCharsets.ISO_8859_1);
        System.arraycopy(marca, 0, b, 0, Math.min(marca.length, bytes));
        for (int i = marca.length; i < bytes; i++) b[i] = (byte) ('a' + (i % 26));
        return b;
    }

    private static MultipartFile archivo(String nombre, byte[] contenido) {
        return new MockMultipartFile("archivo", nombre, "application/pdf", contenido);
    }

    private ChequePdfSubidoDto subir(String nombre, byte[] contenido) {
        return servicio.subir(usuario, String.valueOf(POST), archivo(nombre, contenido));
    }

    private Path guardado() {
        return carpeta.resolve(POST + ".pdf");
    }

    private List<String> contenidoDeLaCarpeta() throws IOException {
        return contenidoDe(carpeta);
    }

    private static List<String> contenidoDe(Path dir) throws IOException {
        try (Stream<Path> s = Files.list(dir)) {
            return s.map(p -> p.getFileName().toString()).sorted().collect(Collectors.toList());
        }
    }

    private static String mensaje(Runnable accion) {
        return assertThrows(SpBusinessException.class, accion::run).getMessage();
    }

    private static MultipartFile archivoQueSeCorta(String nombre) {
        return new MockMultipartFile("archivo", nombre, "application/pdf", pdf(5000)) {
            @Override
            public InputStream getInputStream() {
                return new InputStream() {
                    private int n;

                    @Override
                    public int read() throws IOException {
                        if (n >= 3000) throw new IOException("conexion perdida");
                        return n++ < 9 ? "%PDF-1.4\n".charAt(n - 1) : 'x';
                    }
                };
            }
        };
    }

    // ------------------------------------------------------------------ estado

    @Test
    @DisplayName("estado: sin archivo, existe=false, el nombre igual es <codPostergacion>.pdf y tamano y fecha son null")
    void estadoSinArchivo() {
        ChequePdfEstadoDto e = servicio.estado(usuario, POST);

        assertFalse(e.isExiste());
        assertEquals("40.pdf", e.getNombreArchivo());
        assertNull(e.getTamanoBytes());
        assertNull(e.getFechaModificacion());
    }

    @Test
    @DisplayName("estado: con archivo, dice su tamano y la fecha de modificacion local de La Paz, sin zona (yyyy-MM-dd'T'HH:mm:ss)")
    void estadoConArchivo() throws Exception {
        Files.write(guardado(), pdf(1234));
        Files.setLastModifiedTime(guardado(), FileTime.from(Instant.parse("2026-10-03T18:22:10Z")));   // 14:22:10 en La Paz

        ChequePdfEstadoDto e = servicio.estado(usuario, POST);

        assertTrue(e.isExiste());
        assertEquals("40.pdf", e.getNombreArchivo());
        assertEquals(1234L, (long) e.getTamanoBytes());
        assertEquals("2026-10-03T14:22:10", e.getFechaModificacion());
    }

    @Test
    @DisplayName("estado: el servicio de produccion (constructor publico) da la hora de La Paz aunque la JVM corra en UTC")
    void estadoUsaLaZonaDelProyectoYNoLaDeLaJvm() throws Exception {
        java.util.TimeZone original = java.util.TimeZone.getDefault();
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"));
        try {
            Files.write(guardado(), pdf(100));
            Files.setLastModifiedTime(guardado(), FileTime.from(Instant.parse("2026-10-03T18:00:00Z")));
            PostergacionPdfService deProduccion = new PostergacionPdfService(postergaciones, acceso, carpeta.toString());

            assertEquals("2026-10-03T14:00:00", deProduccion.estado(usuario, POST).getFechaModificacion());
        } finally {
            java.util.TimeZone.setDefault(original);
        }
    }

    @Test
    @DisplayName("estado: una carpeta llamada 40.pdf no es un documento")
    void estadoConUnaCarpetaDeNombreIgual() throws Exception {
        Files.createDirectory(guardado());
        assertFalse(servicio.estado(usuario, POST).isExiste());
    }

    @Test
    @DisplayName("tienePdf (para el listado): true / false segun el archivo (false tambien si la carpeta aun no existe); null si la carpeta no sirve, sin lanzar nunca")
    void tienePdf() throws Exception {
        assertEquals(Boolean.FALSE, servicio.tienePdf(POST));
        Files.write(guardado(), pdf(100));
        assertEquals(Boolean.TRUE, servicio.tienePdf(POST));

        assertNull(new PostergacionPdfService(postergaciones, acceso, "", RELOJ).tienePdf(POST));
        assertEquals(Boolean.FALSE, new PostergacionPdfService(postergaciones, acceso, carpeta.resolve("nada").toString(), RELOJ).tienePdf(POST));
        Path archivo = Files.createFile(carpeta.resolve("soy-un-archivo"));
        assertNull(new PostergacionPdfService(postergaciones, acceso, archivo.toString(), RELOJ).tienePdf(POST));
    }

    // ------------------------------------------------------------------ subir

    @Test
    @DisplayName("subir un PDF nuevo: se guarda como <codPostergacion>.pdf, reemplazo=false, con sus bytes exactos y sin temporales")
    void subirNuevo() throws Exception {
        byte[] contenido = pdf(50_000);

        ChequePdfSubidoDto r = subir("Carta del cliente.pdf", contenido);

        assertEquals("40.pdf", r.getNombreArchivo());
        assertEquals(50_000L, r.getTamanoBytes());
        assertFalse(r.isReemplazo());
        assertArrayEquals(contenido, Files.readAllBytes(guardado()));
        assertEquals(Collections.singletonList("40.pdf"), contenidoDeLaCarpeta(), "ni temporales ni otros archivos");
    }

    @Test
    @DisplayName("subir donde ya hay uno lo REEMPLAZA: reemplazo=true, queda el nuevo y solo el nuevo")
    void subirReemplaza() throws Exception {
        subir("uno.pdf", pdf(2_000));
        byte[] nuevo = pdf(3_000);
        nuevo[20] = 'Z';

        ChequePdfSubidoDto r = subir("dos.pdf", nuevo);

        assertTrue(r.isReemplazo());
        assertEquals(3_000L, r.getTamanoBytes());
        assertArrayEquals(nuevo, Files.readAllBytes(guardado()));
        assertEquals(Collections.singletonList("40.pdf"), contenidoDeLaCarpeta());
    }

    @Test
    @DisplayName("subir no pide boton propio (solo btnDetalleCH): busca la postergacion, de ella el cheque, y comprueba que su sucursal se ve (y nada mas)")
    void subirNoPideBoton() {
        subir("a.pdf", pdf(100));

        verify(postergaciones).obtener(POST);
        verify(cheques).obtener(COD);
        verify(chequeService).exigirSucursalLectura(usuario, SUCURSAL);
        verificarSoloSePidioElDetalle(usuario);
        verify(chequeService, never()).exigirSucursalEscritura(any(), any());
    }

    @Test
    @DisplayName("tamano: justo 2.010.000 bytes se acepta; 2.010.001 no, y el mensaje dice el tamano recibido y el limite")
    void limiteDeTamano() throws Exception {
        assertEquals(2_010_000L, subir("justo.pdf", pdf(2_010_000)).getTamanoBytes());

        String m = mensaje(() -> subir("grande.pdf", pdf(2_010_001)));

        assertTrue(m.contains("2.010.001 bytes"), m);
        assertTrue(m.contains("2,01 MB"), m);
        assertTrue(m.contains("menos de 2 MegaBytes"), m);
        assertTrue(m.contains("2.010.000 bytes"), m);
        assertEquals(2_010_000L, Files.size(guardado()), "el rechazo no toco el que ya estaba");
        assertEquals(Collections.singletonList("40.pdf"), contenidoDeLaCarpeta());
    }

    @Test
    @DisplayName("el tamano que declara el servidor no es lo unico que se mira: si el flujo real pasa el limite, se corta y no queda nada")
    void tamanoDeclaradoMenorQueElReal() throws Exception {
        MultipartFile miente = new MockMultipartFile("archivo", "m.pdf", "application/pdf", pdf(100)) {
            @Override
            public InputStream getInputStream() {
                return new ByteArrayInputStream(pdf(2_500_000));
            }
        };

        String m = mensaje(() -> servicio.subir(usuario, String.valueOf(POST), miente));

        assertTrue(m.contains("menos de 2 MegaBytes"), m);
        assertEquals(0, contenidoDeLaCarpeta().size(), "sin PDF a medias ni temporal");
    }

    @Test
    @DisplayName("un archivo vacio se rechaza diciendo que esta vacio (0 bytes) y de la postergacion")
    void vacio() throws Exception {
        String m = mensaje(() -> subir("vacio.pdf", new byte[0]));

        assertEquals(MensajesCheque.archivoPdfPostergacionVacio("vacio.pdf"), m);
        assertTrue(m.contains("«vacio.pdf»") && m.contains("está vacío (0 bytes)") && m.contains("de la postergación"), m);
        assertEquals(0, contenidoDeLaCarpeta().size());
    }

    @Test
    @DisplayName("solo .pdf: un .png, un nombre sin extension o uno con otra extension detras se rechazan con las palabras del legacy")
    void soloPdf() throws Exception {
        for (String nombre : new String[] {"foto.png", "documento", "pdf", "archivo.pdf.exe", "archivo.pdfx", "archivo.pdf "}) {
            String m = mensaje(() -> subir(nombre, pdf(100)));
            assertEquals(MensajesCheque.archivoNoEsPdfPostergacion(nombre), m, nombre);
            assertTrue(m.startsWith("Solo se permiten pdf") && m.contains("de la postergación"), m);
        }
        String sinNombre = mensaje(() -> servicio.subir(usuario, String.valueOf(POST),
                new MockMultipartFile("archivo", null, "application/pdf", pdf(100))));
        assertTrue(sinNombre.startsWith("Solo se permiten pdf"), sinNombre);
        assertEquals(0, contenidoDeLaCarpeta().size());
    }

    @Test
    @DisplayName("la extension no distingue mayusculas: .PDF y .Pdf valen y se guardan como <codPostergacion>.pdf")
    void extensionEnMayusculas() throws Exception {
        assertEquals("40.pdf", subir("ESCANEO.PDF", pdf(100)).getNombreArchivo());
        assertEquals("40.pdf", subir("Escaneo.Pdf", pdf(200)).getNombreArchivo());

        assertEquals(Collections.singletonList("40.pdf"), contenidoDeLaCarpeta());
        assertEquals(200L, Files.size(guardado()));
    }

    @Test
    @DisplayName("el contenido tiene que ser un PDF: un texto con extension .pdf se rechaza, aunque el cliente diga application/pdf")
    void contenidoQueNoEsPdf() throws Exception {
        String m = mensaje(() -> subir("falso.pdf", "esto no es un pdf, es un texto".getBytes(StandardCharsets.UTF_8)));

        assertEquals(MensajesCheque.archivoPdfSinContenidoPdf("falso.pdf"), m);
        assertTrue(m.contains("no es un PDF") && m.contains("%PDF-"), m);
        assertEquals(0, contenidoDeLaCarpeta().size());
    }

    @Test
    @DisplayName("la marca %PDF- se busca en los primeros 1024 bytes: dentro vale (basura antes), pasada la ventana no")
    void ventanaDeLaMarca() throws Exception {
        byte[] dentro = new byte[3000];
        Arrays.fill(dentro, (byte) ' ');
        System.arraycopy("%PDF-1.7".getBytes(StandardCharsets.ISO_8859_1), 0, dentro, 1010, 8);
        assertEquals(3000L, subir("conBasuraAntes.pdf", dentro).getTamanoBytes());

        byte[] fuera = new byte[3000];
        Arrays.fill(fuera, (byte) ' ');
        System.arraycopy("%PDF-1.7".getBytes(StandardCharsets.ISO_8859_1), 0, fuera, 1500, 8);
        assertTrue(mensaje(() -> subir("fuera.pdf", fuera)).contains("no es un PDF"));
        assertEquals(3000L, Files.size(guardado()), "el rechazo no toco el guardado");
    }

    @Test
    @DisplayName("sin archivo: mensaje claro de la postergacion, y no se consulta nada")
    void sinArchivo() {
        String m = mensaje(() -> servicio.subir(usuario, String.valueOf(POST), null));

        assertEquals(MensajesCheque.FALTA_ARCHIVO_PDF_POSTERGACION, m);
        verify(postergaciones, never()).obtener(anyInt());
    }

    @Test
    @DisplayName("postergacion inexistente: error claro y no se escribe nada")
    void postergacionInexistente() throws Exception {
        String m = mensaje(() -> servicio.subir(usuario, "999", archivo("a.pdf", pdf(100))));

        assertEquals(MensajesCheque.postergacionNoEncontrada(999), m);
        assertEquals(0, contenidoDeLaCarpeta().size(), "ni 999.pdf ni 0.pdf");
        assertEquals(MensajesCheque.postergacionNoEncontrada(999), mensaje(() -> servicio.estado(usuario, 999)));
        assertEquals(MensajesCheque.postergacionNoEncontrada(999), mensaje(() -> servicio.descargar(usuario, 999)));
    }

    @Test
    @DisplayName("la postergacion existe pero su cheque ya no: error de cheque no encontrado")
    void chequeInexistente() {
        ChPostergacion huerfana = new ChPostergacion();
        huerfana.setCodPostergacion(41);
        huerfana.setCodCheque(777);
        when(postergaciones.obtener(41)).thenReturn(huerfana);

        assertEquals(MensajesCheque.chequeNoEncontrado(777), mensaje(() -> servicio.estado(usuario, 41)));
    }

    @Test
    @DisplayName("sin permiso de sucursal: SinPermisoException (403 con motivo), y no se escribe ni se lee nada")
    void sinPermisoDeSucursal() throws Exception {
        doThrow(new SinPermisoException(MensajesCheque.SOLO_SU_SUCURSAL)).when(chequeService).exigirSucursalLectura(any(), any());
        Files.write(guardado(), pdf(100));

        SinPermisoException e1 = assertThrows(SinPermisoException.class, () -> subir("a.pdf", pdf(300)));
        assertEquals(MensajesCheque.SOLO_SU_SUCURSAL, e1.getMessage());
        assertThrows(SinPermisoException.class, () -> servicio.estado(usuario, POST));
        assertThrows(SinPermisoException.class, () -> servicio.descargar(usuario, POST));

        assertEquals(100L, Files.size(guardado()), "no se reemplazo");
        assertEquals(Collections.singletonList("40.pdf"), contenidoDeLaCarpeta());
    }

    @Test
    @DisplayName("codPostergacion: solo numerico y mayor que 0; un texto con rutas, signos, ceros o letras se rechaza sin tocar la carpeta")
    void codPostergacionInvalido() throws Exception {
        for (String malo : new String[] {"abc", "-5", "0", "00", "40.pdf", "../1", "..\\1", "1/2", "1e3", "1,5", "99999999999", "+7", "٣"}) {
            String m = mensaje(() -> servicio.subir(usuario, malo, archivo("a.pdf", pdf(100))));
            assertEquals(MensajesCheque.codPostergacionPdfNoValido(malo), m, "«" + malo + "»");
        }
        for (String falta : new String[] {null, "", "   "}) {
            assertEquals(MensajesCheque.FALTA_POSTERGACION_PDF, mensaje(() -> servicio.subir(usuario, falta, archivo("a.pdf", pdf(100)))));
        }
        assertEquals(MensajesCheque.FALTA_POSTERGACION_PDF, mensaje(() -> servicio.estado(usuario, null)));
        assertEquals(MensajesCheque.codPostergacionPdfNoValido("0"), mensaje(() -> servicio.estado(usuario, 0)));
        assertEquals(MensajesCheque.codPostergacionPdfNoValido("-3"), mensaje(() -> servicio.descargar(usuario, -3)));
        verify(postergaciones, never()).obtener(anyInt());
        assertEquals(0, contenidoDeLaCarpeta().size());
    }

    @Test
    @DisplayName("un codigo con espacios alrededor o ceros a la izquierda se acepta y el nombre sale del numero (7 -> 7.pdf)")
    void codigoConEspaciosOCeros() {
        ChPostergacion siete = new ChPostergacion();
        siete.setCodPostergacion(7);
        siete.setCodCheque(COD);
        when(postergaciones.obtener(7)).thenReturn(siete);

        assertEquals("7.pdf", servicio.subir(usuario, "  0007 ", archivo("a.pdf", pdf(100))).getNombreArchivo());
        assertTrue(Files.exists(carpeta.resolve("7.pdf")));
    }

    @Test
    @DisplayName("el nombre del archivo subido se ignora: uno con '../' o con ruta termina igual en <codPostergacion>.pdf dentro de la carpeta")
    void sinRecorridoDeDirectorios() throws Exception {
        Path base = Files.createDirectory(carpeta.resolve("postergaciones"));
        PostergacionPdfService s = new PostergacionPdfService(postergaciones, acceso, base.toString(), RELOJ);

        s.subir(usuario, String.valueOf(POST), archivo("../../escapado.pdf", pdf(100)));
        s.subir(usuario, String.valueOf(POST), archivo("..\\..\\escapado.pdf", pdf(100)));
        s.subir(usuario, String.valueOf(POST), archivo("C:\\Windows\\System32\\x.pdf", pdf(100)));
        s.subir(usuario, String.valueOf(POST), archivo("/etc/passwd.pdf", pdf(100)));

        assertEquals(Collections.singletonList("postergaciones"), contenidoDe(carpeta), "nada se escapo de la carpeta de los PDF");
        assertEquals(Collections.singletonList("40.pdf"), contenidoDe(base));
    }

    // ------------------------------------------------------------------ fallos y temporales

    @Test
    @DisplayName("si el flujo se corta a mitad de la escritura: error explicito, el temporal se borra y el PDF anterior queda intacto")
    void falloAMitadDeEscritura() throws Exception {
        byte[] anterior = pdf(777);
        Files.write(guardado(), anterior);

        String m = mensaje(() -> servicio.subir(usuario, String.valueOf(POST), archivoQueSeCorta("nuevo.pdf")));

        assertEquals(MensajesCheque.noSePudoGuardarPdfPostergacion(POST), m);
        assertTrue(m.contains("el documento anterior no se tocó"), m);
        assertArrayEquals(anterior, Files.readAllBytes(guardado()));
        assertEquals(Collections.singletonList("40.pdf"), contenidoDeLaCarpeta(), "sin restos de temporales");
    }

    @Test
    @DisplayName("si el movimiento final falla (el destino es una carpeta con cosas dentro): error explicito y sin temporales")
    void falloAlMover() throws Exception {
        Files.createDirectory(guardado());
        Files.write(guardado().resolve("dentro.txt"), new byte[] {1});

        String m = mensaje(() -> subir("a.pdf", pdf(500)));

        assertEquals(MensajesCheque.noSePudoGuardarPdfPostergacion(POST), m);
        assertEquals(Collections.singletonList("40.pdf"), contenidoDeLaCarpeta(), "solo lo que ya estaba: ningun .tmp");
    }

    @Test
    @DisplayName("un rechazo por contenido, nombre o tamano no deja ningun temporal")
    void rechazosSinTemporales() throws Exception {
        mensaje(() -> subir("a.png", pdf(100)));
        mensaje(() -> subir("a.pdf", "xx".getBytes(StandardCharsets.UTF_8)));
        mensaje(() -> subir("a.pdf", pdf(2_100_000)));
        mensaje(() -> subir("a.pdf", new byte[0]));

        assertEquals(0, contenidoDeLaCarpeta().size());
    }

    // ------------------------------------------------------------------ carpeta

    @Test
    @DisplayName("carpeta no configurada (vacia o null): los tres endpoints responden el error claro con el nombre de la propiedad y de la variable")
    void carpetaNoConfigurada() {
        for (String vacia : new String[] {"", "   ", null}) {
            PostergacionPdfService s = new PostergacionPdfService(postergaciones, acceso, vacia, RELOJ);

            assertEquals(MensajesCheque.CARPETA_PDF_POSTERGACION_NO_CONFIGURADA, mensaje(() -> s.estado(usuario, POST)));
            assertEquals(MensajesCheque.CARPETA_PDF_POSTERGACION_NO_CONFIGURADA, mensaje(() -> s.descargar(usuario, POST)));
            assertEquals(MensajesCheque.CARPETA_PDF_POSTERGACION_NO_CONFIGURADA,
                    mensaje(() -> s.subir(usuario, String.valueOf(POST), archivo("a.pdf", pdf(100)))));
        }
        assertTrue(MensajesCheque.CARPETA_PDF_POSTERGACION_NO_CONFIGURADA.contains("cheques.postergacion.pdf.dir"));
        assertTrue(MensajesCheque.CARPETA_PDF_POSTERGACION_NO_CONFIGURADA.contains("POSTERGACIONES_PDF_DIR"));
    }

    @Test
    @DisplayName("carpeta que aun no existe (nadie subio nada): consultar y descargar dicen que no hay PDF, sin error y sin crearla; subir la crea, con sus padres")
    void carpetaInexistenteSeCreaAlSubir() throws Exception {
        Path aun = carpeta.resolve("uploads").resolve("postergacionCheques");
        PostergacionPdfService s = new PostergacionPdfService(postergaciones, acceso, aun.toString(), RELOJ);

        assertFalse(s.estado(usuario, POST).isExiste());
        assertEquals(MensajesCheque.pdfPostergacionNoEncontrado(POST), mensaje(() -> s.descargar(usuario, POST)));
        assertEquals(Boolean.FALSE, s.tienePdf(POST), "el listado dice sin PDF, no no se sabe");
        assertFalse(Files.exists(aun), "leer no crea la carpeta");

        s.subir(usuario, String.valueOf(POST), archivo("a.pdf", pdf(100)));

        assertTrue(Files.isDirectory(aun), "subir crea la carpeta y sus padres");
        assertEquals(Collections.singletonList(POST + ".pdf"), contenidoDe(aun), "solo el PDF: ningun .tmp");
        assertEquals(Boolean.TRUE, s.tienePdf(POST));
    }

    @Test
    @DisplayName("si la carpeta no se puede crear (un padre es un archivo): subir dice la ruta y el motivo; consultar sigue diciendo que no hay PDF")
    void carpetaQueNoSePuedeCrear() throws Exception {
        Path archivo = carpeta.resolve("soy-un-archivo");
        Files.write(archivo, new byte[] {1});
        Path imposible = archivo.resolve("postergaciones");
        PostergacionPdfService s = new PostergacionPdfService(postergaciones, acceso, imposible.toString(), RELOJ);

        String m = mensaje(() -> s.subir(usuario, String.valueOf(POST), archivo("a.pdf", pdf(100))));

        assertEquals(MensajesCheque.carpetaPdfPostergacionNoDisponible(imposible.toString(), "no se pudo crear"), m);
        assertFalse(s.estado(usuario, POST).isExiste());
    }

    @Test
    @DisplayName("la ruta configurada es un archivo y no una carpeta: se dice")
    void carpetaQueEsUnArchivo() throws Exception {
        Path archivo = carpeta.resolve("no-soy-carpeta");
        Files.write(archivo, new byte[] {1});
        PostergacionPdfService s = new PostergacionPdfService(postergaciones, acceso, archivo.toString(), RELOJ);

        assertEquals(MensajesCheque.carpetaPdfPostergacionNoDisponible(archivo.toString(), "no es una carpeta"),
                mensaje(() -> s.estado(usuario, POST)));
    }

    @Test
    @DisplayName("una ruta con caracteres imposibles se rechaza con mensaje, no con un 500")
    void rutaInvalida() {
        PostergacionPdfService s = new PostergacionPdfService(postergaciones, acceso, "carpeta\u0000postergaciones", RELOJ);

        assertEquals(MensajesCheque.carpetaPdfPostergacionNoDisponible("carpeta\u0000postergaciones", "la ruta no es válida"),
                mensaje(() -> s.estado(usuario, POST)));
    }

    @Test
    @DisplayName("el orden: primero lo que el usuario puede corregir. Con la carpeta inexistente, un .png sigue diciendo 'Solo se permiten pdf'")
    void primeroLoQueElUsuarioPuedeCorregir() {
        PostergacionPdfService s = new PostergacionPdfService(postergaciones, acceso, carpeta.resolve("nada").toString(), RELOJ);

        assertTrue(mensaje(() -> s.subir(usuario, String.valueOf(POST), archivo("a.png", pdf(100)))).startsWith("Solo se permiten pdf"));
    }

    // ------------------------------------------------------------------ descargar

    @Test
    @DisplayName("descargar: los bytes del archivo y el nombre de descarga Posterg_<codPostergacion>_.pdf (sin el espacio inicial del legacy)")
    void descargar() throws Exception {
        byte[] contenido = pdf(4321);
        Files.write(guardado(), contenido);

        ChequePdfService.Descarga d = servicio.descargar(usuario, POST);

        assertEquals("Posterg_40_.pdf", d.getNombreDescarga());
        assertArrayEquals(contenido, d.getContenido());
        verify(chequeService).exigirSucursalLectura(usuario, SUCURSAL);
        verificarSoloSePidioElDetalle(usuario);
    }

    @Test
    @DisplayName("descargar una postergacion sin PDF: error explicito (no un 200 vacio) con que hacer")
    void descargarSinArchivo() {
        String m = mensaje(() -> servicio.descargar(usuario, POST));

        assertEquals(MensajesCheque.pdfPostergacionNoEncontrado(POST), m);
        assertTrue(m.startsWith("No se encontró el archivo PDF de la postergación 40"), m);
        assertTrue(m.contains("Cargar PDF"), m);
    }

    @Test
    @DisplayName("lo que se sube se descarga igual: ida y vuelta de los mismos bytes")
    void idaYVuelta() {
        byte[] contenido = pdf(123_456);
        subir("x.pdf", contenido);

        assertArrayEquals(contenido, servicio.descargar(usuario, POST).getContenido());
        assertEquals(123_456L, (long) servicio.estado(usuario, POST).getTamanoBytes());
    }
}
