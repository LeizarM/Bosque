package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.commons.JasperReportExport;
import bo.bosque.com.impexpap.commons.service.CatalogoItemSapService;
import bo.bosque.com.impexpap.dao.ICcrSolicitud;
import bo.bosque.com.impexpap.dao.ICcrSolicitudDetalle;
import bo.bosque.com.impexpap.dto.SolicitudCorteDto;
import bo.bosque.com.impexpap.model.CcrSolicitud;
import bo.bosque.com.impexpap.model.CcrSolicitudDetalle;
import bo.bosque.com.impexpap.model.ItemSap;
import bo.bosque.com.impexpap.utils.Utiles;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Solicitudes de servicio de corte (modulo Produccion, vista tccrControlCorteResmado).
 *
 * Reemplaza al bean {@code wSolicitudCrtCtrl} del sistema anterior. Solo se
 * migro el tipo ESPECIAL: el boton de "Serv. Corte Estandar" esta comentado en
 * el .xhtml de produccion desde 2025 y no se genera mas. Las solicitudes STD
 * historicas se siguen listando y consultando igual.
 */
@RestController
@CrossOrigin("*")
@RequestMapping("/solicitud-corte")
public class SolicitudCorteController {

    /** Estado con el que nace una solicitud. */
    private static final String ESTADO_SOLICITADA = "SOL";

    /** Los estados desde los que ya no se puede cancelar. */
    private static final List<String> ESTADOS_NO_CANCELABLES =
            java.util.Arrays.asList("FIN", "CNC", "REC");

    private final ICcrSolicitud solicitudDao;
    private final ICcrSolicitudDetalle detalleDao;
    private final CatalogoItemSapService catalogoItemSap;
    private final JasperReportExport jasperReportExport;

    public SolicitudCorteController(ICcrSolicitud solicitudDao,
                                    ICcrSolicitudDetalle detalleDao,
                                    CatalogoItemSapService catalogoItemSap,
                                    JasperReportExport jasperReportExport) {
        this.solicitudDao = solicitudDao;
        this.detalleDao = detalleDao;
        this.catalogoItemSap = catalogoItemSap;
        this.jasperReportExport = jasperReportExport;
    }


    // ==================================================================
    // CONSULTA
    // ==================================================================

