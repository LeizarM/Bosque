package bo.bosque.com.impexpap.commons;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Locale;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.dao.IChPostergacion;
import bo.bosque.com.impexpap.dto.ChequePdfEstadoDto;
import bo.bosque.com.impexpap.dto.ChequePdfSubidoDto;
import bo.bosque.com.impexpap.model.ChPostergacion;
import bo.bosque.com.impexpap.security.jwt.DatosToken;

/**
 * El documento PDF de una postergacion ("Cargar PDF" / "Descargar PDF" de cada fila del panel Postergaciones de
 * {@code cheque.xhtml:362-371}). Mismo comportamiento y mismas reglas que {@link ChequePdfService}; la parte tecnica comun
 * esta en {@link AlmacenPdf}.
 *
 * <h2>Lo que hace el legacy y se conserva (decision 2 de CLAUDE.md)</h2>
 * <ul>
 *   <li>El documento es un archivo {@code <codPostergacion>.pdf} en {@code D:\Bosque\postergacionCheques\}
 *       ({@code Carga.cargaRutaPostergacion}, {@code Carga.java:94-99}; el nombre, {@code WizardCheque.java:2319}). <b>No hay nada
 *       en la base de datos</b>: la existencia se comprueba mirando el archivo. {@code tch_chPostergacion.nombreArchivo} queda
 *       siempre vacio: el legacy nunca lo actualiza al subir el PDF.</li>
 *   <li>Subir uno donde ya hay otro lo <b>reemplaza</b>.</li>
 *   <li>Solo {@code .pdf} y de menos de 2 MegaBytes ({@link #TAMANO_MAXIMO_BYTES}: {@code sizeLimit="2010000"} y
 *       {@code allowTypes} de {@code cheque.xhtml:1373-1374}).</li>
 *   <li>Se descarga como {@code Posterg_<codPostergacion>_.pdf} ({@code WizardCheque.java:2368}, {@code Carga.java:256}; sin el
 *       espacio inicial que traia el encabezado del legacy).</li>
 *   <li><b>Ningun permiso de boton</b>: los botones de la fila no tenian ninguna condicion. Se exige lo que la pantalla ya
 *       garantizaba sin decirlo: que la postergacion exista, que su cheque exista y que el usuario pueda ver su sucursal.</li>
 * </ul>
 *
 * <h2>Lo que cambia, y es solo tecnico</h2>
 * Los mismos cuidados que {@link ChequePdfService}: la extension no distingue mayusculas; el contenido tiene que empezar con
 * {@code %PDF-}; el nombre sale del <b>numero</b> {@code codPostergacion} y nunca de un texto del cliente; la escritura es
 * atomica (el documento anterior no se pierde si algo falla).
 *
 * <h2>La carpeta</h2>
 * Propiedad {@code cheques.postergacion.pdf.dir}; por defecto {@code <uploads.dir>/postergacionCheques} (ruta relativa a la
 * aplicacion, como Depositos; en produccion es el volumen persistente {@code /app/uploads}), o la que diga la variable
 * {@code POSTERGACIONES_PDF_DIR}. La carpeta se <b>crea al subir el primer PDF</b>; mientras no exista, ninguna postergacion tiene
 * PDF. Los nombres son los del sistema anterior ({@code D:\Bosque\postergacionCheques\}): copiando esos archivos aqui se ven.
 */
@Service
public class PostergacionPdfService {

    private static final Logger log = LoggerFactory.getLogger(PostergacionPdfService.class);

    /** {@code sizeLimit="2010000"} del {@code p:fileUpload} del legacy, en bytes. */
    public static final long TAMANO_MAXIMO_BYTES = AlmacenPdf.TAMANO_MAXIMO_BYTES;

    /** Un codigo de postergacion: solo digitos, y de 9 como maximo para que siempre entre en un {@code int}. */
    private static final Pattern CODIGO = Pattern.compile("\\d{1,9}");

    private final IChPostergacion postergaciones;
    private final AccesoPanelesCheque acceso;
    private final String carpeta;
    private final Clock reloj;

    @Autowired
    public PostergacionPdfService(IChPostergacion postergaciones, AccesoPanelesCheque acceso,
                                  @Value("${cheques.postergacion.pdf.dir:}") String carpeta) {
        this(postergaciones, acceso, carpeta, AlmacenPdf.RELOJ);
    }

