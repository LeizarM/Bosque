package bo.bosque.com.impexpap.controller;
import bo.bosque.com.impexpap.commons.JasperReportExport;
import bo.bosque.com.impexpap.dao.*;
import bo.bosque.com.impexpap.model.*;
import bo.bosque.com.impexpap.utils.ApiResponse;
import bo.bosque.com.impexpap.utils.RespuestaSp;
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
    private final ICambiosTigo cTigoDao;
    private final IChipTigo ctTigoDao;


    public TigoController(JdbcTemplate jdbcTemplate
            ,IFacturaTigo fTigo
            ,ISociosTigo sTigo
            ,ITigoEjecutado eTigo
            ,ICambiosTigo cTigo
            ,IChipTigo ctTigo) {
        this.jdbcTemplate = jdbcTemplate;

        this.fTigoDao    = fTigo;
        this.sTigoDao    = sTigo;
        this.eTigoDao    = eTigo;
        this.cTigoDao    = cTigo;
        this.ctTigoDao   = ctTigo;
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

                    // --- INICIO DEL CAMBIO: Agrupación en Java ---
                    Map<Integer, FacturaTigo> facturasAgrupadas = new HashMap<>();

                    for (int fila = headerRowIndex + 1; fila <= sheet.getLastRowNum(); fila++) {
                        Row row = sheet.getRow(fila);
                        if (row == null) continue;

                        String primerValor = getCellValue(row.getCell(colIndex.get("N° Factura")));
                        if (primerValor == null || primerValor.trim().isEmpty() || primerValor.trim().equalsIgnoreCase("Total")) continue;

                        Integer nroContrato = parseIntCell(row, colIndex.get("N° Contrato"));
                        if (nroContrato == null || nroContrato != 9268908) continue;

                        Integer nroCuenta = parseIntCell(row, colIndex.get("N° Cuenta"));
                        float montoFila = parseFloatCell(row, colIndex.get("Total cobrado por Cuenta Bs."));

                        if (facturasAgrupadas.containsKey(nroCuenta)) {
                            // Si la cuenta ya existe en el mapa, sumamos el monto
                            FacturaTigo fExistente = facturasAgrupadas.get(nroCuenta);
                            fExistente.setTotalCobradoXCuenta(fExistente.getTotalCobradoXCuenta() + montoFila);
                        } else {
                            // Si es nueva, creamos el objeto completo
                            FacturaTigo nuevaFactura = new FacturaTigo();
                            nuevaFactura.setNroFactura(primerValor);
                            nuevaFactura.setTipoServicio(getCellValue(row.getCell(colIndex.get("Tipo Servicio"))));
                            nuevaFactura.setNroContrato(nroContrato);
                            nuevaFactura.setNroCuenta(nroCuenta);
                            nuevaFactura.setPeriodoCobrado(getCellValue(row.getCell(colIndex.get("Período Cobrado"))).replace(" ", ""));
                            nuevaFactura.setDescripcionPlan(getCellValue(row.getCell(colIndex.get("Descripción del Plan"))));
                            nuevaFactura.setTotalCobradoXCuenta(montoFila);
                            nuevaFactura.setAudUsuario(audUsuario);
                            nuevaFactura.setEstado("No ejecutado");

                            facturasAgrupadas.put(nroCuenta, nuevaFactura);
                        }
                    }

                    // --- GUARDADO FINAL: Una sola llamada por número de cuenta ---
                    for (FacturaTigo factura : facturasAgrupadas.values()) {
                        fTigoDao.registrarFacturaTigo(factura, "A");
                    }
                    // --- FIN DEL CAMBIO ---
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

        try {
            if (!this.eTigoDao.generarAnticiposTigo(periodo)) {
                response.put("msg", "Error al generar anticipos Tigo (El SP devolvió 0 filas)");
                response.put("ok", "error");
                return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
            }

            response.put("msg", "Anticipos generados correctamente");
            response.put("ok", "true");
            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (RuntimeException ex) {
            // Captura errores personalizados (RAISERROR) o de ambiente
            response.put("msg", ex.getMessage());
            response.put("ok", "error_sp");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            response.put("msg", "Error interno al procesar anticipos.");
            response.put("ok", "error_interno");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
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
        try {
            // Ejecutar SP vía DAO
            if (!this.eTigoDao.registrarTigoEjecutado(te, "G")) {
                response.put("msg", "Error al registrar la factura Tigo (Fallo DAO)");
                response.put("ok", "error");
                return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
            }

            response.put("msg", "Factura Tigo ejecutada correctamente");
            response.put("ok", "ok");
            return new ResponseEntity<>(response, HttpStatus.CREATED);

        } catch (RuntimeException ex) { // <-- NUEVO CATCH para el error del SP
            String spMessage = ex.getMessage();
            if (spMessage != null) {
                // Devolver 400 (Bad Request) con el mensaje extraído
                response.put("msg", spMessage);
                response.put("ok", "error_sp");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST); // <-- CLAVE: HTTP 400
            }
            throw ex;

        } catch (Exception e) {
            // Capturar otros errores no previstos
            response.put("msg", "Error interno del servidor.");
            response.put("ok", "error_interno");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // Ejecutar SP vía DAO
        /*if (!this.eTigoDao.registrarTigoEjecutado(te, "G")) {
            response.put("msg", "Error al registrar la factura Tigo");
            response.put("ok", "error");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        response.put("msg", "Factura Tigo registrada correctamente");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);*/
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

        //System.out.println("→ Registros recibidos desde SQL: " + (flat != null ? flat.size() : 0));

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
                //System.out.println("→ Nodo Raíz indexado: " + t.getNombreCompleto());
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
                //System.out.println("→ Agregado hijo: " + child.getCorporativo() + " → Padre: " + parent.getNombreCompleto());
            } else {
                // Caso de datos huérfanos
                roots.add(child);
               // System.out.println("→ ADVERTENCIA: Padre " + padreId + " no encontrado, tratado como raíz: " + child.getNombreCompleto());
            }
        }

        // --- 4. Ordenamiento Final y Resumen ---

        sortTreeByNombreCompleto(roots);

        // Agregar resumen como último nodo raíz (Java lo espera así)
        if (resumen != null) {
            resumen.setItems(new ArrayList<>());
            roots.add(resumen);
        }

        //System.out.println("→ Total nodos raíz devueltos: " + roots.size());
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
    /**
     * Procedimiento para exportar rpt cambios tigo a pdf
     * @param emp
     * @return
     */
    @PostMapping("/RptCambiosTigo")
    public ResponseEntity<?> descargarRptLineasCorporativas(@RequestBody TigoEjecutado eTigo)  {

        String nombreReporte = "RptLineasCorporativas";


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
     * Endpoint para actualizar empresa en lotes
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/actualizarEmpresaLote")
    public ResponseEntity<?> actualizarEmpresaLote(@RequestBody TigoEjecutado te) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Validamos que llegue la lista de IDs
            if (te.getListaCodEmpleado() == null || te.getListaCodEmpleado().isEmpty()) {
                response.put("msg", "Debe seleccionar al menos un empleado");
                response.put("ok", "error");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            if (this.eTigoDao.actualizarEmpresaLote(te)) {
                response.put("msg", "Empresas actualizadas correctamente");
                response.put("ok", "ok");
                return new ResponseEntity<>(response, HttpStatus.OK);
            } else {
                response.put("msg", "No se realizaron cambios en la base de datos");
                response.put("ok", "warn");
                return new ResponseEntity<>(response, HttpStatus.OK);
            }

        } catch (Exception e) {
            response.put("msg", "Error interno: " + e.getMessage());
            response.put("ok", "error");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    /**
     * NUEVOS ENDPOINTS USANDO EL SP HELPER
     */
    private static final String SUCCESS_MESSAGE = "Operación realizada exitosamente";

    // -----------------------------------------------------------------
    // CAMBIOS DE LINEAS CORPORATIVAS TIGO
    // -----------------------------------------------------------------

    /**
     * Registrar o actualizar un cambio de linea.
     * Si codCambio == 0 → INSERT, si codCambio > 0 → UPDATE
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/registrarCambioLinea")
    public ResponseEntity<ApiResponse<?>> registrarCambioLinea(@RequestBody CambiosTigo mb) {
        RespuestaSp res = cTigoDao.abmCambioLinea(mb, mb.getCodCambio() == 0 ? "I" : "U");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(res.getErrormsg(), res.getIdGenerado(), HttpStatus.CREATED.value()));
    }

    /**
     * Eliminar un cambio pendiente.
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/eliminarCambioLinea")
    public ResponseEntity<ApiResponse<?>> eliminarCambioLinea(@RequestBody CambiosTigo mb) {
        RespuestaSp res = cTigoDao.abmCambioLinea(mb, "D");
        return ResponseEntity.status(HttpStatus.OK)
                .body(new ApiResponse<>(res.getErrormsg(), res.getIdGenerado(), HttpStatus.OK.value()));
    }

    /**
     * Aplicar todos los cambios pendientes de un periodo.
     * Se llama antes de ejecutar la factura del periodo.
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/aplicarCambiosLinea")
    public ResponseEntity<ApiResponse<?>> aplicarCambiosLinea(@RequestBody CambiosTigo mb) {
        RespuestaSp res = cTigoDao.abmCambioLinea(mb, "A");
        return ResponseEntity.status(HttpStatus.OK)
                .body(new ApiResponse<>(res.getErrormsg(), res.getIdGenerado(), HttpStatus.OK.value()));
    }

    /**
     * Lista unificada de numeros asignados (empleados + externos).
     * Filtrable por tipoSocio y search.
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/listarNumerosAsignados")
    public ResponseEntity<ApiResponse<?>> listarNumerosAsignados(@RequestBody CambiosTigo mb) {
        return procesarListaCambios(cTigoDao.listarNumeros(mb));
    }

    /**
     * Lista de cambios registrados.
     * Filtrable por periodoCobrado, estado, codEmpleado o codCambio.
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/listarCambiosLinea")
    public ResponseEntity<ApiResponse<?>> listarCambiosLinea(@RequestBody CambiosTigo mb) {
        return procesarListaCambios(cTigoDao.listarCambios(mb));
    }

    /**
     * Lista de destinos posibles para el dropdown de reasignacion.
     * Incluye externos + empleados con corporativo + empleados sin corporativo.
     * Filtrable por tipoSocio y search.
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/listarDestinosLinea")
    public ResponseEntity<ApiResponse<?>> listarDestinosLinea(@RequestBody CambiosTigo mb) {
        return procesarListaCambios(cTigoDao.listarDestinos(mb));
    }
    /**
     * Reasignar numeros SIN ASIGNAR DEPENDIENDO SI ES EXTERNO O EMPLEADO.
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/reasignarNumeroSinAsignar")
    public ResponseEntity<ApiResponse<?>> reasignarNumero(@RequestBody CambiosTigo mb) {
        RespuestaSp res = cTigoDao.abmCambioLinea(mb, "B");
        return ResponseEntity.status(HttpStatus.OK)
                .body(new ApiResponse<>(res.getErrormsg(), res.getIdGenerado(), HttpStatus.OK.value()));
    }

    /**
     * REGISTRAR CHIP PERDIDO TIGO
     * @param ct
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/registrarPerdidaChip")
    public ResponseEntity<ApiResponse<?>> registrarPerdida(@RequestBody ChipTigo ct) {
        RespuestaSp res = ctTigoDao.abmChipTigo(ct, ct.getCodLinea() == 0 ? "I" : "U");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(res.getErrormsg(), res.getIdGenerado(), HttpStatus.CREATED.value()));
    }

    /**
     * LISTAR PERDIDAS CHIP TIGO
     * @param ct
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/listarPerdidas")
    public ResponseEntity<ApiResponse<?>> listarPerdidas(@RequestBody ChipTigo ct) {
        return procesarListaCambios(ctTigoDao.listarPerdidas(ct));
    }

    /**
     * eliminar registro de perdida chip tigo
     * @param ct
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/eliminarRegistroPerdida")
    public ResponseEntity<ApiResponse<?>> eliminarRegistroPerdida(@RequestBody ChipTigo ct) {
        RespuestaSp res = ctTigoDao.abmChipTigo(ct, "D");
        return ResponseEntity.status(HttpStatus.OK)
                .body(new ApiResponse<>(res.getErrormsg(), res.getIdGenerado(), HttpStatus.OK.value()));
    }

    /**
     * LISTAR PERIODOS PARA EL DROPDOWN chips tigo
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/listarPeriodos")
    public ResponseEntity<ApiResponse<?>> listarPeriodos() {
        return procesarListaCambios(ctTigoDao.listarPeriodos());
    }
    /**
     * Devolera una lista de los tipos del motivo de renovacion chip tigo
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/tipoRenovacion")
    public List<Tipos> listTipoRenovacion(){
        return this.ctTigoDao.listTipoRenovacion();
    }

    /**
     * REPORTE REGISTRO PERDIDA LINEA
     * @param ct
     * @return
     */
    @PostMapping("/RptPerdidaLineas")
    public ResponseEntity<?> RptPerdidaLineas(@RequestBody ChipTigo ct)  {

        String nombreReporte = "RptPerdidaEquipos";


        try{
            Map<String, Object> params = new HashMap<>();
            params.put("periodo", ct.getPeriodo() );
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
     * LISTAR PERIODOS PARA EL DROPDOWN cambios linea tigo
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/listarPeriodosCambio")
    public ResponseEntity<ApiResponse<?>> listarPeriodosCambio() {
        return procesarListaCambios(cTigoDao.listarPeriodosCambio());
    }

    /**
     * reporte cambios linea tigo
     * @param ct
     * @return
     */
    @PostMapping("/RptCambiosLineaTigo")
    public ResponseEntity<?> RptCambiosLineaTigo(@RequestBody CambiosTigo ct)  {

        String nombreReporte = "RptCambiosLineaTigo";


        try{
            Map<String, Object> params = new HashMap<>();
            params.put("periodoCobrado", ct.getPeriodoCobrado() );
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
     * Ejecutar periodo Tigo completo (ACCION='E').
     *
     * Unifica en una sola llamada lo que antes requerían dos endpoints:
     *   - /ejecutarTigo      → ACCION='G' → INSERT en tTigo_ejecutado
     *   - /generarAnticiposTigo → ACCION='B' → INSERT en [BOSQUE].dbo.Anticipo_2
     *
     * Ahora el SP maneja ambos INSERTs en una sola transacción con:
     *   - Validaciones estructuradas (@error, @errormsg)
     *   - Rollback total si algo falla
     *   - @idGenerado = total de registros procesados
     *
     * Devuelve HTTP 200 con el mensaje del SP (éxito o error descriptivo).
     * Devuelve HTTP 400 si el SP reporta un error de negocio (@error > 0).
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/ejecutarPeriodoTigo")
    public ResponseEntity<ApiResponse<?>> ejecutarPeriodoTigo(@RequestBody TigoEjecutado te) {
        RespuestaSp res = eTigoDao.ejecutarPeriodo(te);

        if (res.getError() != 0) {
            // Error de negocio devuelto por el SP (validaciones, cuadre, etc.)
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(res.getErrormsg(), null, HttpStatus.BAD_REQUEST.value()));
        }

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ApiResponse<>(res.getErrormsg(), res.getIdGenerado(), HttpStatus.OK.value()));
    }
    /**
     * REPORTE DE LINEAS CORPORATIVAS DEL PERSONAL - SERA ENVIADO CADA MES A TODO EL PERSONAL.
     * @param te
     * @return
     */
    @PostMapping("/RptCorporativosPersonal")
    public ResponseEntity<?> RptCorporativosPersonal(@RequestBody TigoEjecutado te)  {

        String nombreReporte = "RptCorporativosPersonal";


        try{
            Map<String, Object> params = new HashMap<>();
            params.put("periodoCobrado", te.getPeriodoCobrado() );
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
     * REPORTE COMPARACION DE EMPRESAS BOSQUE CLIENTE Y BOSQUE WEB -- PREVIO A EJECUTAR LA FACTURA DE TIGO
     * @return
     */
    @PostMapping("/RptComparacionEmpresas")
    public ResponseEntity<?> RptComparacionEmpresas()  {

        String nombreReporte = "RptComparacionEmpresas";


        try{
            Map<String, Object> params = new HashMap<>();

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
     * Metodo auxiliar para procesar listas de cambios.
     * Devuelve 204 si no hay registros, 200 si hay.
     */
    private <T> ResponseEntity<ApiResponse<?>> procesarListaCambios(List<T> lista) {
        if (lista == null || lista.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(new ApiResponse<>("No se encontraron registros.", null,
                            HttpStatus.NO_CONTENT.value()));
        }
        return ResponseEntity.ok(new ApiResponse<>(SUCCESS_MESSAGE, lista, HttpStatus.OK.value()));
    }


}
