package bo.bosque.com.impexpap.commons;

import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.dao.IArchivoDocumental;
import bo.bosque.com.impexpap.model.CajaArchivo;
import bo.bosque.com.impexpap.model.FotoCaja;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.UUID;

/**
 * Las fotos de las cajas del archivo: el archivo vive en {@code <uploads.dir>/archivo-cajas} y la ruta relativa en
 * {@code tarc_cajaFoto}. El orden es siempre disco y despues base al subir (si el SP rechaza se borra el archivo
 * nuevo), y base y despues disco al quitar (un archivo huerfano no rompe nada; una fila sin archivo si).
 */
@Service
public class FotosCajaService {

    private static final Logger log = LoggerFactory.getLogger(FotosCajaService.class);

    /** Carpeta dentro de uploads.dir; toda ruta guardada en la base empieza con ella. */
    static final String CARPETA = "archivo-cajas";

    /** La app comprime antes de subir; el tope deja pasar una foto de telefono sin comprimir. */
    public static final long TAMANO_MAXIMO_BYTES = 8L * 1024 * 1024;

    public static final int MAXIMO_FOTOS = 3;

    /** Los bytes de una foto y su tipo MIME, listos para responder. */
    public static final class Imagen {
        private final byte[] contenido;
        private final String tipo;

        Imagen(byte[] contenido, String tipo) {
            this.contenido = contenido;
            this.tipo = tipo;
        }

        public byte[] getContenido() {
            return contenido;
        }

        public String getTipo() {
            return tipo;
        }
    }

    private final IArchivoDocumental dao;
    private final Path base;

    @Autowired
    public FotosCajaService(IArchivoDocumental dao, @Value("${uploads.dir:./uploads}") String uploadsDir) {
        this.dao = dao;
        this.base = Paths.get(uploadsDir).toAbsolutePath().normalize();
    }

    /** Agrega una foto en el primer lugar libre de la caja. Devuelve el codFoto. */
    public long subir(long codCaja, MultipartFile archivo, long audUsuario) {
        if (dao.obtenerCaja(codCaja) == null) throw new SpBusinessException("La caja indicada no existe.");
        // El SP tambien lo rechaza; comprobarlo antes evita escribir en disco para nada.
        if (dao.listarFotos(codCaja).size() >= MAXIMO_FOTOS) {
            throw new SpBusinessException("La caja ya tiene 3 fotos. Reemplaza o elimina una antes de agregar otra.");
        }
        String ruta = guardar(codCaja, archivo);

        FotoCaja alta = new FotoCaja();
        alta.setCodCaja(codCaja);
        alta.setRuta(ruta);
        alta.setAudUsuario(audUsuario);
        try {
            return dao.registrarFoto(alta, "I").getIdGenerado();
        } catch (RuntimeException e) {
            borrar(ruta);
            throw e;
        }
    }

    /** Cambia la imagen de una foto existente; conserva caja y posicion. */
    public void reemplazar(long codFoto, MultipartFile archivo, long audUsuario) {
        FotoCaja actual = existente(codFoto);
        String nueva = guardar(actual.getCodCaja(), archivo);

        FotoCaja cambio = new FotoCaja();
        cambio.setCodFoto(codFoto);
        cambio.setRuta(nueva);
        cambio.setAudUsuario(audUsuario);
        try {
            dao.registrarFoto(cambio, "U");
        } catch (RuntimeException e) {
            borrar(nueva);
            throw e;
        }
        borrar(actual.getRuta());
    }

    public void eliminar(long codFoto, long audUsuario) {
        FotoCaja actual = existente(codFoto);
        FotoCaja baja = new FotoCaja();
        baja.setCodFoto(codFoto);
        baja.setAudUsuario(audUsuario);
        dao.registrarFoto(baja, "D");
        borrar(actual.getRuta());
    }

    /** Elimina una caja vacia; el SP borra las filas de sus fotos y aqui se borran los archivos. */
    public RespuestaSp eliminarCaja(long codCaja, long audUsuario) {
        List<FotoCaja> fotos = dao.listarFotos(codCaja);
        CajaArchivo baja = new CajaArchivo();
        baja.setCodCaja(codCaja);
        baja.setAudUsuario(audUsuario);
        RespuestaSp respuesta = dao.registrarCaja(baja, "D");
        for (FotoCaja f : fotos) borrar(f.getRuta());
        return respuesta;
    }

