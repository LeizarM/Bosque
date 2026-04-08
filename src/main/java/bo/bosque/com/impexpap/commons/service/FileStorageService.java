package bo.bosque.com.impexpap.commons.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

@Slf4j
@Service
public class FileStorageService {

    private final Path root = Paths.get("uploads/depositos");

    public FileStorageService() {
        init();
    }

    public void init() {
        try {
            if (!Files.exists(root)) {
                Files.createDirectory(root);
            }
        } catch (IOException e) {
            throw new RuntimeException("No se pudo inicializar la carpeta uploads");
        }
    }

    public String saveFile(MultipartFile archivo, Long idDeposito) throws IOException {
        try {
            // Leer la imagen original
            BufferedImage originalImage = ImageIO.read(archivo.getInputStream());

            // Verificar si la imagen se leyó correctamente
            if (originalImage == null) {
                throw new IOException("No se pudo leer la imagen");
            }

            // Crear una nueva imagen con el tipo de color correcto
            BufferedImage newImage = new BufferedImage(
                    originalImage.getWidth(),
                    originalImage.getHeight(),
                    BufferedImage.TYPE_INT_RGB
            );

            // Copiar la imagen original a la nueva manteniendo los colores
            newImage.createGraphics().drawImage(originalImage, 0, 0, Color.WHITE, null);

            // Crear nombre del archivo
            String nombreArchivo = "deposito_" + idDeposito + ".jpg";
            System.out.println(nombreArchivo);
            Path targetPath = this.root.resolve(nombreArchivo);

            // Guardar como JPG con máxima calidad
            File outputFile = targetPath.toFile();
            ImageIO.write(newImage, "jpg", outputFile);

            return nombreArchivo;
        } catch (IOException e) {
            throw new IOException("Error al guardar y convertir el archivo: " + e.getMessage());
        }
    }



    public Resource obtenerArchivo(int idDeposito) throws IOException {
        try {
            Path file = root.resolve("deposito_" + idDeposito + ".jpg");
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("No se pudo leer el archivo");
            }
        } catch (Exception e) {
            throw new RuntimeException("Error: " + e.getMessage());
        }
    }

    // ── Métodos para vouchers de pagos al exterior ─────────────────────────

    /**
     * Guarda un archivo voucher en la ruta relativa indicada (bajo uploads/).
     * Crea los directorios intermedios si no existen.
     * No convierte el archivo; lo guarda tal cual (soporta PDF, JPG, JPEG, PNG).
     *
     * @param archivo      archivo multipart a guardar
     * @param rutaRelativa ruta relativa, ej: "pagos-extranjeros/vouchers/5_1234567890.pdf"
     * @return             la misma rutaRelativa si el guardado fue exitoso
     */
    public String guardarVoucher(MultipartFile archivo, String rutaRelativa) throws IOException {
        Path targetPath = Paths.get("uploads").resolve(rutaRelativa);
        Files.createDirectories(targetPath.getParent());
        Files.copy(archivo.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        log.info("Voucher guardado en: {}", targetPath.toAbsolutePath());
        return rutaRelativa;
    }

    /**
     * Carga un archivo voucher a partir de su ruta relativa (bajo uploads/).
     *
     * @param rutaRelativa ruta relativa, ej: "pagos-extranjeros/vouchers/5_1234567890.pdf"
     * @return             recurso listo para streaming
     */
    public Resource obtenerVoucher(String rutaRelativa) throws IOException {
        Path filePath = Paths.get("uploads").resolve(rutaRelativa);
        Resource resource = new UrlResource(filePath.toUri());
        if (resource.exists() && resource.isReadable()) {
            return resource;
        }
        throw new RuntimeException("No se pudo leer el voucher: " + rutaRelativa);
    }

    /**
     * Elimina un archivo voucher a partir de su ruta relativa (bajo uploads/).
     * Se usa como rollback manual si la actualización en BD falla después de guardar.
     * No lanza excepción si el archivo no existe.
     *
     * @param rutaRelativa ruta relativa del archivo a eliminar
     */
    public void eliminarVoucher(String rutaRelativa) {
        try {
            Path filePath = Paths.get("uploads").resolve(rutaRelativa);
            boolean eliminado = Files.deleteIfExists(filePath);
            if (eliminado) {
                log.info("Voucher eliminado (rollback): {}", filePath.toAbsolutePath());
            }
        } catch (IOException e) {
            log.warn("No se pudo eliminar el voucher en rollback: {}. Error: {}", rutaRelativa, e.getMessage());
        }
    }
}