    /** Para las pruebas: un reloj con la zona que se quiera para la fecha de modificacion. */
    PostergacionPdfService(IChPostergacion postergaciones, AccesoPanelesCheque acceso, String carpeta, Clock reloj) {
        this.postergaciones = postergaciones;
        this.acceso = acceso;
        this.carpeta = carpeta;
        this.reloj = reloj;
    }

    // ===================================================================== //
    //                              CONSULTA                                 //
    // ===================================================================== //

    /** Si la postergacion ya tiene su PDF ({@code verificarPathPostrgcnPdf} del legacy, sin el efecto de abrir el dialogo). */
    public ChequePdfEstadoDto estado(Authentication auth, Integer codPostergacion) {
        int cod = postergacionConAcceso(auth, texto(codPostergacion));
        Path pdf = carpetaDisponible(false).resolve(nombreGuardado(cod));
        String nombre = pdf.getFileName().toString();
        if (!Files.isRegularFile(pdf)) return new ChequePdfEstadoDto(false, nombre, null, null);
        try {
            long tamano = Files.size(pdf);
            return new ChequePdfEstadoDto(true, nombre, tamano, AlmacenPdf.fechaDeModificacion(pdf, reloj.getZone()));
        } catch (NoSuchFileException e) {
            return new ChequePdfEstadoDto(false, nombre, null, null);   // lo borraron entre la pregunta y la lectura
        } catch (IOException e) {
            log.error("No se pudo leer el archivo del PDF de la postergacion {}: {}", cod, e.toString());
            throw new SpBusinessException(MensajesCheque.noSePudoLeerPdfPostergacion(cod));
        }
    }

    /**
     * Para el listado: {@code true} / {@code false} segun exista el archivo, o {@code null} si no se puede saber porque la carpeta
     * no esta configurada o no esta disponible. <b>Nunca lanza</b>: el listado de postergaciones no debe fallar por la carpeta.
     * El que llama ya comprobo el acceso al cheque.
     */
    public Boolean tienePdf(int codPostergacion) {
        try {
            Path dir = carpetaDisponible(false);
            return Files.isRegularFile(dir.resolve(nombreGuardado(codPostergacion)));
        } catch (SpBusinessException e) {
            return null;
        }
    }

    // ===================================================================== //
    //                               DESCARGA                                //
    // ===================================================================== //

    /** {@code Carga.downLoad}: el PDF de la postergacion, con el nombre {@code Posterg_<codPostergacion>_.pdf}. */
    public ChequePdfService.Descarga descargar(Authentication auth, Integer codPostergacion) {
        int cod = postergacionConAcceso(auth, texto(codPostergacion));
        Path pdf = carpetaDisponible(false).resolve(nombreGuardado(cod));
        if (!Files.isRegularFile(pdf)) throw new SpBusinessException(MensajesCheque.pdfPostergacionNoEncontrado(cod));
        byte[] contenido;
        try {
            contenido = Files.readAllBytes(pdf);
        } catch (NoSuchFileException e) {
            throw new SpBusinessException(MensajesCheque.pdfPostergacionNoEncontrado(cod));
        } catch (IOException e) {
            log.error("No se pudo leer el archivo del PDF de la postergacion {}: {}", cod, e.toString());
            throw new SpBusinessException(MensajesCheque.noSePudoLeerPdfPostergacion(cod));
        }
        log.info("PDF de postergacion descargado: usuario={} ({}), codPostergacion={}, bytes={}",
                auth.getName(), DatosToken.codUsuarioDe(auth), cod, contenido.length);
        return new ChequePdfService.Descarga("Posterg_" + cod + "_.pdf", contenido);
    }

    // ===================================================================== //
    //                                SUBIDA                                 //
    // ===================================================================== //

