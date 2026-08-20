package bo.bosque.com.impexpap.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import bo.bosque.com.impexpap.commons.JasperReportExport;
import bo.bosque.com.impexpap.dao.*;
import bo.bosque.com.impexpap.dto.ReporteProduccionDto;
import bo.bosque.com.impexpap.model.*;
import bo.bosque.com.impexpap.utils.ApiResponse;
import bo.bosque.com.impexpap.utils.Utiles;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RestController
@CrossOrigin("*")
@RequestMapping("/loteProduccion")
public class LoteProduccionController {


    private final ILoteProduccion loteProducionDao;
    private final IMaterialIngreso materialIngresoDao;
    private final IMaterialSalida materialSalidaDao;
    private final IMerma mermaDao;
    private final IMaquinaProduccion maquinaProduccionDao;
    private final IEmpresa empresaDao;
    private final JasperReportExport jasperReportExport;

    /**
     * Constructor de la clase
     * @param loteProducionDao
     * @param materialIngresoDao
     * @param materialSalidaDao
     * @param mermaDao
     * @param maquinaProduccionDao
     * @param empresaDao
     * @param jasperReportExport
     */
    public LoteProduccionController(ILoteProduccion loteProducionDao, IMaterialIngreso materialIngresoDao, IMaterialSalida materialSalidaDao, IMerma mermaDao, IMaquinaProduccion maquinaProduccionDao, IEmpresa empresaDao, JasperReportExport jasperReportExport) {
        this.loteProducionDao = loteProducionDao;
        this.materialIngresoDao = materialIngresoDao;
        this.materialSalidaDao = materialSalidaDao;
        this.mermaDao = mermaDao;
        this.maquinaProduccionDao = maquinaProduccionDao;
        this.empresaDao = empresaDao;
        this.jasperReportExport = jasperReportExport;
    }

    /**
     * Servicio para obtener los datos de lote produccion
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/newLoteProduccion")
    public List<LoteProduccion> obtenerListadeLotesDeProduccion( @RequestBody LoteProduccion mb ){

        List<LoteProduccion> lstTemp = this.loteProducionDao.obtenerLotesProduccionNew( mb.getIdMa() );

        if( lstTemp.size() == 0 ) return new ArrayList<>();

        return lstTemp;

    }

    /**
     * Servicio para obtener los articulo
     * @return lstTemp
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/articulos")
    public List<LoteProduccion> obtenerArticulo(){

        List<LoteProduccion> lstTemp = this.loteProducionDao.obtenerArticulos();

        if( lstTemp.size() == 0 ) return new ArrayList<>();

        return lstTemp;

    }

    /**
     * servicio para registrar el lote de produccion
     * @param regLoteProduccion
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/registroLoteProduccion")
    public ResponseEntity<?> registrarLoteProduccion(@RequestBody LoteProduccion regLoteProduccion ) {


        Map<String, Object> response = new HashMap<>();
        regLoteProduccion.setFecha( new Utiles().fechaJ_a_Sql(regLoteProduccion.getFecha()));
        String acc = "U";
        if( regLoteProduccion.getIdLp() == 0){
            acc = "I";
        }

        if( !this.loteProducionDao.registrarLoteProduccion( regLoteProduccion, acc ) ){
            response.put("msg", "Error al Registrar El lote de produccion");
            response.put("ok", "error");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        response.put("msg", "Datos de Lote Produccion Actualizados");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * servicio para registrar el material de ingreso
     * @param regMatIng
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/registroIngreso")
    public ResponseEntity<?> registrarMaterialIngreso( @RequestBody List<MaterialIngreso> regMatIng  ) {

        Map<String, Object> response = new HashMap<>();
        boolean errorOccurred = false;

        for (MaterialIngreso material : regMatIng) {
            String acc = material.getIdMi() == 0 ? "I" : "U"; // Determinar la acción por cada material

            if (!this.materialIngresoDao.registrarMaterialIngreso(material, acc)) {
                errorOccurred = true;
                // Podrías optar por recolectar más detalles sobre qué material causó el error
                response.put("msg", "Error al registrar el material de ingreso con ID: " + material.getIdMi());
                response.put("ok", "error");

                // Puedes decidir si retornar inmediatamente en caso de error o continuar procesando
                return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        response.put("msg", "Todos los datos de ingreso han sido actualizados correctamente");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * servicio para registrar el material de ingreso
     * @param regMatSal
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/registroSalida")
    public ResponseEntity<?> registrarMaterialSalida( @RequestBody List<MaterialSalida> regMatSal  ) {

        Map<String, Object> response = new HashMap<>();

        for (MaterialSalida material : regMatSal) {
            String acc = material.getIdMs() == 0 ? "I" : "U"; // Determinar la acción por cada material

            if (!this.materialSalidaDao.registrarMaterialSalida(material, acc)) {

                // Podrías optar por recolectar más detalles sobre qué material causó el error
                response.put("msg", "Error al registrar el material de salida con ID: " + material.getIdMs());
                response.put("ok", "error");

                // Puedes decidir si retornar inmediatamente en caso de error o continuar procesando
                return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        response.put("msg", "Todos los datos de salida han sido actualizados correctamente");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);

    }


    /**
     * Servicio para registrar la merma
     * @param regMerma
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/registroMerma")
    public ResponseEntity<?> registrarMerma( @RequestBody List<Merma> regMerma  ) {

        Map<String, Object> response = new HashMap<>();

        for (Merma material : regMerma) {
            String acc = material.getIdMe() == 0 ? "I" : "U"; // Determinar la acción por cada material

            if (!this.mermaDao.registrarMerma(material, acc)) {

                // Podrías optar por recolectar más detalles sobre qué material causó el error
                response.put("msg", "Error al registrar la merma con ID: " + material.getIdMe());
                response.put("ok", "error");

                // Puedes decidir si retornar inmediatamente en caso de error o continuar procesando
                return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        response.put("msg", "Todos los datos de merma han sido actualizados correctamente");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }


    /**
     * Servicio para obtener las maquinas de producción
     * @return lstTemp
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/maquina")
    public List<MaquinaProduccion> obtenerMaquina(){

        List<MaquinaProduccion> lstTemp = this.maquinaProduccionDao.obtenerMaquina();

        if( lstTemp.size() == 0 ) return new ArrayList<>();

        return lstTemp;

    }


    /**
     * Obtiene todos las empresas registradas en el sistema.
     * @return
     */
    @PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
    @PostMapping("/lst-empresas")
    public List<Empresa> obtenerEmpresas() {

        List<Empresa> empresas = empresaDao.obtenerEmpresas();

        if( empresas.size() == 0 ) return new ArrayList<>();

        return empresas;

    }


