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
import bo.bosque.com.impexpap.dao.IChCheque;
import bo.bosque.com.impexpap.dto.ChequeFilaDto;
import bo.bosque.com.impexpap.dto.ChequePdfEstadoDto;
import bo.bosque.com.impexpap.dto.ChequePdfSubidoDto;
import bo.bosque.com.impexpap.security.jwt.DatosToken;

/**
 * El documento PDF de un cheque ("Cargar Documento PDF" / "Descargar Documento PDF" de {@code cheque.xhtml}).
 *
 * <h2>Lo que hace el legacy y se conserva (decision 2 de CLAUDE.md)</h2>
 * <ul>
 *   <li>El documento es un archivo {@code <codCheque>.pdf} en una carpeta del servidor ({@code D:\Bosque\cheques\} en el legacy).
 *       <b>No hay nada en la base de datos</b>: la existencia se comprueba mirando el archivo.</li>
 *   <li>Subir uno donde ya hay otro lo <b>reemplaza</b>.</li>
 *   <li>Solo {@code .pdf} y de menos de 2 MegaBytes ({@link #TAMANO_MAXIMO_BYTES}, el {@code sizeLimit} del
 *       {@code p:fileUpload}).</li>
 *   <li>Se descarga como {@code <codCheque>_.pdf}.</li>
 *   <li><b>Ningun permiso de boton</b>: los botones de la fila no tenian ninguna condicion. Se exige lo que la pantalla ya
 *       garantizaba sin decirlo: el cheque existe y el usuario puede ver su sucursal
 *       ({@link ChequeService#exigirSucursalLectura}).</li>
 * </ul>
 *
 * <h2>Lo que cambia, y es solo tecnico (no cambia lo que acepta un PDF legitimo)</h2>
 * <ul>
 *   <li>El legacy, con un cheque inexistente, seguia con codigo 0 y habria escrito {@code 0.pdf}: aqui es un error.</li>
 *   <li>La extension no distingue mayusculas ({@code .PDF} tambien vale). El legacy usaba {@code /(\.|\/)(pdf)$/}, que si las
 *       distingue, pero por accidente: el archivo se guarda siempre como {@code <codCheque>.pdf}.</li>
 *   <li>El cliente no es frontera de confianza: ademas de la extension se comprueba que el contenido empiece con
 *       {@code %PDF-} (dentro de los primeros 1024 bytes) y que el peso real no pase el limite.</li>
 *   <li>El nombre del archivo sale del <b>numero</b> {@code codCheque} y nunca de un texto del cliente: no hay recorrido de
 *       carpetas posible, ni con el nombre del archivo subido (que se ignora).</li>
 *   <li>La escritura es <b>atomica</b>: se escribe un temporal en la misma carpeta y se mueve sobre el destino, de modo que
 *       un corte a medias no deja un PDF truncado ni pierde el anterior; el temporal se borra si algo falla. (El legacy borraba
 *       el anterior y despues movia el nuevo.)</li>
 * </ul>
 *
 * <h2>La carpeta</h2>
 * Propiedad {@code cheques.pdf.dir}; por defecto {@code <uploads.dir>/cheques} (ruta relativa a la aplicacion, como Depositos y
 * Pagos al exterior: en desarrollo {@code ./uploads/cheques}, en produccion el volumen persistente {@code /app/uploads/cheques}),
 * o la que diga la variable {@code CHEQUES_PDF_DIR}. La carpeta se <b>crea al subir el primer PDF</b>; mientras no exista,
 * ningun cheque tiene PDF. Los nombres de archivo son los del sistema anterior ({@code D:\Bosque\cheques\<codCheque>.pdf}):
 * copiando esos archivos a la carpeta nueva, los PDF viejos se ven desde aqui. El sistema anterior no ve los que se suban desde
 * aqui: cada sistema tiene su propia carpeta.
 *
 * <p>Nunca se escribe el contenido del archivo en el log.
 */
@Service
public class ChequePdfService {

    private static final Logger log = LoggerFactory.getLogger(ChequePdfService.class);

    /** {@code sizeLimit="2010000"} del {@code p:fileUpload} del legacy, en bytes (ver {@link AlmacenPdf}). */
    public static final long TAMANO_MAXIMO_BYTES = AlmacenPdf.TAMANO_MAXIMO_BYTES;

    /** Los primeros bytes donde tiene que aparecer la marca de un PDF (ver {@link AlmacenPdf}). */
    static final int VENTANA_DE_LA_MARCA = AlmacenPdf.VENTANA_DE_LA_MARCA;

