package bo.bosque.com.impexpap.controller;
import bo.bosque.com.impexpap.commons.JasperReportExport;
import bo.bosque.com.impexpap.dao.*;
import bo.bosque.com.impexpap.model.*;
import bo.bosque.com.impexpap.utils.Tipos;
import bo.bosque.com.impexpap.utils.Utiles;
import org.apache.poi.ss.usermodel.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

@RestController
@CrossOrigin("*")
@RequestMapping("/tigo")
public class TigoController {
    private JdbcTemplate jdbcTemplate;

    private final IFacturaTigo fTigoDao;
    private final ISociosTigo  sTigoDao;
    private final ITigoEjecutado eTigoDao;


    public TigoController(JdbcTemplate jdbcTemplate,IFacturaTigo fTigo,ISociosTigo sTigo,ITigoEjecutado eTigo) {
        this.jdbcTemplate = jdbcTemplate;

        this.fTigoDao    = fTigo;
        this.sTigoDao    = sTigo;
        this.eTigoDao    = eTigo;
    }
    /**
     * Procedimiento para registrar o actualizar una factura Tigo
     * @param ft FacturaTigo
     * @return ResponseEntity con estado de operación
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/registrarFacturaTigo")
    public ResponseEntity<?> registrarFacturaTigo(@RequestBody FacturaTigo ft) {
        Map<String, Object> response = new HashMap<>();

        // Validación de acción: Insertar si codFactura es 0
        String acc = "U";
        if (ft.getCodFactura() == 0) {
            acc = "I";
        }

        // Ejecutar SP vía DAO
        if (!this.fTigoDao.registrarFacturaTigo(ft, acc)) {
            response.put("msg", "Error al registrar la factura Tigo");
            response.put("ok", "error");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        response.put("msg", "Factura Tigo registrada correctamente");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    /**
     * Procedimiento para que obtendra los correos por persona
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM"})
    @PostMapping("/obtenerDetalleDeudaTigo")
    public List<FacturaTigo> obtenerDetalleDeuda ( @RequestBody FacturaTigo fTigo ){

        List<FacturaTigo> lstFTigo = this.fTigoDao.obtenerDetalleDeudaTigo( );
        if(lstFTigo.size() == 0) return new ArrayList<>();
        return lstFTigo;
    }
    /**
     * procedimiento para subir la FACTURA TIGO EXCEL
     * @param file
     * @param codEmpleado
     * @return
     */
    @PostMapping("/SubirExcel")
    public ResponseEntity<?> uploadExcel(@RequestParam("file") MultipartFile file, @RequestParam("audUsuario") int audUsuario) {
        Map<String, Object> response = new HashMap<>();

        if (!file.isEmpty()) {
            String extension = "";
            String originalName = file.getOriginalFilename();
            int i = originalName.lastIndexOf('.');
            if (i > 0) {
                extension = originalName.substring(i);
            }
            String nombreArchivo = audUsuario + extension;
            Path rutaArchivo = Paths.get("uploads").resolve(nombreArchivo).toAbsolutePath();

            try {
                Files.copy(file.getInputStream(), rutaArchivo, StandardCopyOption.REPLACE_EXISTING);

                // Procesar el archivo Excel y guardar en la BD usando el DAO
                try (InputStream fis = Files.newInputStream(rutaArchivo);
                     Workbook workbook = WorkbookFactory.create(fis)) {

                    // Busca la hoja por nombre
                    Sheet sheet = workbook.getSheet("DETALLE DEUDA");
                    if (sheet == null) {
                        response.put("msg", "No se encontró la hoja 'DETALLE DEUDA'");
                        response.put("ok", "error");
                        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                    }

                    // Detecta automáticamente la fila de encabezado
                    String[] columnas = {
                            "N° Factura",
                            "Tipo Servicio",
                            "N° Contrato",
                            "N° Cuenta",
                            "Período Cobrado",
                            "Descripción del Plan",
                            "Total cobrado por Cuenta Bs."
                    };

                    Row headerRow = null;
                    int headerRowIndex = -1;
                    for (int fila = 0; fila <= sheet.getLastRowNum(); fila++) {
                        Row row = sheet.getRow(fila);
                        if (row == null) continue;
                        int coincidencias = 0;
                        for (Cell cell : row) {
                            String valor = getCellValue(cell);
                            for (String esperado : columnas) {
                                if (valor.equalsIgnoreCase(esperado)) {
                                    coincidencias++;
                                    break;
                                }
                            }
                        }
                        if (coincidencias >= 5) { // umbral ajustable
                            headerRow = row;
                            headerRowIndex = fila;
                            break;
                        }
                    }

                    if (headerRow == null) {
                        response.put("msg", "No se encontró la fila de encabezado en la hoja 'DETALLE DEUDA'");
                        response.put("ok", "error");
                        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                    }

                    // Mapea los nombres de columna a su índice
                    Map<String, Integer> colIndex = new HashMap<>();
                    for (Cell cell : headerRow) {
                        String colName = getCellValue(cell);
                        colIndex.put(colName, cell.getColumnIndex());
                    }

                    // Procesa las filas (después del encabezado)
                    for (int fila = headerRowIndex + 1; fila <= sheet.getLastRowNum(); fila++) {
                        Row row = sheet.getRow(fila);
                        if (row == null) continue;
                        //System.out.println("NroContrato leído: '" + getCellValue(row.getCell(colIndex.get("N° Contrato"))) + "'");
                        //System.out.println("NroCuenta leído: '" + getCellValue(row.getCell(colIndex.get("N° Cuenta"))) + "'");

                        // Lee el valor de la primera celda
                        String primerValor = getCellValue(row.getCell(colIndex.get("N° Factura")));
                        // Si la fila está vacía o es la fila de "Total", ignora
                        if (primerValor == null || primerValor.trim().isEmpty() || primerValor.trim().equalsIgnoreCase("Total")) continue;
                        // 🔍 Filtrar por N° Cuenta
                        Integer nroContrato = parseIntCell(row, colIndex.get("N° Contrato"));
                        if (nroContrato == null || nroContrato != 9268908) continue;

                        // Puedes agregar más validaciones si hay otras filas de resumen
                        // Ejemplo: si la columna de "Total cobrado por Cuenta Bs." no es numérica, ignora

                        FacturaTigo factura = new FacturaTigo();
                        factura.setNroFactura(primerValor);
                        factura.setTipoServicio(getCellValue(row.getCell(colIndex.get("Tipo Servicio"))));
                        factura.setNroContrato(parseIntCell(row, colIndex.get("N° Contrato")));
                        factura.setNroCuenta(parseIntCell(row, colIndex.get("N° Cuenta")));
                        // En vez de parsePeriodoCobrado(row, colIndex.get("Período Cobrado"))
                        factura.setPeriodoCobrado(getCellValue(row.getCell(colIndex.get("Período Cobrado"))).replace(" ", "")); // "2025-08"
                        factura.setDescripcionPlan(getCellValue(row.getCell(colIndex.get("Descripción del Plan"))));
                        factura.setTotalCobradoXCuenta(parseFloatCell(row, colIndex.get("Total cobrado por Cuenta Bs.")));
                        factura.setAudUsuario(audUsuario);
                        factura.setEstado("No ejecutado");


                        fTigoDao.registrarFacturaTigo(factura, "A");
                    }
                }

                response.put("msg", "Archivo Excel subido y datos guardados correctamente");
                response.put("ok", "success");
                response.put("nombreArchivo", nombreArchivo);

            } catch (Exception e) {
                e.printStackTrace(); // Esto mostrará el error en la consola del backend
                response.put("msg", "Error interno: " + e.getMessage());
                response.put("ok", "error");
                return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            response.put("msg", "No se recibió ningún archivo");
            response.put("ok", "error");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }



    private int parseIntCell(Row row, Integer index) {
        if (index == null) return 0;
        Cell cell = row.getCell(index);
        if (cell == null) return 0;
        switch (cell.getCellType()) {
            case NUMERIC:
                return (int) cell.getNumericCellValue();
            case STRING:
                String val2 = cell.getStringCellValue().trim();
                try { return Integer.parseInt(val2); } catch (Exception e) { return 0; }
            case FORMULA:
                if (cell.getCachedFormulaResultType() == CellType.NUMERIC) {
                    return (int) cell.getNumericCellValue();
                } else if (cell.getCachedFormulaResultType() == CellType.STRING) {
                    String val3 = cell.getStringCellValue().trim();
                    try { return Integer.parseInt(val3); } catch (Exception e) { return 0; }
                }
                return 0;
            default:
                return 0;
        }
    }

    private float parseFloatCell(Row row, Integer index) {
        String val = getCellValue(row.getCell(index)).replace(",", "."); // Por si hay coma decimal
        try { return Float.parseFloat(val); } catch (Exception e) { return 0f; }
    }


    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf(cell.getNumericCellValue()).trim();
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                switch (cell.getCachedFormulaResultType()) {
                    case STRING:
                        return cell.getStringCellValue().trim();
                    case NUMERIC:
                        if (DateUtil.isCellDateFormatted(cell)) {
                            return cell.getDateCellValue().toString();
                        } else {
                            return String.valueOf(cell.getNumericCellValue()).trim();
                        }
                    case BOOLEAN:
                        return String.valueOf(cell.getBooleanCellValue());
                    default:
                        return "";
                }
            default:
                return "";
        }
    }

//obtener socios Tigo
    @Secured({ "ROLE_ADM", "ROLE_LIM"})
    @PostMapping("/obtenerSociosTigo")
    public List<SociosTigo> obtenerSociosTigo ( @RequestBody SociosTigo fTigo ){

        List<SociosTigo> lstSTigo = this.sTigoDao.obtenerSociosTIGO( );
        if(lstSTigo.size() == 0) return new ArrayList<>();
        return lstSTigo;
    }
    /**
     * OBTENER TIGO EJECUTADO TOTAL COBRADO X CUENTA
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM"})
    @PostMapping("/obtenerTotalCobradoXCuenta")
    public List<TigoEjecutado> obtenerTigo ( @RequestBody TigoEjecutado eTigo ){
        String periodo = eTigo.getPeriodoCobrado();

        List<TigoEjecutado> lstSTigo = this.eTigoDao.obtenerTotalCobradoXCuenta(periodo );
        if(lstSTigo.size() == 0) return new ArrayList<>();
        return lstSTigo;
    }
/**
 *PROCEDIMIENTO PARA REGISTRAR UN SOCIO DE CONSUMO TIGO
 */
    /**
     * Procedimiento para el registro de Email
     * @param
     * @return
     */
    @Secured ( { "ROLE_ADM", "ROLE_LIM" }  )
    @PostMapping("/registroSocioTigo")
    public ResponseEntity<?> registroSocioTigo( @RequestBody SociosTigo st ){
        Map<String, Object> response = new HashMap<>();


        String acc = "U";
        if( st.getCodCuenta() == 0 ){
            acc = "I";
        }


        if( !this.sTigoDao.registrarSociosTigo( st, acc ) ){
            response.put("msg", "Error al Actualizar los Datos del Empleado");
            response.put("error", "ok");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        response.put("msg", "Datos Actualizados");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    /**
     * SUBIR ANTICIPOS CONSUMO TIGO
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/generarAnticiposTigo")
    public ResponseEntity<?> generarAnticiposTigo(@RequestBody TigoEjecutado eTigo ) {
        Map<String, Object> response = new HashMap<>();
        String periodo = eTigo.getPeriodoCobrado();
        String accion = "A";

        if (!this.eTigoDao.generarAnticiposTigo(periodo)) {
            response.put("msg", "Error al generar anticipos Tigo");
            response.put("error", "true");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        response.put("msg", "Anticipos generados correctamente");
        response.put("ok", "true");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    /**
     * OBTENER RESUMEN CUENTAS CONSUMO TIGO
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM"})
    @PostMapping("/obtenerResumenCuentas")
    public List<TigoEjecutado> obtenerResumenCuentas ( @RequestBody TigoEjecutado eTigo ){
        String periodo = eTigo.getPeriodoCobrado();
        List<TigoEjecutado> lstSTigo = this.eTigoDao.obtenerResumenCuentas(periodo);
        if(lstSTigo.size() == 0) return new ArrayList<>();
        return lstSTigo;
    }
    /**
     * OBTENER RESUMEN DETALLADO X CUENTA TIGO
     */
    @PostMapping("/obtenerResumenDetallado")
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    public List<TigoEjecutado> obtenerDetalleXCuentas(@RequestBody TigoEjecutado eTigo) {
        String periodo = eTigo.getPeriodoCobrado();
        List<TigoEjecutado> lstSTigo = this.eTigoDao.obtenerDetalleXCuentas(periodo);
        return lstSTigo.isEmpty() ? new ArrayList<>() : lstSTigo;
    }

    /**
     * Procedimiento para exportar consumo tigo a pdf
     * @param emp
     * @return
     */
    @PostMapping("/pdfTigo")
    public ResponseEntity<?> exportPDF(@RequestBody TigoEjecutado eTigo)  {

        String nombreReporte = "RptConsumoTigo";


        try{
            Map<String, Object> params = new HashMap<>();
            params.put("periodoCobrado", eTigo.getPeriodoCobrado() );
            byte[] reportBytes = new JasperReportExport( this.jdbcTemplate).exportPDFStatic( nombreReporte, params);


            HttpHeaders headers = new HttpHeaders();
            //set the PDF format
            headers.setContentLength(reportBytes.length);
            headers.setContentType(MediaType.APPLICATION_PDF);

            return new ResponseEntity<>(reportBytes,headers ,HttpStatus.OK);
        } catch(Exception e) {
            e.printStackTrace();  // 🔥 Imprime el stack trace COMPLETO en consola para ver el error real
            System.err.println("Error detallado: " + e.getClass().getName() + " - " + e.getMessage());  // Imprime tipo y mensaje
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }


    }
    /**
     *
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM"})
    @PostMapping("/obtenerListaGruposTigo")
    public List<SociosTigo> obtenerListaGruposTigo ( @RequestBody SociosTigo fTigo ){
        String periodo = fTigo.getPeriodoCobrado();
        List<SociosTigo> lstSTigo = this.sTigoDao.obtenerListaGruposTigo(periodo);
        if(lstSTigo.size() == 0) return new ArrayList<>();
        return lstSTigo;
    }
    /**
     * PARA ELIMINAR UN GRUPO TIGO
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @DeleteMapping("/grupo/{codCuenta}")
    public ResponseEntity<Map<String, Object>> eliminarGrupo(@PathVariable int codCuenta) {
        Map<String, Object> response = new HashMap<>();

        SociosTigo temp = new SociosTigo();
        temp.setCodCuenta(codCuenta);

        boolean eliminado = this.sTigoDao.registrarSociosTigo(temp, "D");

        if (!eliminado) {
            System.out.println("❌ Error al eliminar el grupo: " + codCuenta);
            response.put("msg", "Error al eliminar el correo.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        System.out.println("✅ grupo eliminado correctamente: " + codCuenta);
        response.put("msg", "Correo eliminado correctamente.");
        return ResponseEntity.ok(response);
    }
    /**
     * registrar tigo ejecutado
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/ejecutarTigo")
    public ResponseEntity<?> registrarTigoEjecutado(@RequestBody TigoEjecutado te) {
        Map<String, Object> response = new HashMap<>();


        // Ejecutar SP vía DAO
        if (!this.eTigoDao.registrarTigoEjecutado(te, "G")) {
            response.put("msg", "Error al registrar la factura Tigo");
            response.put("ok", "error");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        response.put("msg", "Factura Tigo registrada correctamente");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    /**
     * obtener lista tigo ejectuado
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/obtenerTigoEjecutado")
    public List<TigoEjecutado> obtenerTigoEjecutado(@RequestBody TigoEjecutado eTigo) {

        String periodo = eTigo.getPeriodoCobrado();
        String empresa = eTigo.getEmpresa();

        System.out.println("→ Parámetros recibidos: empresa=" + empresa + ", periodo=" + periodo);

        // Recibe la LISTA PLANA, que contiene las filas de encabezado, detalle y el total.
        List<TigoEjecutado> flat = this.eTigoDao.obtenerTigoEjecutado(empresa,periodo);

        System.out.println("→ Registros recibidos desde SQL: " + (flat != null ? flat.size() : 0));

        if (flat == null || flat.isEmpty()) return new ArrayList<>();

        TigoEjecutado resumen = null;

        // --- 1. Separar la fila resumen "ZZZ TOTAL" (codEmpleado = 9999) ---
        Iterator<TigoEjecutado> it = flat.iterator();
        while (it.hasNext()) {
            TigoEjecutado t = it.next();

            if (t != null && t.getCodEmpleado() != null && t.getCodEmpleado().equals(9999)) {
                resumen = t;
                it.remove();
                System.out.println("→ Fila resumen detectada y separada.");
                break;
            }
        }

        // --- 2. Indexación y Separación de Nodos (Padres vs. Hijos) ---

        // Mapa para indexar SOLAMENTE a los Jefes/Grupos (codEmpleadoPadre = 0).
        Map<Integer, TigoEjecutado> parentsById = new HashMap<>();
        List<TigoEjecutado> roots = new ArrayList<>();
        List<TigoEjecutado> children = new ArrayList<>();

        for (TigoEjecutado t : flat) {
            if (t == null) continue;
            // Inicializar la lista de hijos si es nula
            if (t.getItems() == null) t.setItems(new ArrayList<>());

            Integer codEmpleado = t.getCodEmpleado();
            Integer padreId = t.getCodEmpleadoPadre();

            // Los ENCABEZADOS DE GRUPO (RAÍCES) tienen codEmpleadoPadre = 0
            if (padreId != null && padreId == 0) {
                // Indexar por el ID del jefe/grupo (e.g., -6, 172)
                parentsById.put(codEmpleado, t);
                roots.add(t);
                System.out.println("→ Nodo Raíz indexado: " + t.getNombreCompleto());
            } else {
                // Los DETALLES/SOCIOS (HIJOS) tienen codEmpleado = 0 y codEmpleadoPadre != 0
                children.add(t);
            }
        }

        // --- 3. Construcción del Árbol (Asignar Hijos a Padres) ---

        for (TigoEjecutado child : children) {
            Integer padreId = child.getCodEmpleadoPadre();

            // Buscar al padre en el mapa indexado por su ID real
            TigoEjecutado parent = parentsById.get(padreId);

            if (parent != null) {
                parent.getItems().add(child);
                System.out.println("→ Agregado hijo: " + child.getCorporativo() + " → Padre: " + parent.getNombreCompleto());
            } else {
                // Caso de datos huérfanos
                roots.add(child);
                System.out.println("→ ADVERTENCIA: Padre " + padreId + " no encontrado, tratado como raíz: " + child.getNombreCompleto());
            }
        }

        // --- 4. Ordenamiento Final y Resumen ---

        sortTreeByNombreCompleto(roots);

        // Agregar resumen como último nodo raíz (Java lo espera así)
        if (resumen != null) {
            resumen.setItems(new ArrayList<>());
            roots.add(resumen);
        }

        System.out.println("→ Total nodos raíz devueltos: " + roots.size());
        return roots;
    }
    /**
     * OBTENER TIGO EJECUTADO TOTAL COBRADO X CUENTA
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM"})
    @PostMapping("/obtenerNroSinAsignar")
    public List<SociosTigo> obtenerTigo ( @RequestBody SociosTigo sTigo ){
        String periodo = sTigo.getPeriodoCobrado();

        List<SociosTigo> lstSTigo = this.sTigoDao.obtenerNumerosSinAsignar(periodo );
        if(lstSTigo.size() == 0) return new ArrayList<>();
        return lstSTigo;
    }
    /**
     * obtener el arbol detallado
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/obtenerArboldetallado")
    public List<TigoEjecutado> obtenerArbolDetallado(@RequestBody TigoEjecutado eTigo) {

        String periodo = eTigo.getPeriodoCobrado();
        String empresa = eTigo.getEmpresa();

        System.out.println("→ Parámetros recibidos: empresa=" + empresa + ", periodo=" + periodo);

        // La consulta SQL ya devuelve los datos ordenados correctamente (Encabezado, Detalle, Socio)
        List<TigoEjecutado> flat = this.eTigoDao.obtenerArbolDetallado(empresa, periodo);
        System.out.println("→ Registros recibidos desde SQL: " + (flat != null ? flat.size() : 0));

        if (flat == null || flat.isEmpty()) return new ArrayList<>();

        TigoEjecutado resumen = null;

        // --- 1. Separar la fila resumen "ZZZ TOTAL" ---
        Iterator<TigoEjecutado> it = flat.iterator();
        while (it.hasNext()) {
            TigoEjecutado t = it.next();
            // Su lógica de búsqueda de resumen (asumo que getCodEmpleado() devuelve un Integer o String '9999')
            if (t != null && t.getCodEmpleado() != null &&
                    t.getCodEmpleado().equals(9999) && // Uso directo del ID si es Integer
                    t.getNombreCompleto() != null &&
                    t.getNombreCompleto().startsWith("ZZZ TOTAL")) {
                resumen = t;
                it.remove();
                System.out.println("→ Fila resumen detectada y separada: " + resumen.getNombreCompleto());
                break;
            }
        }

        // --- 2. Indexación y Separación de Nodos (Padres vs. Hijos) ---

        // Indexar SOLAMENTE a los Jefes/Encabezados (donde codEmpleadoPadre es 0).
        // Usamos el codEmpleado del jefe como clave (e.g., -6, 172, 32).
        Map<Integer, TigoEjecutado> parentsById = new HashMap<>();
        List<TigoEjecutado> roots = new ArrayList<>();
        List<TigoEjecutado> children = new ArrayList<>(); // Lista temporal para todos los hijos/detalles

        for (TigoEjecutado t : flat) {
            if (t == null) continue;
            // Inicializar la lista de hijos, si es nula
            if (t.getItems() == null) t.setItems(new ArrayList<>());

            Integer padreId = t.getCodEmpleadoPadre();

            if (padreId != null && padreId == 0) {
                // Es una RAÍZ/ENCABEZADO (su codEmpleado es el ID del jefe: -6, 172, 32)
                parentsById.put(t.getCodEmpleado(), t);
                roots.add(t);
                System.out.println("→ Nodo Raíz identificado e indexado: " + t.getNombreCompleto() + " (ID: " + t.getCodEmpleado() + ")");
            } else {
                // Es un HIJO/DETALLE/SOCIO (su codEmpleadoPadre apunta al ID del jefe)
                children.add(t);
                System.out.println("→ Nodo Hijo separado: " + t.getCorporativo() + " (Padre ID: " + padreId + ")");
            }
        }

        // --- 3. Construcción del Árbol (Asignar Hijos a Padres) ---

        for (TigoEjecutado child : children) {
            Integer padreId = child.getCodEmpleadoPadre();

            // Buscar al padre por el codEmpleadoPadre del hijo (e.g., -6, 172, 32)
            TigoEjecutado parent = parentsById.get(padreId);

            if (parent != null) {
                // El padre es una Raíz indexada. Añadir el hijo a su lista de items.
                parent.getItems().add(child);
                // No es necesario ordenar los hijos aquí, ya que el SQL los trae ordenados (OrdenFijo, Corporativo).
            } else {
                // Este caso es una contingencia (padre no se encontró en la lista de raíces).
                roots.add(child);
                System.out.println("→ ADVERTENCIA: Padre " + padreId + " no encontrado, tratado como raíz: " + child.getNombreCompleto());
            }
        }

        // --- 4. Ordenamiento Final y Resumen ---

        // Ordenar solo las raíces (la lista 'roots') por nombreCompleto
        // Los hijos ya están ordenados por la cláusula ORDER BY de SQL (OrdenFijo, corporativo)
        sortTreeByNombreCompleto(roots);

        // Agregar resumen como último nodo raíz
        if (resumen != null) {
            // Asegurarse de que el resumen no tenga hijos
            resumen.setItems(new ArrayList<>());
            roots.add(resumen);
            System.out.println("→ Resumen agregado al final del árbol.");
        }

        System.out.println("→ Total nodos raíz devueltos: " + roots.size());
        return roots;
    }

    /**
     * Lógica para ordenar las raíces del árbol (y recursivamente sus hijos).
     * Mantiene la lógica original de ordenar por nombreCompleto y manejar el total.
     */
    private void sortTreeByNombreCompleto(List<TigoEjecutado> nodes) {
        if (nodes == null || nodes.isEmpty()) return;

        // Primero, ordenar recursivamente los hijos de cada nodo
        for (TigoEjecutado node : nodes) {
            sortTreeByNombreCompleto(node.getItems());
        }

        // Luego, ordenar la lista de nodos actual
        nodes.sort((a, b) -> {
            // Asumo que getFila() corresponde a la columna 'codEmpleado' del total, que es 9999
            boolean aIsTotal = a.getCodEmpleado() != null && a.getCodEmpleado().equals(9999);
            boolean bIsTotal = b.getCodEmpleado() != null && b.getCodEmpleado().equals(9999);

            if (aIsTotal && !bIsTotal) return 1;
            if (!aIsTotal && bIsTotal) return -1;

            String nombreA = a.getNombreCompleto() != null ? a.getNombreCompleto() : "";
            String nombreB = b.getNombreCompleto() != null ? b.getNombreCompleto() : "";
            return nombreA.compareToIgnoreCase(nombreB);
        });
    }


}
