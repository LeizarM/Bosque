package bo.bosque.com.impexpap.commons.service;

import bo.bosque.com.impexpap.dao.IIndemnizacion;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

@Service
public class FiniquitoExcelService {

    private static final Logger logger = LoggerFactory.getLogger(FiniquitoExcelService.class);

    private static final String TEMPLATE_PATH = "reports/templates/PLANTILLA_FINIQUITO.xlsx";

    private final IIndemnizacion indemnizacionDao;

    public FiniquitoExcelService(IIndemnizacion indemnizacionDao) {
        this.indemnizacionDao = indemnizacionDao;
    }

    /**
     * Genera el archivo Excel de Finiquito a partir de la plantilla oficial
     * poblando los datos del empleado obtenidos del Stored Procedure p_list_Indemnizacion (ACCION = 'F')
     * y preservando todas las fórmulas de cálculo.
     *
     * @param codEmpleado      Código del empleado
     * @param fechaCorte       Fecha de corte (yyyy-MM-dd o dd/MM/yyyy)
     * @param motivoRetiro     Motivo del retiro (opcional, ej. "RETIRO VOLUNTARIO")
     * @param domicilioEmpresa Domicilio de la empresa (opcional, ej. "Av. Montes Nro. 605")
     * @return Arreglo de bytes del archivo .xlsx generado
     */
    public byte[] generarFiniquitoExcel(Integer codEmpleado, String fechaCorte, String motivoRetiro, String domicilioEmpresa) {
        if (codEmpleado == null) {
            throw new IllegalArgumentException("El código de empleado no puede ser nulo.");
        }

        // 1. Consultar todos los datos necesarios para el finiquito directamente desde el Stored Procedure (ACCION = 'F')
        Map<String, Object> data = indemnizacionDao.obtenerDatosFiniquito(codEmpleado, fechaCorte);
        if (data == null || data.isEmpty()) {
            throw new RuntimeException("No se encontraron registros de finiquito para el empleado " + codEmpleado
                    + " a la fecha " + fechaCorte);
        }

        // 2. Cargar plantilla Excel desde el classpath o sistema de archivos
        try (InputStream is = obtenerInputStreamPlantilla();
             Workbook workbook = WorkbookFactory.create(is);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Extraer campos devueltos por el Stored Procedure
            String razonSocial = getStr(data, "razonSocial", "ESPPAPEL S.R.L.");
            String domicilioEmp = domicilioEmpresa != null && !domicilioEmpresa.trim().isEmpty()
                    ? domicilioEmpresa.trim()
                    : getStr(data, "domicilioEmpresa", "AV. MONTES NRO. 605");
            String trabajador = getStr(data, "trabajador", "");
            String estadoCivil = getStr(data, "estadoCivil", "");
            String edadStr = getStr(data, "edad", "");
            String domicilioTrabajador = getStr(data, "domicilioTrabajador", "");
            String cargo = getStr(data, "cargo", "");
            String ciNumero = getStr(data, "ciNumero", "");

            Date fechaIngreso = parseDate(getStr(data, "fechaIngreso", null));
            Date fechaSalida = parseDate(getStr(data, "fechaSalida", null));

            String motivo = motivoRetiro != null && !motivoRetiro.trim().isEmpty()
                    ? motivoRetiro.trim()
                    : getStr(data, "motivoRetiro", "RETIRO VOLUNTARIO");

            boolean esRetiroForzoso = motivo != null && (
                    motivo.trim().equalsIgnoreCase("RETIRO FORZOSO")
                    || motivo.trim().equalsIgnoreCase("RF")
                    || motivo.trim().toUpperCase().contains("FORZOSO")
            );

            // Nombres y montos de los últimos 3 meses anteriores
            String mes3Nombre = getStr(data, "mes3Nombre", "");
            String mes2Nombre = getStr(data, "mes2Nombre", "");
            String mes1Nombre = getStr(data, "mes1Nombre", "");

            // Remuneración de los últimos 3 meses: Haber Básico y Bono de Antigüedad por separado
            Double mes3HaberBasico = getDouble(data, "mes3HaberBasico");
            if (mes3HaberBasico == null || mes3HaberBasico == 0.0) {
                mes3HaberBasico = getDouble(data, "mes3Monto");
            }
            Double mes2HaberBasico = getDouble(data, "mes2HaberBasico");
            if (mes2HaberBasico == null || mes2HaberBasico == 0.0) {
                mes2HaberBasico = getDouble(data, "mes2Monto");
            }
            Double mes1HaberBasico = getDouble(data, "mes1HaberBasico");
            if (mes1HaberBasico == null || mes1HaberBasico == 0.0) {
                mes1HaberBasico = getDouble(data, "mes1Monto");
            }

            Double mes3BonoAntiguedad = getDouble(data, "mes3BonoAntiguedad");
            Double mes2BonoAntiguedad = getDouble(data, "mes2BonoAntiguedad");
            Double mes1BonoAntiguedad = getDouble(data, "mes1BonoAntiguedad");

            // Aguinaldo: Meses y Días trabajados en la gestión
            Double mesesAg = getDouble(data, "mesesAguinaldo");
            Double diasAg = getDouble(data, "diasAguinaldoResto");

            // Vacaciones acumuladas con duodécimas
            Double vacacionFiscal = getDouble(data, "vacacionFiscal");
            Double vacacionInterno = getDouble(data, "vacacionInterno");

            // Deducciones: Anticipos o pagos a cuenta / Quinquenios pagados
            Double pagoFiniquitoQuinq = getDouble(data, "pagoFiniquitoQuinq");

            // =========================================================================
            // Columnas desacopladas para pestaña INTERNO (Si el SP las envía en el futuro)
            // =========================================================================
            Double mes3HaberBasicoInt = getDoubleOrNull(data, "mes3HaberBasicoInterno");
            Double mes2HaberBasicoInt = getDoubleOrNull(data, "mes2HaberBasicoInterno");
            Double mes1HaberBasicoInt = getDoubleOrNull(data, "mes1HaberBasicoInterno");

            Double mes3BonoInt = getDoubleOrNull(data, "mes3BonoInterno");
            Double mes2BonoInt = getDoubleOrNull(data, "mes2BonoInterno");
            Double mes1BonoInt = getDoubleOrNull(data, "mes1BonoInterno");

            Double mesesAgInt = getDoubleOrNull(data, "mesesAgInterno");
            Double diasAgInt = getDoubleOrNull(data, "diasAgInterno");

            Double pagoFiniquitoQuinqInt = getDoubleOrNull(data, "pagoQuinqInterno");

            boolean tieneDatosInternos = (mes3HaberBasicoInt != null || mes2HaberBasicoInt != null || mes1HaberBasicoInt != null
                    || mes3BonoInt != null || mes2BonoInt != null || mes1BonoInt != null
                    || mesesAgInt != null || diasAgInt != null || pagoFiniquitoQuinqInt != null);

            // Pie de firma / Recibo
            String diaPie = getStr(data, "diaPie", "");
            String mesPie = getStr(data, "mesPie", "");
            String anioPie = getStr(data, "anioPie", "");

            // 3. Poblar cada una de las hojas de la plantilla (FISCAL e INTERNO)
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                boolean esFiscal = (i == 0 || "FISCAL".equalsIgnoreCase(sheet.getSheetName()));

                // A. DATOS GENERALES (Se llenan en AMBAS hojas con auto-fit de texto si es largo)
                setCellTextAutoFit(sheet, "W15", razonSocial, 32);
                setCellValue(sheet, "W16", ""); // Rama de actividad económica en blanco
                setCellTextAutoFit(sheet, "AF16", domicilioEmp, 30);
                setCellTextAutoFit(sheet, "W17", trabajador, 32);
                setCellValue(sheet, "M18", estadoCivil);
                setCellValue(sheet, "X18", edadStr);
                setCellTextAutoFit(sheet, "AF18", domicilioTrabajador, 30);
                setCellTextAutoFit(sheet, "R19", cargo, 32);
                setCellValue(sheet, "H20", ciNumero);
                setDateCellValue(sheet, "AA20", fechaIngreso);
                setDateCellValue(sheet, "AO20", fechaSalida);
                setCellValue(sheet, "M21", motivo);

                // TIEMPO DE SERVICIO (Row 22 y Row 23):
                // Asegurar que BB23 tiene formula con +1 para cálculo comercial inclusivo de días trabajados
                Cell cellBB23 = getOrCreateCell(sheet, "BB23");
                cellBB23.setCellFormula("IF(OR(AA20=\"\",AO20=\"\"),0,DAYS360(AA20,AO20)+1)");

                // Asegurar que AM22 tiene formula BB26 para computar los días tanto en el encabezado como en fila 40 (Z40=AM22)
                Cell cellAM22 = getOrCreateCell(sheet, "AM22");
                cellAM22.setCellFormula("BB26");

                if (esFiscal) {
                    // =========================================================
                    // 1. PESTAÑA FISCAL: Montos normales oficiales
                    // =========================================================
                    setCellValue(sheet, "P26", mes3Nombre);
                    setCellValue(sheet, "X26", mes2Nombre);
                    setCellValue(sheet, "AF26", mes1Nombre);

                    setNumericCellValue(sheet, "R27", mes3HaberBasico);
                    setNumericCellValue(sheet, "Z27", mes2HaberBasico);
                    setNumericCellValue(sheet, "AH27", mes1HaberBasico);

                    setNumericCellValue(sheet, "R30", mes3BonoAntiguedad);
                    setNumericCellValue(sheet, "Z30", mes2BonoAntiguedad);
                    setNumericCellValue(sheet, "AH30", mes1BonoAntiguedad);

                    // C) DESAHUCIO TRES MESES (EN CASO DE RETIRO FORZOSO en celda AP37)
                    Cell cellAP37 = getOrCreateCell(sheet, "AP37");
                    if (esRetiroForzoso) {
                        cellAP37.setCellFormula("AP35*3");
                    } else {
                        cellAP37.setBlank();
                    }

                    setNumericCellValue(sheet, "Z41", mesesAg);
                    setNumericCellValue(sheet, "AH41", diasAg);

                    Cell cellZ44 = getOrCreateCell(sheet, "Z44");
                    Cell cellAP44 = getOrCreateCell(sheet, "AP44");
                    if (vacacionFiscal != null && vacacionFiscal > 0) {
                        cellZ44.setCellValue(vacacionFiscal);
                    } else {
                        cellZ44.setBlank();
                    }
                    cellAP44.setCellFormula("IF(OR(Z44=\"\",ISBLANK(Z44),Z44<=0),0,AP35/30*Z44)");

                    if (pagoFiniquitoQuinq != null && pagoFiniquitoQuinq > 0) {
                        setNumericCellValue(sheet, "AP52", pagoFiniquitoQuinq);
                    } else {
                        setCellBlank(sheet, "AP52");
                    }

                    // V. IMPORTE DE LA SUMA CANCELADA (Literal en celda S64)
                    double montoLiquido = calcularMontoLiquido(workbook, sheet, data,
                            mes3HaberBasico, mes2HaberBasico, mes1HaberBasico,
                            mes3BonoAntiguedad, mes2BonoAntiguedad, mes1BonoAntiguedad,
                            mesesAg, diasAg, vacacionFiscal, pagoFiniquitoQuinq, esRetiroForzoso);
                    setCellValue(sheet, "S64", convertirMontoALetras(montoLiquido));

                } else if (tieneDatosInternos) {
                    // =========================================================
                    // 2. PESTAÑA INTERNO CON DATOS (Alimentado desde el SP)
                    // =========================================================
                    setCellValue(sheet, "P26", mes3Nombre);
                    setCellValue(sheet, "X26", mes2Nombre);
                    setCellValue(sheet, "AF26", mes1Nombre);

                    setNumericOrBlank(sheet, "R27", mes3HaberBasicoInt);
                    setNumericOrBlank(sheet, "Z27", mes2HaberBasicoInt);
                    setNumericOrBlank(sheet, "AH27", mes1HaberBasicoInt);

                    setNumericOrBlank(sheet, "R30", mes3BonoInt);
                    setNumericOrBlank(sheet, "Z30", mes2BonoInt);
                    setNumericOrBlank(sheet, "AH30", mes1BonoInt);

                    // C) DESAHUCIO TRES MESES (EN CASO DE RETIRO FORZOSO en celda AP37)
                    Cell cellAP37Int = getOrCreateCell(sheet, "AP37");
                    if (esRetiroForzoso) {
                        cellAP37Int.setCellFormula("AP35*3");
                    } else {
                        cellAP37Int.setBlank();
                    }

                    setNumericOrBlank(sheet, "Z41", mesesAgInt);
                    setNumericOrBlank(sheet, "AH41", diasAgInt);

                    Cell cellZ44 = getOrCreateCell(sheet, "Z44");
                    Cell cellAP44 = getOrCreateCell(sheet, "AP44");
                    if (vacacionInterno != null && vacacionInterno > 0) {
                        cellZ44.setCellValue(vacacionInterno);
                    } else {
                        cellZ44.setBlank();
                    }
                    cellAP44.setCellFormula("IF(OR(Z44=\"\",ISBLANK(Z44),Z44<=0),0,AP35/30*Z44)");

                    if (pagoFiniquitoQuinqInt != null && pagoFiniquitoQuinqInt > 0) {
                        setNumericCellValue(sheet, "AP52", pagoFiniquitoQuinqInt);
                    } else {
                        setCellBlank(sheet, "AP52");
                    }

                    double montoLiquidoInt = calcularMontoLiquido(workbook, sheet, data,
                            mes3HaberBasicoInt != null ? mes3HaberBasicoInt : 0.0,
                            mes2HaberBasicoInt != null ? mes2HaberBasicoInt : 0.0,
                            mes1HaberBasicoInt != null ? mes1HaberBasicoInt : 0.0,
                            mes3BonoInt != null ? mes3BonoInt : 0.0,
                            mes2BonoInt != null ? mes2BonoInt : 0.0,
                            mes1BonoInt != null ? mes1BonoInt : 0.0,
                            mesesAgInt, diasAgInt, vacacionInterno, pagoFiniquitoQuinqInt, esRetiroForzoso);
                    setCellValue(sheet, "S64", convertirMontoALetras(montoLiquidoInt));

                } else {
                    // =========================================================
                    // 3. PESTAÑA INTERNO SIN DATOS (Estado actual: Dejar en blanco)
                    // =========================================================
                    setCellBlank(sheet, "P26");
                    setCellBlank(sheet, "X26");
                    setCellBlank(sheet, "AF26");

                    setCellBlank(sheet, "R27");
                    setCellBlank(sheet, "Z27");
                    setCellBlank(sheet, "AH27");

                    setCellBlank(sheet, "R30");
                    setCellBlank(sheet, "Z30");
                    setCellBlank(sheet, "AH30");

                    setCellBlank(sheet, "AP37");

                    setCellBlank(sheet, "Z41");
                    setCellBlank(sheet, "AH41");

                    setCellBlank(sheet, "Z44");
                    setCellBlank(sheet, "AP44");

                    setCellBlank(sheet, "AP52");
                    setCellBlank(sheet, "S64"); // Sin texto literal en blanco
                }

                // Pie de recibo
                setCellValue(sheet, "T73", diaPie);
                setCellValue(sheet, "AB73", mesPie);
                setCellValue(sheet, "AP73", anioPie);

                // Forzar recálculo automático en Excel al abrir el archivo
                sheet.setForceFormulaRecalculation(true);
            }

            workbook.setForceFormulaRecalculation(true);

            // Intentar evaluar fórmulas internamente en POI para rellenar caché
            try {
                workbook.getCreationHelper().createFormulaEvaluator().evaluateAll();
            } catch (Exception evalEx) {
                logger.warn("Evaluación de fórmulas en POI parcial (se resolverán al abrir en Excel): {}", evalEx.getMessage());
            }

            workbook.write(out);
            return out.toByteArray();

        } catch (Exception e) {
            logger.error("Error al generar el archivo Excel de finiquito para empleado {}: {}", codEmpleado, e.getMessage(), e);
            throw new RuntimeException("Error al generar el archivo Excel de finiquito: " + e.getMessage(), e);
        }
    }

    private InputStream obtenerInputStreamPlantilla() throws Exception {
        ClassPathResource resource = new ClassPathResource(TEMPLATE_PATH);
        if (resource.exists()) {
            return resource.getInputStream();
        }

        File fileFallback = new File("src/main/resources/" + TEMPLATE_PATH);
        if (fileFallback.exists()) {
            return new FileInputStream(fileFallback);
        }

        File fileDirect = new File("reports/templates/PLANTILLA_FINIQUITO.xlsx");
        if (fileDirect.exists()) {
            return new FileInputStream(fileDirect);
        }

        InputStream clIs = getClass().getClassLoader().getResourceAsStream(TEMPLATE_PATH);
        if (clIs != null) {
            return clIs;
        }

        throw new FileNotFoundException("No se pudo localizar la plantilla de finiquito en el classpath: " + TEMPLATE_PATH);
    }

    private void setCellValue(Sheet sheet, String cellRefStr, String val) {
        Cell cell = getOrCreateCell(sheet, cellRefStr);
        if (cell != null) {
            cell.setCellValue(val != null ? val : "");
        }
    }

    private void setCellBlank(Sheet sheet, String cellRefStr) {
        Cell cell = getOrCreateCell(sheet, cellRefStr);
        if (cell != null) {
            cell.setBlank();
        }
    }

    private void setCellTextAutoFit(Sheet sheet, String cellRefStr, String val, int maxCharsNormal) {
        Cell cell = getOrCreateCell(sheet, cellRefStr);
        if (cell != null) {
            val = (val != null) ? val.trim() : "";
            cell.setCellValue(val);
            if (val.length() > maxCharsNormal) {
                Workbook wb = sheet.getWorkbook();
                CellStyle currentStyle = cell.getCellStyle();
                CellStyle newStyle = wb.createCellStyle();
                if (currentStyle != null) {
                    newStyle.cloneStyleFrom(currentStyle);
                }
                Font newFont = wb.createFont();
                newFont.setFontName("Arial");
                short fontSize = (val.length() > 42) ? (short) 7 : (short) 8;
                newFont.setFontHeightInPoints(fontSize);
                newStyle.setFont(newFont);
                cell.setCellStyle(newStyle);
            }
        }
    }

    private void setNumericCellValue(Sheet sheet, String cellRefStr, Double val) {
        Cell cell = getOrCreateCell(sheet, cellRefStr);
        if (cell != null) {
            cell.setCellValue(val != null ? val : 0.0);
        }
    }

    private void setNumericOrBlank(Sheet sheet, String cellRefStr, Double val) {
        Cell cell = getOrCreateCell(sheet, cellRefStr);
        if (cell != null) {
            if (val != null) {
                cell.setCellValue(val);
            } else {
                cell.setBlank();
            }
        }
    }

    private double calcularMontoLiquido(Workbook workbook, Sheet sheet, Map<String, Object> data,
                                       Double mes3Hb, Double mes2Hb, Double mes1Hb,
                                       Double mes3Ba, Double mes2Ba, Double mes1Ba,
                                       Double mesesAg, Double diasAg, Double diasVac,
                                       Double deduc, boolean esRetiroForzoso) {
        double montoLiquido = 0.0;
        try {
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            Cell cellAP57 = getOrCreateCell(sheet, "AP57");
            CellValue valLiq = evaluator.evaluate(cellAP57);
            if (valLiq != null && valLiq.getCellType() == CellType.NUMERIC) {
                montoLiquido = valLiq.getNumberValue();
            }
        } catch (Exception evalEx) {
            logger.warn("Evaluación de AP57 en POI parcial: {}", evalEx.getMessage());
        }

        if (montoLiquido <= 0.0) {
            double h3 = mes3Hb != null ? mes3Hb : 0.0;
            double h2 = mes2Hb != null ? mes2Hb : 0.0;
            double h1 = mes1Hb != null ? mes1Hb : 0.0;
            double b3 = mes3Ba != null ? mes3Ba : 0.0;
            double b2 = mes2Ba != null ? mes2Ba : 0.0;
            double b1 = mes1Ba != null ? mes1Ba : 0.0;

            double promedioIndem = (h3 + h2 + h1 + b3 + b2 + b1) / 3.0;
            Double aniosServ = getDouble(data, "aniosServicio");
            Double mesesServ = getDouble(data, "mesesServicio");
            Double diasServ = getDouble(data, "diasServicio");

            double desahucio = esRetiroForzoso ? (promedioIndem * 3.0) : 0.0;

            double indemAnios = (aniosServ != null ? aniosServ : 0.0) * promedioIndem;
            double indemMeses = ((mesesServ != null ? mesesServ : 0.0) / 12.0) * promedioIndem;
            double indemDias = ((diasServ != null ? diasServ : 0.0) / 360.0) * promedioIndem;

            double agMeses = ((mesesAg != null ? mesesAg : 0.0) / 12.0) * promedioIndem;
            double agDias = ((diasAg != null ? diasAg : 0.0) / 360.0) * promedioIndem;

            double vacMonto = (diasVac != null && diasVac > 0) ? (promedioIndem / 30.0 * diasVac) : 0.0;

            double totBeneficios = desahucio + indemAnios + indemMeses + indemDias + agMeses + agDias + vacMonto;
            double totDeducciones = (deduc != null && deduc > 0) ? deduc : 0.0;

            montoLiquido = Math.max(0.0, totBeneficios - totDeducciones);
        }
        return montoLiquido;
    }

    private void setDateCellValue(Sheet sheet, String cellRefStr, Date date) {
        if (date == null) return;
        Cell cell = getOrCreateCell(sheet, cellRefStr);
        if (cell != null) {
            CellStyle currentStyle = cell.getCellStyle();
            if (currentStyle == null || currentStyle.getDataFormat() == 0) {
                CellStyle dateStyle = sheet.getWorkbook().createCellStyle();
                if (currentStyle != null) {
                    dateStyle.cloneStyleFrom(currentStyle);
                }
                DataFormat df = sheet.getWorkbook().createDataFormat();
                dateStyle.setDataFormat(df.getFormat("dd/mm/yyyy"));
                cell.setCellStyle(dateStyle);
            }
            cell.setCellValue(date);
        }
    }

    private Cell getOrCreateCell(Sheet sheet, String cellRefStr) {
        CellReference ref = new CellReference(cellRefStr);
        Row row = sheet.getRow(ref.getRow());
        if (row == null) {
            row = sheet.createRow(ref.getRow());
        }
        Cell cell = row.getCell(ref.getCol());
        if (cell == null) {
            cell = row.createCell(ref.getCol());
        }
        return cell;
    }

    private String getStr(Map<String, Object> map, String key, String defaultVal) {
        if (map == null) return defaultVal;
        Object val = map.get(key);
        if (val == null) return defaultVal;
        String s = val.toString().trim();
        return s.isEmpty() ? defaultVal : s;
    }

    private Double getDouble(Map<String, Object> map, String key) {
        if (map == null) return 0.0;
        Object val = map.get(key);
        if (val == null) return 0.0;
        if (val instanceof Number) {
            return ((Number) val).doubleValue();
        }
        try {
            return Double.parseDouble(val.toString().trim());
        } catch (Exception e) {
            return 0.0;
        }
    }

    private Double getDoubleOrNull(Map<String, Object> map, String key) {
        if (map == null) return null;
        Object val = map.get(key);
        if (val == null) return null;
        if (val instanceof Number) {
            return ((Number) val).doubleValue();
        }
        try {
            String s = val.toString().trim();
            if (s.isEmpty() || s.equalsIgnoreCase("null")) return null;
            return Double.parseDouble(s);
        } catch (Exception e) {
            return null;
        }
    }

    private Date parseDate(String str) {
        if (str == null || str.trim().isEmpty() || str.trim().equals("-")) {
            return null;
        }
        String s = str.trim();
        String[] formats = {"dd/MM/yyyy", "yyyy-MM-dd", "dd-MM-yyyy", "yyyy/MM/dd"};
        for (String fmt : formats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(fmt);
                sdf.setLenient(false);
                return sdf.parse(s);
            } catch (ParseException ignored) {
            }
        }
        return null;
    }

    /**
     * Convierte un monto numérico a su expresión literal oficial en bolivianos.
     * Ejemplo: 24534.50 -> "VEINTICUATRO MIL QUINIENTOS TREINTA Y CUATRO 50/100 BOLIVIANOS"
     */
    public static String convertirMontoALetras(double monto) {
        if (Double.isNaN(monto) || Double.isInfinite(monto) || monto < 0) {
            monto = 0.0;
        }

        long entero = (long) Math.floor(monto);
        int centavos = (int) Math.round((monto - entero) * 100);
        if (centavos == 100) {
            entero++;
            centavos = 0;
        }

        String textoEntero;
        if (entero == 0) {
            textoEntero = "CERO";
        } else {
            textoEntero = convertirEntero(entero);
        }

        return String.format("%s %02d/100 BOLIVIANOS", textoEntero.trim(), centavos);
    }

    private static final String[] UNIDADES = {
            "", "UN", "DOS", "TRES", "CUATRO", "CINCO", "SEIS", "SIETE", "OCHO", "NUEVE",
            "DIEZ", "ONCE", "DOCE", "TRECE", "CATORCE", "QUINCE", "DIECISÉIS", "DIECISIETE", "DIECIOCHO", "DIECINUEVE", "VEINTE"
    };

    private static final String[] DECENAS = {
            "", "", "VEINTI", "TREINTA", "CUARENTA", "CINCUENTA", "SESENTA", "SETENTA", "OCHENTA", "NOVENTA"
    };

    private static final String[] CENTENAS = {
            "", "CIENTO", "DOSCIENTOS", "TRESCIENTOS", "CUATROCIENTOS", "QUINIENTOS",
            "SEISCIENTOS", "SETECIENTOS", "OCHOCIENTOS", "NOVECIENTOS"
    };

    private static String convertirEntero(long n) {
        if (n == 0) return "";
        if (n == 100) return "CIEN";

        if (n < 1000) {
            return convertirCentenas((int) n);
        }

        if (n < 1000000) {
            long miles = n / 1000;
            long resto = n % 1000;
            String textoMiles = (miles == 1) ? "MIL" : (convertirEntero(miles) + " MIL");
            if (resto == 0) {
                return textoMiles;
            }
            return textoMiles + " " + convertirCentenas((int) resto);
        }

        if (n < 1000000000000L) {
            long millones = n / 1000000;
            long resto = n % 1000000;
            String textoMillones = (millones == 1) ? "UN MILLÓN" : (convertirEntero(millones) + " MILLONES");
            if (resto == 0) {
                return textoMillones;
            }
            return textoMillones + " " + convertirEntero(resto);
        }

        return String.valueOf(n);
    }

    private static String convertirCentenas(int n) {
        if (n == 0) return "";
        if (n == 100) return "CIEN";

        int c = n / 100;
        int d = n % 100;

        String res = "";
        if (c > 0) {
            res = CENTENAS[c];
        }

        if (d > 0) {
            String textoDecenas = convertirDecenas(d);
            if (!res.isEmpty()) {
                res += " " + textoDecenas;
            } else {
                res = textoDecenas;
            }
        }

        return res;
    }

    private static String convertirDecenas(int n) {
        if (n <= 20) {
            return UNIDADES[n];
        }

        if (n < 30) {
            if (n == 21) return "VEINTIÚN";
            if (n == 22) return "VEINTIDÓS";
            if (n == 23) return "VEINTITRÉS";
            if (n == 26) return "VEINTISÉIS";
            return "VEINTI" + UNIDADES[n - 20];
        }

        int d = n / 10;
        int u = n % 10;

        if (u == 0) {
            return DECENAS[d];
        }

        return DECENAS[d] + " Y " + UNIDADES[u];
    }
}