    /** Un codigo de cheque: solo digitos, y de 9 como maximo para que siempre entre en un {@code int}. */
    private static final Pattern CODIGO = Pattern.compile("\\d{1,9}");

    /** El archivo listo para descargar. */
    public static final class Descarga {
        private final String nombreDescarga;
        private final byte[] contenido;

        Descarga(String nombreDescarga, byte[] contenido) {
            this.nombreDescarga = nombreDescarga;
            this.contenido = contenido;
        }

        /** {@code <codCheque>_.pdf}. */
        public String getNombreDescarga() {
            return nombreDescarga;
        }

        public byte[] getContenido() {
            return contenido;
        }
    }

    private final IChCheque cheques;
    private final ChequeService chequeService;
    private final String carpeta;
    private final Clock reloj;

    @Autowired
    public ChequePdfService(IChCheque cheques, ChequeService chequeService,
                            @Value("${cheques.pdf.dir:}") String carpeta) {
        this(cheques, chequeService, carpeta, AlmacenPdf.RELOJ);
    }

    /** Para las pruebas: un reloj con la zona que se quiera para la fecha de modificacion. */
    ChequePdfService(IChCheque cheques, ChequeService chequeService, String carpeta, Clock reloj) {
        this.cheques = cheques;
        this.chequeService = chequeService;
        this.carpeta = carpeta;
        this.reloj = reloj;
    }

    // ===================================================================== //
    //                              CONSULTA                                 //
    // ===================================================================== //

    /** Si el cheque ya tiene su PDF ({@code verificarPathChkPdf} del legacy, sin el efecto de abrir el dialogo). */
    public ChequePdfEstadoDto estado(Authentication auth, Integer codCheque) {
        int cod = chequeConAcceso(auth, texto(codCheque));
        Path pdf = carpetaDisponible(false).resolve(nombreGuardado(cod));
        String nombre = pdf.getFileName().toString();
        if (!Files.isRegularFile(pdf)) return new ChequePdfEstadoDto(false, nombre, null, null);
        try {
            long tamano = Files.size(pdf);
            return new ChequePdfEstadoDto(true, nombre, tamano, AlmacenPdf.fechaDeModificacion(pdf, reloj.getZone()));
        } catch (NoSuchFileException e) {
            return new ChequePdfEstadoDto(false, nombre, null, null);   // lo borraron entre la pregunta y la lectura
        } catch (IOException e) {
            log.error("No se pudo leer el archivo del PDF del cheque {}: {}", cod, e.toString());
            throw new SpBusinessException(MensajesCheque.noSePudoLeerPdf(cod));
        }
    }

    // ===================================================================== //
    //                               DESCARGA                                //
    // ===================================================================== //

    /** {@code Carga.downLoad}: el PDF del cheque, con el nombre {@code <codCheque>_.pdf}. */
    public Descarga descargar(Authentication auth, Integer codCheque) {
        int cod = chequeConAcceso(auth, texto(codCheque));
        Path pdf = carpetaDisponible(false).resolve(nombreGuardado(cod));
        if (!Files.isRegularFile(pdf)) throw new SpBusinessException(MensajesCheque.pdfNoEncontrado(cod));
        byte[] contenido;
        try {
            contenido = Files.readAllBytes(pdf);
        } catch (NoSuchFileException e) {
            throw new SpBusinessException(MensajesCheque.pdfNoEncontrado(cod));
        } catch (IOException e) {
            log.error("No se pudo leer el archivo del PDF del cheque {}: {}", cod, e.toString());
            throw new SpBusinessException(MensajesCheque.noSePudoLeerPdf(cod));
        }
        log.info("PDF de cheque descargado: usuario={} ({}), codCheque={}, bytes={}",
                auth.getName(), DatosToken.codUsuarioDe(auth), cod, contenido.length);
        return new Descarga(cod + "_.pdf", contenido);
    }

    // ===================================================================== //
    //                                SUBIDA                                 //
    // ===================================================================== //