    /**
     * Solicitudes de un rango de fechas. Sin rango devuelve todas.
     * @param mb
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/listado")
    public List<CcrSolicitud> obtenerSolicitudes( @RequestBody SolicitudCorteDto mb ) {

        List<CcrSolicitud> lstTemp = this.solicitudDao.obtenerSolicitudes(
                mb.getFechaIni(), mb.getFechaFin() );

        if( lstTemp.size() == 0 ) return new ArrayList<>();

        return lstTemp;

    }


    /**
     * Los items de una solicitud, con lo que devolvio SAP.
     * @param mb con idSolicitud
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/detalle")
    public List<CcrSolicitudDetalle> obtenerDetalle( @RequestBody SolicitudCorteDto mb ) {

        List<CcrSolicitudDetalle> lstTemp =
                this.detalleDao.obtenerDetalleXSolicitud( mb.getIdSolicitud() );

        if( lstTemp.size() == 0 ) return new ArrayList<>();

        return lstTemp;

    }


    /**
     * Busca en el catalogo de items SAP que se pueden cortar.
     *
     * Devuelve solo lo que coincide, no el catalogo entero: son ~1.500 items y
     * mandarlos todos eran 550 KB por cada vez que se abria el formulario.
     * Ver {@link CatalogoItemSapService}.
     *
     * @param mb con `texto` y `limite`
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/items-sap")
    public List<ItemSap> buscarItemsSap( @RequestBody SolicitudCorteDto mb ) {

        List<ItemSap> lstTemp = this.catalogoItemSap.buscar( mb.getTexto(), mb.getLimite() );

        if( lstTemp.size() == 0 ) return new ArrayList<>();

        return lstTemp;

    }


    /**
     * Cuantos items tiene el catalogo, para que el buscador pueda decir entre
     * cuantos se esta buscando.
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/items-sap-total")
    public Map<String, Object> totalItemsSap() {
        Map<String, Object> r = new HashMap<>();
        r.put("total", this.catalogoItemSap.total());
        return r;
    }


    // ==================================================================
    // ESCRITURA
    // ==================================================================

    /**
     * Registra la solicitud con todos sus items.
     *
     * Va en una transaccion: si un item falla, se deshace tambien la cabecera.
     * El sistema anterior compensaba a mano —borraba el detalle y despues la
     * cabecera—, lo que dejaba solicitudes huerfanas si el borrado tambien
     * fallaba.
     *
     * @param mb cabecera + detalle
     * @return el idSolicitud generado
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/registrar")
    @Transactional
    public ResponseEntity<?> registrarSolicitud( @RequestBody SolicitudCorteDto mb ) {

        Map<String, Object> response = new HashMap<>();
        CcrSolicitud cabecera = mb.getSolicitud();

        if( cabecera == null || mb.getDetalle().isEmpty() ){
            response.put("msg", "La solicitud debe tener al menos un item a cortar");
            response.put("ok", "error");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        // La observacion es obligatoria: es lo que explica el corte a planta.
        String obs = cabecera.getObservacion() == null ? "" : cabecera.getObservacion().trim();
        if( obs.length() < 3 || obs.length() > 200 ){
            response.put("msg", "La observacion debe tener entre 3 y 200 caracteres");
            response.put("ok", "error");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        cabecera.setEstado( ESTADO_SOLICITADA );
        cabecera.setFechaSistema( new java.util.Date() );
        // El total es la suma del detalle, no un dato que se escriba a mano.
        double total = 0;
        for( CcrSolicitudDetalle d : mb.getDetalle() ) total += d.getCantToneladasSolicitados();
        cabecera.setTotalToneladas( total );

        long idSolicitud = this.solicitudDao.registrarSolicitud( cabecera );

        if( idSolicitud <= 0 ){
            response.put("msg", "No se pudo registrar la solicitud de corte");
            response.put("ok", "error");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        for( CcrSolicitudDetalle d : mb.getDetalle() ){
            d.setIdSolicitud( idSolicitud );
            d.setAudUsuario( cabecera.getAudUsuario() );

            if( !this.detalleDao.registrarDetalle( d ) ){
                // Con @Transactional alcanza con lanzar: se deshace todo.
                throw new IllegalStateException(
                        "No se pudo registrar el item " + d.getCodigoSAPBase() );
            }
        }

        response.put("msg", "Solicitud de corte registrada");
        response.put("ok", "ok");
        response.put("idSolicitud", idSolicitud);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }


    /**
     * Cancela una solicitud. Pide un motivo largo a proposito: una cancelacion
     * consume un numero de solicitud para siempre y hay que poder explicarla
     * meses despues.
     *
     * @param mb con idSolicitud, observacion y audUsuario
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/cancelar")
    public ResponseEntity<?> cancelarSolicitud( @RequestBody SolicitudCorteDto mb ) {

        Map<String, Object> response = new HashMap<>();

        String motivo = mb.getObservacion() == null ? "" : mb.getObservacion().trim();
        if( motivo.length() <= 15 ){
            response.put("msg", "El motivo de la cancelacion debe tener mas de 15 caracteres");
            response.put("ok", "error");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        CcrSolicitud mbTemp = new CcrSolicitud();
        mbTemp.setIdSolicitud( mb.getIdSolicitud() );
        mbTemp.setObservacion( motivo );
        mbTemp.setAudUsuario( mb.getAudUsuario() );

        if( !this.solicitudDao.cancelarSolicitud( mbTemp ) ){
            response.put("msg", "No se pudo cancelar la solicitud");
            response.put("ok", "error");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        response.put("msg", "Solicitud cancelada");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }


    /**
     * Los estados desde los que ya no se puede cancelar, para que el front no
     * tenga que repetir la regla.
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/estados-no-cancelables")
    public List<String> estadosNoCancelables() {
        return ESTADOS_NO_CANCELABLES;
    }


    // ==================================================================
    // REPORTES
    // ==================================================================

    /**
     * La boleta de una solicitud (RptCcr01 y sus cuatro subreportes).
     *
     * Los logos van como InputStream y se consumen al leerlos, asi que se abren
     * nuevos en cada generacion: cachearlos daria el logo la primera vez y nada
     * el resto.
     *
     * @param mb con idSolicitud
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/reporte-solicitud-pdf")
    public ResponseEntity<?> reporteSolicitudPdf( @RequestBody SolicitudCorteDto mb ) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("idRegistro", (int) mb.getIdSolicitud());
            params.put("logoEmpresa", getClass().getResourceAsStream("/logos/logoEmpresa.jpg"));
            params.put("logoSistema", getClass().getResourceAsStream("/logos/logoIzquierdo.jpg"));
            params.put("logoAgua",    getClass().getResourceAsStream("/logos/logoEmpresaAgua.jpg"));

            byte[] reportBytes = this.jasperReportExport.exportPDFStatic("RptCcr01", params);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentLength(reportBytes.length);
            headers.setContentType(MediaType.APPLICATION_PDF);

            return new ResponseEntity<>(reportBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            System.out.println("Error: SolicitudCorteController en reporteSolicitudPdf ->" + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    /**
     * Resumen de solicitudes de corte entre fechas.
     * @param mb con fechaIni y fechaFin
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/reporte-resumen-pdf")
    public ResponseEntity<?> reporteResumenPdf( @RequestBody SolicitudCorteDto mb ) {
        try {
            Utiles utiles = new Utiles();

            Map<String, Object> params = new HashMap<>();
            params.put("fechaIni", utiles.fechaJ_a_Sql(mb.getFechaIni()));
            params.put("fechaFin", utiles.fechaJ_a_Sql(mb.getFechaFin()));

            byte[] reportBytes = this.jasperReportExport
                    .exportPDFStatic("RptResumenSolicitudCorte", params);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentLength(reportBytes.length);
            headers.setContentType(MediaType.APPLICATION_PDF);

            return new ResponseEntity<>(reportBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            System.out.println("Error: SolicitudCorteController en reporteResumenPdf ->" + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