    public Imagen leer(long codFoto) {
        FotoCaja foto = existente(codFoto);
        Path archivo = resolver(foto.getRuta());
        try {
            return new Imagen(Files.readAllBytes(archivo), tipoMime(archivo));
        } catch (NoSuchFileException e) {
            throw new SpBusinessException("La foto ya no esta en el servidor. Reemplazala por una nueva.");
        } catch (IOException e) {
            log.error("No se pudo leer la foto {} ({}): {}", codFoto, archivo, e.toString());
            throw new SpBusinessException("No se pudo leer la foto.");
        }
    }

    // ------------------------------------------------------------------ apoyo

    private FotoCaja existente(long codFoto) {
        FotoCaja foto = dao.obtenerFoto(codFoto);
        if (foto == null) throw new SpBusinessException("La foto indicada no existe.");
        return foto;
    }

    /** Valida el contenido real y escribe un temporal que despues se mueve: un corte no deja una foto a medias. */
    String guardar(long codCaja, MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) throw new SpBusinessException("No se recibio ninguna foto.");
        if (archivo.getSize() > TAMANO_MAXIMO_BYTES) {
            throw new SpBusinessException("La foto pesa mas de 8 MB. Elige una mas liviana.");
        }
        try {
            byte[] datos = archivo.getBytes();
            String extension = extensionDeImagen(datos);
            if (extension == null) throw new SpBusinessException("La foto debe ser una imagen JPG, PNG o WEBP.");

            Path carpeta = base.resolve(CARPETA);
            Files.createDirectories(carpeta);
            // El nombre sale de numeros y un UUID, nunca del nombre que mando el cliente.
            String nombre = "caja-" + codCaja + "-" + UUID.randomUUID() + "." + extension;
            Path temporal = carpeta.resolve(nombre + ".tmp");
            Files.write(temporal, datos, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            try {
                Files.move(temporal, carpeta.resolve(nombre), StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporal, carpeta.resolve(nombre), StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(temporal);
            }
            return CARPETA + "/" + nombre;
        } catch (IOException e) {
            log.error("No se pudo guardar la foto de la caja {}: {}", codCaja, e.toString());
            throw new SpBusinessException("No se pudo guardar la foto en el servidor.");
        }
    }

    /** jpg, png o webp segun la firma de los primeros bytes; null si no es ninguna. */
    static String extensionDeImagen(byte[] d) {
        if (d == null) return null;
        if (d.length >= 3 && (d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8 && (d[2] & 0xFF) == 0xFF) return "jpg";
        if (d.length >= 8 && (d[0] & 0xFF) == 0x89 && d[1] == 'P' && d[2] == 'N' && d[3] == 'G'
                && d[4] == 0x0D && d[5] == 0x0A && d[6] == 0x1A && d[7] == 0x0A) return "png";
        if (d.length >= 12 && d[0] == 'R' && d[1] == 'I' && d[2] == 'F' && d[3] == 'F'
                && d[8] == 'W' && d[9] == 'E' && d[10] == 'B' && d[11] == 'P') return "webp";
        return null;
    }

    /** La ruta guardada, resuelta y confinada a la carpeta de fotos (una fila alterada no lee otro archivo). */
    Path resolver(String ruta) {
        Path archivo = ruta == null ? null : base.resolve(ruta).normalize();
        if (archivo == null || !archivo.startsWith(base.resolve(CARPETA))) {
            throw new SpBusinessException("La ruta guardada de la foto no es valida.");
        }
        return archivo;
    }

    private static String tipoMime(Path archivo) {
        String n = archivo.getFileName().toString().toLowerCase();
        if (n.endsWith(".png")) return "image/png";
        if (n.endsWith(".webp")) return "image/webp";
        return "image/jpeg";
    }

    /** Sin excepciones: un archivo que no se pudo borrar queda huerfano y se anota en el log. */
    private void borrar(String ruta) {
        try {
            Files.deleteIfExists(resolver(ruta));
        } catch (IOException | RuntimeException e) {
            log.warn("No se pudo borrar la foto {}: {}", ruta, e.toString());
        }
    }
}
