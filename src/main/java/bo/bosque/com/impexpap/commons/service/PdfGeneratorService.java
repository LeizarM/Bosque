package bo.bosque.com.impexpap.commons.service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.Style;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.stereotype.Service;
import bo.bosque.com.impexpap.model.DepositoCheque;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class PdfGeneratorService {

    private final Path uploadPath = Paths.get("uploads/depositos");
    private static final DeviceRgb HEADER_COLOR = new DeviceRgb(48, 84, 150);
    private static final DeviceRgb BORDER_COLOR = new DeviceRgb(214, 214, 214);

    public byte[] generarPdfDeposito(DepositoCheque deposito) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf, PageSize.A4.rotate());

            document.setMargins(20, 20, 20, 20);

            // Título con estilo mejorado
            Paragraph titulo = new Paragraph("Comprobante de Depósito")
                    .setFontSize(18)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(15)
                    .setFontColor(HEADER_COLOR);
            document.add(titulo);

            // Calcular dimensiones
            float pageWidth = PageSize.A4.getHeight() - 40;
            float availableHeight = PageSize.A4.getWidth() - 40;
            float dataHeight = availableHeight / 3;
            float imageMaxHeight = availableHeight - dataHeight - 40;

            // Imagen
            try {
                String imagePath = uploadPath.resolve("deposito_" + deposito.getIdDeposito() + ".jpg").toString();
                java.io.File imageFile = new java.io.File(imagePath);

                if (imageFile.exists() && imageFile.isFile()) {
                    ImageData imageData = ImageDataFactory.create(imagePath);
                    Image image = new Image(imageData);

                    float scaleFactor = Math.min(
                            pageWidth / image.getImageWidth(),
                            imageMaxHeight / image.getImageHeight()
                    );

                    image.scale(scaleFactor, scaleFactor);
                    image.setHorizontalAlignment(HorizontalAlignment.CENTER);
                    document.add(image);
                } else {
                    // Si no se encuentra la imagen, agregar un mensaje
                    Paragraph noImageMsg = new Paragraph("Sin imagen o es un deposito para identificar")
                            .setFontSize(12)
                            .setTextAlignment(TextAlignment.CENTER)
                            .setMarginBottom(15)
                            .setFontColor(ColorConstants.RED)
                            .setItalic();
                    document.add(noImageMsg);
                }
            } catch (Exception e) {
                // Si hay un error al procesar la imagen, agregar un mensaje
                Paragraph errorImageMsg = new Paragraph("Error al cargar la imagen: " + e.getMessage())
                        .setFontSize(12)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginBottom(15)
                        .setFontColor(ColorConstants.RED)
                        .setItalic();
                document.add(errorImageMsg);
            }

            // Tabla de datos mejorada
            Table table = new Table(UnitValue.createPercentArray(new float[]{25, 25, 25, 25}))
                    .setWidth(UnitValue.createPercentValue(100))
                    .setMarginTop(15);

            // Estilos para las celdas
            Style headerStyle = new Style()
                    .setBackgroundColor(HEADER_COLOR)
                    .setFontColor(ColorConstants.WHITE)
                    .setBold()
                    .setFontSize(11)
                    .setPadding(8);

            Style cellStyle = new Style()
                    .setFontSize(10)
                    .setPadding(8)
                    .setBorder(Border.NO_BORDER)
                    .setBorderBottom(new SolidBorder(BORDER_COLOR, 1));

            // Primera sección
            addSectionHeader(table, "Información General", headerStyle, 4);
            addTableRow(table, cellStyle,
                    "ID Depósito: " + nvl(deposito.getIdDeposito()),
                    "Empresa: " + nvl(deposito.getNombreEmpresa()),
                    "Cliente: " + nvl(deposito.getCodCliente()),
                    "Documento: " + nvl(deposito.getNumeroDeDocumentos())
            );

            // Segunda sección
            addSectionHeader(table, "Detalle", headerStyle, 4);
            addTableRow(table, cellStyle,
                    "Banco: " + nvl(deposito.getNombreBanco()),
                    "Importe: " + formatImporte((double) deposito.getImporte(), nvl(deposito.getMoneda())),
                    "N° Factura(s): " + nvl(deposito.getNumeroDeFacturas()),
                    "Fecha: " + java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            );

            document.add(table);

            // Pie de página
            Paragraph footer = new Paragraph(
                    "Este documento es un comprobante digital generado el " +
                            java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")))
                    .setFontSize(8)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(10)
                    .setFontColor(ColorConstants.GRAY);
            document.add(footer);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar el PDF: " + e.getMessage());
        }
    }

    // Método para manejar valores nulos
    private String nvl(Object value) {
        if (value == null) {
            return "No disponible";
        }
        return value.toString();
    }

    // Método para formatear el importe
    private String formatImporte(Double importe, String moneda) {
        if (importe == null) {
            return "0.00 " + moneda;
        }
        return String.format("%,.2f %s", importe, moneda);
    }
    private void addSectionHeader(Table table, String headerText, Style headerStyle, int colspan) {
        Cell headerCell = new Cell(1, colspan)
                .add(new Paragraph(headerText))
                .addStyle(headerStyle)
                .setTextAlignment(TextAlignment.LEFT);
        table.addCell(headerCell);
    }

    private void addTableRow(Table table, Style cellStyle, String... values) {
        for (String value : values) {
            Cell cell = new Cell()
                    .add(new Paragraph(value))
                    .addStyle(cellStyle)
                    .setTextAlignment(TextAlignment.LEFT);
            table.addCell(cell);
        }
    }
}