    /**
     * {@code uploadFile} + {@code saveDocumentoChkPdf}: guarda el PDF como {@code <codCheque>.pdf}, reemplazando el que hubiera.
     *
     * @param codChequeTexto el valor de la parte {@code codCheque} del formulario, tal como llego (se valida: solo digitos, mayor que 0)
     * @param archivo        la parte {@code archivo}
     */
    public ChequePdfSubidoDto subir(Authentication auth, String codChequeTexto, MultipartFile archivo) {
        int cod = codChequeValido(codChequeTexto);
        if (archivo == null) throw new SpBusinessException(MensajesCheque.FALTA_ARCHIVO_PDF);
        exigirChequeConAcceso(auth, cod);
        int usuario = DatosToken.codUsuarioDe(auth);

        String nombreOriginal = archivo.getOriginalFilename();
        if (nombreOriginal == null || !nombreOriginal.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new SpBusinessException(MensajesCheque.archivoNoEsPdf(nombreOriginal));
        }
        long declarado = archivo.getSize();
        if (declarado <= 0) throw new SpBusinessException(MensajesCheque.archivoPdfVacio(nombreOriginal));
        if (declarado > TAMANO_MAXIMO_BYTES) {
            throw new SpBusinessException(MensajesCheque.archivoPdfMuyGrande(declarado, TAMANO_MAXIMO_BYTES));
        }

        try (InputStream entrada = archivo.getInputStream()) {
            byte[] cabecera = AlmacenPdf.leerHasta(entrada, VENTANA_DE_LA_MARCA);
            if (cabecera.length == 0) throw new SpBusinessException(MensajesCheque.archivoPdfVacio(nombreOriginal));
            if (!AlmacenPdf.tieneMarcaPdf(cabecera)) {
                throw new SpBusinessException(MensajesCheque.archivoPdfSinContenidoPdf(nombreOriginal));
            }

            Path dir = carpetaDisponible(true);
            Path destino = dir.resolve(nombreGuardado(cod));
            boolean reemplazo = Files.exists(destino);
            long escritos = AlmacenPdf.guardarAtomico(dir, destino, "cheque-" + cod, cabecera, entrada,
                    total -> new SpBusinessException(MensajesCheque.archivoPdfMuyGrande(total, TAMANO_MAXIMO_BYTES)), log);

            log.info("PDF de cheque guardado: usuario={} ({}), codCheque={}, bytes={}, reemplazo={}",
                    auth.getName(), usuario, cod, escritos, reemplazo);
            return new ChequePdfSubidoDto(destino.getFileName().toString(), escritos, reemplazo);
        } catch (IOException e) {
            log.error("No se pudo guardar el PDF del cheque {} (usuario {}): {}", cod, usuario, e.toString());
            throw new SpBusinessException(MensajesCheque.noSePudoGuardarPdf(cod));
        }
    }

    // ===================================================================== //
    //                                APOYO                                  //
    // ===================================================================== //

    /** El unico nombre con el que se guarda: sale del numero, nunca de un texto del cliente. */
    private static String nombreGuardado(int codCheque) {
        return codCheque + ".pdf";
    }

    private static String texto(Integer codCheque) {
        return codCheque == null ? null : codCheque.toString();
    }

    /** El codigo del cheque: solo digitos y mayor que 0. */
    private static int codChequeValido(String texto) {
        String t = texto == null ? "" : texto.trim();
        if (t.isEmpty()) throw new SpBusinessException(MensajesCheque.FALTA_CHEQUE_PDF);
        if (!CODIGO.matcher(t).matches()) throw new SpBusinessException(MensajesCheque.codChequePdfNoValido(texto));
        int cod = Integer.parseInt(t);
        if (cod <= 0) throw new SpBusinessException(MensajesCheque.codChequePdfNoValido(texto));
        return cod;
    }

    private int chequeConAcceso(Authentication auth, String codChequeTexto) {
        int cod = codChequeValido(codChequeTexto);
        exigirChequeConAcceso(auth, cod);
        return cod;
    }

    /** El cheque existe y el usuario puede ver su sucursal (sin permiso de boton, como los botones de la fila del legacy). */
    private void exigirChequeConAcceso(Authentication auth, int cod) {
        ChequeFilaDto c = cheques.obtener(cod);
        if (c == null) throw new SpBusinessException(MensajesCheque.chequeNoEncontrado(cod));
        chequeService.exigirSucursalLectura(auth, c.getCodSucursal());
    }

    /**
     * La carpeta de los PDF: se crea al subir; para leer se devuelve la ruta aunque aun no exista (sin carpeta, sin PDF).
     *
     * @param escribir true para subir (ademas de leer hay que poder escribir)
     */
    private Path carpetaDisponible(boolean escribir) {
        return AlmacenPdf.carpetaDisponible(carpeta, escribir,
                () -> new SpBusinessException(MensajesCheque.CARPETA_PDF_NO_CONFIGURADA),
                (ruta, motivo) -> new SpBusinessException(MensajesCheque.carpetaPdfNoDisponible(ruta, motivo)), log);
    }
}
