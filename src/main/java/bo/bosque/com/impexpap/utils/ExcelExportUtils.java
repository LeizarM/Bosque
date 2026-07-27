package bo.bosque.com.impexpap.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class ExcelExportUtils {

    /**
     * Exporta una lista de mapas (ej. resultado de
     * SpHelper.ejecutarListadoDinamico) a un arreglo de bytes Excel.
     * Utiliza SXSSFWorkbook para bajo consumo de memoria en reportes grandes.
     *
     * @param data      Lista de mapas con los datos (Clave = Nombre Columna, Valor
     *                  = Dato)
     * @param sheetName Nombre de la hoja en el Excel
     * @return Arreglo de bytes del archivo Excel generado
     */
    public byte[] exportToExcel(List<Map<String, Object>> data, String sheetName) {
        if (data == null || data.isEmpty()) {
            return new byte[0]; // Retorna vacío si no hay datos
        }

        // SXSSFWorkbook usa streaming para no cargar todo en memoria (mantiene 100
        // filas en memoria por defecto)
        try (SXSSFWorkbook workbook = new SXSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(sheetName);

            // Track columns for auto-sizing (necesario en SXSSF)
            if (sheet instanceof org.apache.poi.xssf.streaming.SXSSFSheet) {
                ((org.apache.poi.xssf.streaming.SXSSFSheet) sheet).trackAllColumnsForAutoSizing();
            }

            // Estilos
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dateStyle = workbook.createCellStyle();
            CreationHelper createHelper = workbook.getCreationHelper();
            dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd/MM/yyyy"));

            // Obtener cabeceras a partir del primer mapa
            Map<String, Object> firstRow = data.get(0);
            Set<String> keys = firstRow.keySet();
            String[] headers = keys.toArray(new String[0]);

            // Fila de Cabeceras
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Filas de Datos
            int rowIdx = 1;
            for (Map<String, Object> mapRow : data) {
                Row row = sheet.createRow(rowIdx++);
                for (int i = 0; i < headers.length; i++) {
                    Cell cell = row.createCell(i);
                    Object value = mapRow.get(headers[i]);

                    if (value != null) {
                        if (value instanceof String) {
                            cell.setCellValue((String) value);
                        } else if (value instanceof Number) {
                            cell.setCellValue(((Number) value).doubleValue());
                        } else if (value instanceof Boolean) {
                            cell.setCellValue((Boolean) value);
                        } else if (value instanceof java.util.Date) {
                            cell.setCellValue((java.util.Date) value);
                            cell.setCellStyle(dateStyle);
                        } else {
                            cell.setCellValue(value.toString());
                        }
                    }
                }
            }

            // Auto-size a las columnas
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                workbook.write(out);
                workbook.dispose(); // Liberar archivos temporales del disco
                return out.toByteArray();
            }

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Error al generar archivo Excel", e);
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);

        // Colores y bordes básicos
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }
}