    /**
     * {@code uploadFile} + {@code saveDocumentoPostrgcnPdf}: guarda el PDF como {@code <codPostergacion>.pdf}, reemplazando el
     * que hubiera.
     *
     * @param codPostergacionTexto el valor de la parte {@code codPostergacion} del formulario, tal como llego (se valida: solo
     *                             digitos, mayor que 0)
     * @param archivo              la parte {@code archivo}
     */
    public ChequePdfSubidoDto subir(Authentication auth, String codPostergacionTexto, MultipartFile archivo) {
        int cod = codPostergacionValido(codPostergacionTexto);
        if (archivo == null) throw new SpBusinessException(MensajesCheque.FALTA_ARCHIVO_PDF_POSTERGACION);
        exigirPostergacionConAcceso(auth, cod);
        int usuario = DatosToken.codUsuarioDe(auth);

        String nombreOriginal = archivo.getOriginalFilename();
        if (nombreOriginal == null || !nombreOriginal.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new SpBusinessException(MensajesCheque.archivoNoEsPdfPostergacion(nombreOriginal));
        }
        long declarado = archivo.getSize();
        if (declarado <= 0) throw new SpBusinessException(MensajesCheque.archivoPdfPostergacionVacio(nombreOriginal));
        if (declarado > TAMANO_MAXIMO_BYTES) {
            throw new SpBusinessException(MensajesCheque.archivoPdfMuyGrande(declarado, TAMANO_MAXIMO_BYTES));
        }

        try (InputStream entrada = archivo.getInputStream()) {
            byte[] cabecera = AlmacenPdf.leerHasta(entrada, AlmacenPdf.VENTANA_DE_LA_MARCA);
            if (cabecera.length == 0) throw new SpBusinessException(MensajesCheque.archivoPdfPostergacionVacio(nombreOriginal));
            if (!AlmacenPdf.tieneMarcaPdf(cabecera)) {
                throw new SpBusinessException(MensajesCheque.archivoPdfSinContenidoPdf(nombreOriginal));
            }

            Path dir = carpetaDisponible(true);
            Path destino = dir.resolve(nombreGuardado(cod));
            boolean reemplazo = Files.exists(destino);
            long escritos = AlmacenPdf.guardarAtomico(dir, destino, "postergacion-" + cod, cabecera, entrada,
                    total -> new SpBusinessException(MensajesCheque.archivoPdfMuyGrande(total, TAMANO_MAXIMO_BYTES)), log);

            log.info("PDF de postergacion guardado: usuario={} ({}), codPostergacion={}, bytes={}, reemplazo={}",
                    auth.getName(), usuario, cod, escritos, reemplazo);
            return new ChequePdfSubidoDto(destino.getFileName().toString(), escritos, reemplazo);
        } catch (IOException e) {
            log.error("No se pudo guardar el PDF de la postergacion {} (usuario {}): {}", cod, usuario, e.toString());
            throw new SpBusinessException(MensajesCheque.noSePudoGuardarPdfPostergacion(cod));
        }
    }

    // ===================================================================== //
    //                                APOYO                                  //
    // ===================================================================== //

    /** El unico nombre con el que se guarda: sale del numero, nunca de un texto del cliente. */
    private static String nombreGuardado(int codPostergacion) {
        return codPostergacion + ".pdf";
    }

    private static String texto(Integer cod) {
        return cod == null ? null : cod.toString();
    }

    /** El codigo de la postergacion: solo digitos y mayor que 0. */
    private static int codPostergacionValido(String texto) {
        String t = texto == null ? "" : texto.trim();
        if (t.isEmpty()) throw new SpBusinessException(MensajesCheque.FALTA_POSTERGACION_PDF);
        if (!CODIGO.matcher(t).matches()) throw new SpBusinessException(MensajesCheque.codPostergacionPdfNoValido(texto));
        int cod = Integer.parseInt(t);
        if (cod <= 0) throw new SpBusinessException(MensajesCheque.codPostergacionPdfNoValido(texto));
        return cod;
    }

    private int postergacionConAcceso(Authentication auth, String codTexto) {
        int cod = codPostergacionValido(codTexto);
        exigirPostergacionConAcceso(auth, cod);
        return cod;
    }

    /**
     * La postergacion existe, y de ella sale el cheque: tiene que existir y el usuario puede ver su sucursal (sin permiso de
     * boton, como los botones de la fila del legacy).
     */
    private void exigirPostergacionConAcceso(Authentication auth, int cod) {
        ChPostergacion p = postergaciones.obtener(cod);
        if (p == null) throw new SpBusinessException(MensajesCheque.postergacionNoEncontrada(cod));
        acceso.chequeVisible(auth, p.getCodCheque());
    }

    /**
     * La carpeta de los PDF, solo si ya existe y se puede usar. Nunca la crea.
     *
     * @param escribir true para subir (ademas de leer hay que poder escribir)
     */
    private Path carpetaDisponible(boolean escribir) {
        return AlmacenPdf.carpetaDisponible(carpeta, escribir,
                () -> new SpBusinessException(MensajesCheque.CARPETA_PDF_POSTERGACION_NO_CONFIGURADA),
                (ruta, motivo) -> new SpBusinessException(MensajesCheque.carpetaPdfPostergacionNoDisponible(ruta, motivo)), log);
    }
}
