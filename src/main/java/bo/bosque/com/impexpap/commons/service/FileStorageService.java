package bo.bosque.com.impexpap.commons.service;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.Objects;

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
}