    /**
     * Obtiene los docNum por orden de fabricación para una empresa.
     * @return
     */
    @PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
    @PostMapping("/lstDocNumOrdFabXEmpresa")
    public List<LoteProduccion> obtenerDocNumOrdFabXEmpresa( @RequestBody LoteProduccion mb ) {

        List<LoteProduccion> temp = loteProducionDao.obtenerDocNumXEmpresa( mb.getCodEmpresa() );

        if( temp.size() == 0 ) return new ArrayList<>();

        return temp;

    }


    // ==================================================================
    // VER LOTE DE PRODUCCION
    // ==================================================================

    /**
     * Lista los lotes de produccion de un rango de fechas.
     * Sin rango en el cuerpo devuelve los ultimos 125, como antes.
     *
     * @param mb parametros con fechaIni y fechaFin
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/listaLotes")
    public List<LoteProduccion> obtenerLotesProduccion( @RequestBody ReporteProduccionDto mb ) {

        List<LoteProduccion> lstTemp = this.loteProducionDao.obtenerLotesProduccion(
                mb.getFechaIni(), mb.getFechaFin() );

        if( lstTemp.size() == 0 ) return new ArrayList<>();

        return lstTemp;

    }


    /**
     * Material de ingreso de un lote.
     * @param mb lote con idLp
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/materialIngreso")
    public List<MaterialIngreso> obtenerMaterialIngreso( @RequestBody LoteProduccion mb ) {

        List<MaterialIngreso> lstTemp = this.materialIngresoDao.obtenerMaterialIngresoXLote( mb.getIdLp() );

        if( lstTemp.size() == 0 ) return new ArrayList<>();

        return lstTemp;

    }


    /**
     * Material de salida de un lote.
     * @param mb lote con idLp
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/materialSalida")
    public List<MaterialSalida> obtenerMaterialSalida( @RequestBody LoteProduccion mb ) {

        List<MaterialSalida> lstTemp = this.materialSalidaDao.obtenerMaterialSalidaXLote( mb.getIdLp() );

        if( lstTemp.size() == 0 ) return new ArrayList<>();

        return lstTemp;

    }


    /**
     * Mermas de un lote.
     * @param mb lote con idLp
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/merma")
    public List<Merma> obtenerMerma( @RequestBody LoteProduccion mb ) {

        List<Merma> lstTemp = this.mermaDao.obtenerMermaXLote( mb.getIdLp() );

        if( lstTemp.size() == 0 ) return new ArrayList<>();

        return lstTemp;

    }


    // ==================================================================
    // REPORTES
    // ==================================================================
    //
    // exportPDFStatic carga el .jasper precompilado de resources/reports/, no
    // compila el .jrxml. Si se modifica un .jrxml hay que recompilarlo y
    // versionar los dos archivos, o el endpoint responde 500.
    //
    // El SQL vive dentro del .jasper y Jasper recibe la conexion, por eso aqui
    // solo viajan los parametros.

    /**
     * Detalle de un lote en PDF, con sus subreportes de ingreso, salida y merma.
     * @param mb parametros con idLp
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/reporte-lote-pdf")
    public ResponseEntity<?> reporteLotePdf( @RequestBody ReporteProduccionDto mb ) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("idLp", mb.getIdLp());

            byte[] reportBytes = this.jasperReportExport.exportPDFStatic("RptLoteProduccion", params);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentLength(reportBytes.length);
            headers.setContentType(MediaType.APPLICATION_PDF);

            return new ResponseEntity<>(reportBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            System.out.println("Error: LoteProduccionController en reporteLotePdf ->" + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    /**
     * Cuadro resumen de produccion por maquina entre dos fechas.
     * @param mb parametros con fechaIni y fechaFin
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/reporte-resumen-pdf")
    public ResponseEntity<?> reporteResumenPdf( @RequestBody ReporteProduccionDto mb ) {
        try {
            Utiles utiles = new Utiles();

            Map<String, Object> params = new HashMap<>();
            params.put("fechaIni", utiles.fechaJ_a_Sql(mb.getFechaIni()));
            params.put("fechaFin", utiles.fechaJ_a_Sql(mb.getFechaFin()));

            byte[] reportBytes = this.jasperReportExport.exportPDFStatic("RptResumenLoteProduccionXFechas", params);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentLength(reportBytes.length);
            headers.setContentType(MediaType.APPLICATION_PDF);

            return new ResponseEntity<>(reportBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            System.out.println("Error: LoteProduccionController en reporteResumenPdf ->" + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    /**
     * Cuadro de resmado por grupo entre dos fechas.
     * @param mb parametros con fechaIni y fechaFin
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/reporte-resmado-pdf")
    public ResponseEntity<?> reporteResmadoPdf( @RequestBody ReporteProduccionDto mb ) {
        try {
            Utiles utiles = new Utiles();

            Map<String, Object> params = new HashMap<>();
            params.put("fechaIni", utiles.fechaJ_a_Sql(mb.getFechaIni()));
            params.put("fechaFin", utiles.fechaJ_a_Sql(mb.getFechaFin()));

            byte[] reportBytes = this.jasperReportExport.exportPDFStatic("RptResmado", params);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentLength(reportBytes.length);
            headers.setContentType(MediaType.APPLICATION_PDF);

            return new ResponseEntity<>(reportBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            System.out.println("Error: LoteProduccionController en reporteResmadoPdf ->" + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    /**
     * Consolidado de corte por maquina entre dos fechas.
     * Es el tercer reporte que ofrecia la pantalla del sistema anterior.
     *
     * @param mb parametros con fechaIni y fechaFin
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/reporte-corte-pdf")
    public ResponseEntity<?> reporteCortePdf( @RequestBody ReporteProduccionDto mb ) {
        try {
            Utiles utiles = new Utiles();

            Map<String, Object> params = new HashMap<>();
            params.put("fechaIni", utiles.fechaJ_a_Sql(mb.getFechaIni()));
            params.put("fechaFin", utiles.fechaJ_a_Sql(mb.getFechaFin()));

            byte[] reportBytes = this.jasperReportExport
                    .exportPDFStatic("RptConsolidadoCorteMaquina", params);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentLength(reportBytes.length);
            headers.setContentType(MediaType.APPLICATION_PDF);

            return new ResponseEntity<>(reportBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            System.out.println("Error: LoteProduccionController en reporteCortePdf ->" + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


}
